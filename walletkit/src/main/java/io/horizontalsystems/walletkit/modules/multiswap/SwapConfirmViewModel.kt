package io.horizontalsystems.walletkit.modules.multiswap

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.BackgroundManager
import io.horizontalsystems.walletkit.core.BackgroundManagerState
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.WatchAccountException
import io.horizontalsystems.walletkit.core.badge
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.core.managers.CurrencyManager
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.entities.SwapRecord
import io.horizontalsystems.walletkit.modules.multiswap.history.SwapOperation
import io.horizontalsystems.walletkit.modules.multiswap.history.SwapRecordOutput
import io.horizontalsystems.walletkit.modules.multiswap.history.SwapStatus
import io.horizontalsystems.walletkit.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.ISplitSwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.RetryableSwapError
import io.horizontalsystems.walletkit.modules.multiswap.providers.SwapHelper
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionResult
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceFactory
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.modules.send.SendNodeFailover
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataField
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataFieldDepositAddress
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataFieldDepositMemo
import io.horizontalsystems.walletkit.modules.send.SendModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.coroutines.cancellation.CancellationException

class SwapConfirmViewModel(
    private val swapProvider: IMultiSwapProvider,
    private val swapQuote: SwapQuote,
    private val currencyManager: CurrencyManager,
    private val fiatServiceIn: FiatService,
    private val fiatServiceOut: FiatService,
    private val fiatServiceOutMin: FiatService,
    sendTransactionService: AbstractSendTransactionService,
    private val timerService: TimerService,
    private val priceImpactService: PriceImpactService,
    private val swapDefenseSystemService: SwapDefenseSystemService,
    private val backgroundManager: BackgroundManager,
    private val swapWalletEnabler: SwapWalletEnabler,
    initialRecipient: Address? = null,
    // A split swap's recipients, chosen with their shares before this screen; null for a
    // single swap. They cannot change here: the preview is priced for exactly these.
    private val splitRecipients: List<SplitSwapRecipient>? = null,
) : ViewModelUiState<SwapConfirmUiState>() {
    private var sendTransactionSettings: SendTransactionSettings? = null
    private val currency = currencyManager.baseCurrency
    private val tokenIn = swapQuote.tokenIn

    // Replaced when the chain is moved to another node: see replaceSendTransactionService
    @Volatile
    var sendTransactionService: AbstractSendTransactionService = sendTransactionService
        private set
    private var sendTransactionServiceJob: Job? = null

    private val nodeFailover = SendNodeFailover.forToken(tokenIn)
    // The deposit may have reached the network although its send failed. Sending it again could
    // pay twice, so this screen offers neither Swap nor a new quote any more, and the swap it
    // shows is not edited; the record saved before the broadcast tracks the swap.
    // Set while sending, off the main thread.
    @Volatile
    private var maybeSentCaution: CautionViewItem? = null
    private val tokenOut = swapQuote.tokenOut

    val isSplit: Boolean = splitRecipients != null

    // tokenOut the account can't hold — the recipient came from SwapRecipientPage,
    // is mandatory and must not be cleared (there is no own-wallet address to fall back to)
    val recipientRequired = !isSplit && !SwapHelper.isTokenReceivableByAccount(tokenOut)

    // MEV protection is a property of the confirmation quote (the route) and needs a protected
    // sender on the sell chain; nothing is known before the first quote arrives.
    private fun supportsMevProtection(finalQuote: SwapFinalQuote?): Boolean =
        sendTransactionService.supportsMevProtection && finalQuote?.mevProtectionAllowed == true

    // whether a typed ZEC recipient must be transparent is the selected route's own rule
    val zcashTransparentRecipientOnly: Boolean
        get() = swapProvider.zcashTransparentRecipientOnly(swapQuote)
    private val amountIn = swapQuote.amountIn.let { amount ->
        if (amount.scale() > tokenIn.decimals) {
            amount.setScale(tokenIn.decimals, RoundingMode.DOWN)
        } else {
            amount
        }
    }
    private var fiatAmountIn: BigDecimal? = null
    private var recipient: Address? = initialRecipient
    private var slippage: BigDecimal? = null

    private var fiatAmountOut: BigDecimal? = null
    private var fiatAmountOutMin: BigDecimal? = null

    private var error: Throwable? = null
    private var initialLoading = true
    private var loading = true
    private var isInBackground = true
    private var timerState = timerService.stateFlow.value
    private var sendTransactionState = sendTransactionService.stateFlow.value
    private var priceImpactState = priceImpactService.stateFlow.value
    private var swapDefenseState = swapDefenseSystemService.stateFlow.value

    private var amountOut: BigDecimal? = null
    private var amountOutMin: BigDecimal? = null
    private var estimatedTime: Long? = null
    private var quoteFields: List<DataField> = listOf()
    // The quote on screen: what Swap commits. Has no side effects, so it is re-requested freely.
    private var finalQuote: SwapFinalQuote? = null
    // Stands in for the send service's cautions while the quote has nothing to estimate: the
    // only check possible before the order exists is the amount against the balance.
    private var unpricedCaution: CautionViewItem? = null
    private var fetchFinalQuoteJob: Job? = null
    // From the moment Swap is pressed until the send ends, nothing may replace the quote or the
    // data the send service holds: a refresh landing in between would commit another quote, or
    // put the stand-in deposit back under a transfer about to be signed.
    @Volatile
    private var sending = false

    // The account this confirmation was opened for. The send service is built from it and
    // keeps it, so everything below must agree with it rather than with whatever is active.
    private val confirmingAccountId = App.accountManager.activeAccount?.id

    init {
        fiatServiceIn.setCurrency(currency)
        fiatServiceIn.setToken(tokenIn)
        fiatServiceIn.setAmount(amountIn)

        fiatServiceOut.setCurrency(currency)
        fiatServiceOut.setToken(tokenOut)
        fiatServiceOut.setAmount(amountOut)

        fiatServiceOutMin.setCurrency(currency)
        fiatServiceOutMin.setToken(tokenOut)
        fiatServiceOutMin.setAmount(amountOutMin)

        viewModelScope.launch {
            fiatServiceIn.stateFlow.collect {
                fiatAmountIn = it.fiatAmount
                priceImpactService.setAmountIn(fiatAmountIn)
                emitState()
            }
        }

        viewModelScope.launch {
            fiatServiceOut.stateFlow.collect {
                fiatAmountOut = it.fiatAmount
                priceImpactService.setAmountOut(fiatAmountOut)
                emitState()
            }
        }

        viewModelScope.launch {
            fiatServiceOutMin.stateFlow.collect {
                fiatAmountOutMin = it.fiatAmount
                emitState()
            }
        }

        collectSendTransactionService()

        viewModelScope.launch {
            timerService.stateFlow.collect {
                timerState = it

                // The quote has no side effects, so an expired one is silently re-requested
                // instead of dead-ending the screen behind a Refresh button.
                if (timerState.timeout && !sending) {
                    refresh(silent = true)
                }
            }
        }

        viewModelScope.launch {
            priceImpactService.stateFlow.collect {
                handleUpdatedPriceImpactState(it)
            }
        }

        viewModelScope.launch {
            swapDefenseSystemService.stateFlow.collect {
                swapDefenseState = it

                emitState()
            }
        }

        viewModelScope.launch {
            backgroundManager.stateFlow.collect { state ->
                when (state) {
                    BackgroundManagerState.EnterBackground -> {
                        isInBackground = true
                        fetchFinalQuoteJob?.cancel()
                        timerService.stop()
                    }
                    BackgroundManagerState.EnterForeground -> {
                        isInBackground = false
                        if (sendTransactionSettings != null) {
                            refresh(silent = true)
                        }
                    }
                }
            }
        }

        sendTransactionService.start(viewModelScope)
        swapDefenseSystemService.start(viewModelScope)
    }

    private fun collectSendTransactionService() {
        val service = sendTransactionService
        sendTransactionServiceJob = viewModelScope.launch {
            launch {
                service.sendTransactionSettingsFlow.collect {
                    sendTransactionSettings = it

                    if (!isInBackground && !sending) {
                        fetchFinalQuote()
                    }
                }
            }

            launch {
                service.stateFlow.collect { transactionState ->
                    sendTransactionState = transactionState

                    initialLoading = initialLoading && transactionState.loading
                    loading = transactionState.loading

                    swapDefenseSystemService.setSendable(sendTransactionState.sendable)

                    emitState()

                    if (sendTransactionState.sendable) {
                        timerService.start(20)
                    }
                }
            }
        }
    }

    // The chain's adapter was rebuilt on another node. The service in use holds the adapter
    // that was stopped, so a new one takes its place. Collecting its settings asks for a new
    // quote unless a swap is being sent; then the caller re-quotes.
    private fun replaceSendTransactionService() {
        val replacement = SendTransactionServiceFactory.create(tokenIn)
        sendTransactionServiceJob?.cancel()
        sendTransactionService.clear()
        sendTransactionService = replacement
        sendTransactionState = replacement.stateFlow.value
        collectSendTransactionService()
        replacement.start(viewModelScope)
    }

    /**
     * Lets the chain move to another node if [failure] was its node's and nothing was sent.
     * Returns what to tell the user, or null when the failure stands as it is.
     */
    private suspend fun switchNodeAfter(failure: Throwable): String? {
        val outcome = nodeFailover.handle(failure) as? SendNodeFailover.Outcome.Switched ?: return null
        return serviceOnNewNode(outcome)
    }

    /**
     * Takes up a node change that finished after the screen stopped waiting for it. Until
     * then the send service holds the adapter that was stopped. Returns what to tell the
     * user, or null when there was no such change.
     */
    private fun adoptLateNodeSwitch(): String? {
        val outcome = nodeFailover.lateSwitch() ?: return null
        return serviceOnNewNode(outcome)
    }

    private fun serviceOnNewNode(outcome: SendNodeFailover.Outcome.Switched): String? {
        return try {
            replaceSendTransactionService()
            Translator.getString(R.string.Send_NodeSwitched_Description, outcome.nodeName)
        } catch (e: Throwable) {
            null
        }
    }

    private fun handleUpdatedPriceImpactState(priceImpactState: PriceImpactService.State) {
        this.priceImpactState = priceImpactState

        swapDefenseSystemService.setPriceImpact(priceImpactState.priceImpact, priceImpactState.priceImpactLevel)

        emitState()
    }

    override fun createState(): SwapConfirmUiState {
        // A quote with nothing to build before the order exists has no estimate to show, and the
        // send service's state describes whatever it was last fed, not this quote.
        val unpriced = finalQuote != null && finalQuote?.sendTransactionData == null

        return SwapConfirmUiState(
            initialLoading = initialLoading,
            loading = loading,
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            amountOut = amountOut,
            amountOutMin = amountOutMin,
            fiatAmountIn = fiatAmountIn,
            fiatAmountOut = fiatAmountOut,
            fiatAmountOutMin = fiatAmountOutMin,
            currency = currency,
            networkFee = if (unpriced) null else sendTransactionState.networkFee,
            cautions = listOfNotNull(maybeSentCaution) +
                if (unpriced) listOfNotNull(unpricedCaution) else sendTransactionState.cautions,
            maybeSent = maybeSentCaution != null,
            // Frozen while sending: the committed deposit is re-estimated through the on-screen
            // service on chains without a fresh instance, and its interim not-sendable state
            // must not swap the button out from under the send running in its scope.
            validQuote = sending || (error == null && (if (unpriced) unpricedCaution == null else sendTransactionState.sendable)),
            priceImpact = priceImpactState.priceImpact,
            priceImpactLevel = priceImpactState.priceImpactLevel,
            quoteFields = quoteFields,
            transactionFields = if (unpriced) emptyList() else sendTransactionState.fields,
            hasSettings = sendTransactionService.hasSettings,
            hasNonceSettings = sendTransactionService.hasNonceSettings,
            swapDefenseSystemMessage = swapDefenseState.systemMessage,
            mevProtectionEnabled = swapDefenseState.mevProtectionEnabled,
            supportsMevProtection = supportsMevProtection(finalQuote),
            recipient = recipient,
            recipientRequired = recipientRequired,
            slippage = slippage,
            estimatedTime = estimatedTime,
            error = error,
            splitOutputs = finalQuote?.splitOutputs,
            splitOutputFiatAmounts = splitOutputFiatAmounts(finalQuote?.splitOutputs),
        )
    }

    // Each output's share of the total's fiat value: one rate serves every output, so the
    // proportion is exact and needs no second price lookup.
    private fun splitOutputFiatAmounts(outputs: List<SplitSwapOutput>?): List<BigDecimal?>? {
        outputs ?: return null
        val total = amountOut?.takeIf { it > BigDecimal.ZERO }
        val fiatTotal = fiatAmountOut
        return outputs.map { output ->
            if (total == null || fiatTotal == null) {
                null
            } else {
                (fiatTotal * output.amountOut).divide(total, currency.decimal, RoundingMode.DOWN)
            }
        }
    }

    override fun onCleared() {
        timerService.stop()
        fiatServiceIn.clear()
        fiatServiceOut.clear()
        fiatServiceOutMin.clear()
        sendTransactionService.clear()
    }

    fun refresh(silent: Boolean = false) {
        if (isInBackground || sending || maybeSentCaution != null) return

        if (!silent) {
            loading = true
            emitState()
        }

        sendTransactionService.refreshUuid()
        fetchFinalQuote()

        if (!silent) {
            stat(page = StatPage.SwapConfirmation, event = StatEvent.Refresh)
        }
    }

    private fun fetchFinalQuote() {
        if (sending) return

        fetchFinalQuoteJob?.cancel()
        fetchFinalQuoteJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                error = null

                // The new send service asks for a fresh quote as soon as it is collected
                if (adoptLateNodeSwitch() != null) return@launch

                if (App.accountManager.activeAccount?.isWatchAccount != false) {
                    throw WatchAccountException()
                }

                val finalQuote = if (splitRecipients != null) {
                    val splitProvider = swapProvider as? ISplitSwapProvider
                        ?: throw IllegalStateException("${swapProvider.id} cannot split a swap")
                    splitProvider.fetchSplitFinalQuote(
                        tokenIn,
                        tokenOut,
                        amountIn,
                        splitRecipients,
                        sendTransactionSettings,
                        swapQuote,
                        slippage ?: IMultiSwapProvider.DEFAULT_SLIPPAGE,
                    )
                } else {
                    swapProvider.fetchFinalQuote(
                        tokenIn,
                        tokenOut,
                        amountIn,
                        sendTransactionSettings,
                        swapQuote,
                        recipient,
                        slippage ?: IMultiSwapProvider.DEFAULT_SLIPPAGE,
                    )
                }

                ensureActive()

                // Decided before the quote is published, so the button never reads the new quote
                // against the previous quote's caution.
                val sendTransactionData = finalQuote.sendTransactionData
                unpricedCaution = if (sendTransactionData == null) {
                    UnpricedDeposit.caution(tokenIn, amountIn, UnpricedDeposit.availableBalance(tokenIn, null))
                } else {
                    null
                }

                this@SwapConfirmViewModel.finalQuote = finalQuote
                swapDefenseSystemService.setSupportsMevProtection(supportsMevProtection(finalQuote))
                amountOut = finalQuote.amountOut
                amountOutMin = finalQuote.amountOutMin
                estimatedTime = finalQuote.estimatedTime
                slippage = finalQuote.slippage
                // The deposit target comes from the provider's server and is what the signed
                // transaction actually pays; when it is known at this point it must be on screen
                // next to the recipient so a compromised or spoofed provider response cannot
                // redirect funds unseen. Transfer providers name it only with the order.
                quoteFields = finalQuote.fields + listOfNotNull(
                    finalQuote.depositAddress?.let { DataFieldDepositAddress(it) },
                    finalQuote.depositMemo?.let { DataFieldDepositMemo(it) },
                )
                emitState()

                fiatServiceOut.setAmount(amountOut)
                fiatServiceOutMin.setAmount(amountOutMin)

                if (sendTransactionData != null) {
                    sendTransactionService.setSendTransactionData(sendTransactionData)
                } else {
                    // Nothing to estimate before the order names its deposit address, so the
                    // screen settles without a fee. The send service's state collector is what
                    // normally clears the loading flags, tells the defense system the quote can
                    // be sent and arms the expiry timer.
                    loading = false
                    initialLoading = false
                    swapDefenseSystemService.setSendable(unpricedCaution == null)
                    emitState()
                    // CountDownTimer binds to the current thread's Looper.
                    withContext(Dispatchers.Main) {
                        timerService.start(20)
                    }
                }

                priceImpactService.setProviderTitle(swapQuote.venue.name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: RetryableSwapError) {
                // in this case we should keep state as loading
                // temp solution. need find better one
            } catch (t: Throwable) {
                // The node failed the estimate, so the chain moves to another one. The new
                // send service asks for a fresh quote as soon as it is collected.
                if (switchNodeAfter(t) != null) return@launch

                loading = false
                initialLoading = false
                error = t

                emitState()
            }
        }
    }

    /**
     * Creates the order for the quote on screen, signs and broadcasts its transaction, records
     * the swap and reports the broadcast hash. A [SwapCommitFailedException] means the order
     * could not be created or its deposit could not be built: a fresh quote is already being
     * loaded, and the screen only shows the reason briefly.
     */
    suspend fun swap() = withContext(Dispatchers.Default) {
        val quote = finalQuote ?: throw IllegalStateException("No quote to commit")
        // Read once: setRecipient() can change the field while the send is suspended, and the
        // record and the wallet to enable must describe the swap that was actually sent.
        val recipient = this@SwapConfirmViewModel.recipient

        synchronized(this@SwapConfirmViewModel) {
            // Checked here and not only by the button: nothing may send this swap again
            maybeSentCaution?.let { throw DepositMaybeSentException(it.text) }
            if (sending) throw IllegalStateException("A swap is already being sent")
            sending = true
        }

        try {
            fetchFinalQuoteJob?.cancel()
            timerService.stop()

            stat(page = StatPage.SwapConfirmation, event = StatEvent.Send)

            // The send service was built with the account that was active when the quote was
            // prepared and holds on to it, so it would still sign after a switch — to a watch
            // account, or to another signing one whose wallet this swap was never meant to spend.
            val activeAccount = App.accountManager.activeAccount
            if (activeAccount == null ||
                activeAccount.isWatchAccount ||
                activeAccount.id != confirmingAccountId
            ) {
                throw WatchAccountException()
            }

            // Before an order exists: the quote was estimated through a send service that
            // holds a stopped adapter, so the swap is quoted again on the new node.
            adoptLateNodeSwitch()?.let { nodeSwitched ->
                requote()
                throw SwapCommitFailedException(SwapCommitFailure.Failed, DepositNotSendableException(nodeSwitched))
            }

            // The only step with side effects. On failure the quote is spent either way, so a
            // fresh preview replaces it; the reason is surfaced by the caller.
            val commitment = try {
                swapProvider.commit(quote)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                requote()
                throw (e as? SwapCommitFailedException) ?: SwapCommitFailedException(SwapCommitFailure.Failed, e)
            }

            // The order exists from here on. A deposit that cannot be built or sent leaves it to
            // lapse unfunded, and the screen re-quotes so a retry never reuses it.
            val service = try {
                sendServiceFor(quote, commitment)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val nodeSwitched = switchNodeAfter(e)
                requote()
                throw SwapCommitFailedException(
                    SwapCommitFailure.Failed,
                    nodeSwitched?.let { DepositNotSendableException(it) } ?: e,
                )
            }

            // Saved before broadcasting so the record survives the app dying between the two, and
            // so a send that fails after the node may have accepted the transaction stays
            // trackable: the kits do not say which side of the broadcast an error came from, so
            // the record is never marked failed here and no fresh quote is offered. Tracking by
            // uuid resolves it either way, and the spent token already forces a new preview on
            // the next press, through a notice.
            val recordId = saveSwapRecord(commitment, recipient)

            val startedAt = System.currentTimeMillis()
            // Set when the send failed but the wallet lists the deposit afterwards
            var deliveredHash: String? = null
            val result = try {
                try {
                    service.sendTransaction(swapDefenseState.mevProtectionEnabled)
                } finally {
                    if (service !== sendTransactionService) service.clear()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                when (val outcome = nodeFailover.handle(e)) {
                    // The node failed before anything was sent: the order lapses unfunded like
                    // one whose deposit could not be built, and the next press commits a new
                    // one on the node the chain was moved to.
                    is SendNodeFailover.Outcome.Switched -> {
                        val nodeSwitched = serviceOnNewNode(outcome) ?: throw e
                        requote()
                        throw SwapCommitFailedException(SwapCommitFailure.Failed, DepositNotSendableException(nodeSwitched))
                    }

                    // As on the send screen: the screen says what is being done while the
                    // chain is moved to another node and the deposit is looked for there.
                    // Nothing is sent again from here either way.
                    SendNodeFailover.Outcome.MaybeSent -> {
                        showMaybeSent(Translator.getString(R.string.Send_MaybeSent_Checking))
                        when (val delivery = nodeFailover.confirmDelivery(amountIn, startedAt)) {
                            is SendNodeFailover.Delivery.Found -> {
                                maybeSentCaution = null
                                emitState()
                                deliveredHash = delivery.transactionHash
                                null
                            }

                            is SendNodeFailover.Delivery.NotFound -> {
                                val text = delivery.nodeName
                                    ?.let { Translator.getString(R.string.Send_MaybeSent_NotFound, it) }
                                    ?: Translator.getString(R.string.Send_MaybeSent_Description)
                                showMaybeSent(text)
                                throw DepositMaybeSentException(text)
                            }
                        }
                    }

                    SendNodeFailover.Outcome.Unhandled -> throw e
                }
            }

            val txHash = deliveredHash ?: result?.let { transactionHash(it) }
            val providerSwapId = commitment.providerSwapId
            if (txHash != null) {
                App.swapRecordManager.updateTransactionHash(recordId, txHash)
                // Best-effort and off the critical path: funds have moved, so the screen must
                // not wait on the report, and tracking resolves the swap without it.
                if (providerSwapId != null) {
                    val splitProvider = swapProvider as? ISplitSwapProvider
                    if (splitRecipients != null && splitProvider != null) {
                        App.swapSyncService.reportSplitInboundTxHash(splitProvider, providerSwapId, txHash)
                    } else {
                        App.swapSyncService.reportInboundTxHash(swapProvider, providerSwapId, txHash)
                    }
                }
            }

            // The bought token lands in this account unless a custom recipient was set, so its
            // wallet has to be enabled for the funds to show on Balance. The transaction is
            // already broadcast at this point, so a failure here must not surface as a swap error.
            // A split's outputs go to typed addresses, any of which may be this account's.
            if (recipient == null && (splitRecipients == null || SwapHelper.isTokenReceivableByAccount(tokenOut))) {
                runCatching { swapWalletEnabler.enableReceivingWallet(activeAccount, tokenOut) }
            }

            // remember the delivery address so the recipient page can suggest it next time
            recipient?.let {
                App.recentAddressManager.setRecentAddress(it, tokenOut.blockchainType)
            }
            splitRecipients?.forEach {
                App.recentAddressManager.setRecentAddress(it.address, tokenOut.blockchainType)
            }

            result
        } finally {
            synchronized(this@SwapConfirmViewModel) { sending = false }
        }
    }

    // The quote on screen is spent or stale: load a new one without the loading placeholder, so
    // the screen keeps its numbers until the fresh ones land.
    private fun requote() {
        synchronized(this) { sending = false }
        // The last state was published with the send in progress, which held the button on, and
        // the inputs still describe the spent quote, so recomputing alone would keep it on. The
        // button waits until the fresh quote's own state lands.
        loading = true
        emitState()
        refresh(silent = true)
    }

    /**
     * The service that signs the committed quote. The on-screen one when the transaction is the
     * previewed one. A transfer provider's deposit is only named by the order, so its transaction
     * is rebuilt now: the on-screen service takes it when it reflects new data as soon as it is
     * set, otherwise a fresh instance does, so no state built for the stand-in can be signed. The
     * fee choices carry over; the estimate is redone for the real destination.
     */
    private suspend fun sendServiceFor(quote: SwapFinalQuote, commitment: SwapCommitment): AbstractSendTransactionService {
        val data = commitment.quote.sendTransactionData
            ?: throw IllegalStateException("The order names no deposit to send")
        if (data == quote.sendTransactionData) return sendTransactionService

        val service = sendTransactionService.fresh()?.also { it.start(viewModelScope) } ?: sendTransactionService
        try {
            service.setSendTransactionData(data)
            val state = service.awaitSettled()
            if (!state.sendable) {
                // A caution's text can be blank while its title carries the reason.
                val caution = state.cautions.firstOrNull { it.type == CautionViewItem.Type.Error }
                throw DepositNotSendableException(caution?.text?.ifBlank { null } ?: caution?.title ?: "Deposit cannot be sent")
            }
            return service
        } catch (e: Throwable) {
            if (service !== sendTransactionService) service.clear()
            throw e
        }
    }

    private fun showMaybeSent(text: String) {
        maybeSentCaution = CautionViewItem(
            title = Translator.getString(R.string.Send_MaybeSent_Title),
            text = text,
            type = CautionViewItem.Type.Error,
        )
        emitState()
    }

    private fun transactionHash(result: SendTransactionResult): String? = when (result) {
        is SendTransactionResult.Evm -> result.transactionHash
        is SendTransactionResult.Btc -> result.transactionRecord?.transactionHash
        is SendTransactionResult.Zcash -> result.transactionHash
        is SendTransactionResult.Solana -> result.txHash
        is SendTransactionResult.Tron -> result.txHash
        is SendTransactionResult.Thorchain -> result.txHash
        is SendTransactionResult.Stellar -> result.txHash
        is SendTransactionResult.Xrp -> result.txHash
        is SendTransactionResult.Near -> result.txHash
        is SendTransactionResult.Monero -> result.txHash
        is SendTransactionResult.Zano -> result.txHash
        SendTransactionResult.Ton -> null
    }

    private suspend fun saveSwapRecord(commitment: SwapCommitment, recipient: Address?): Int {
        val quote = commitment.quote
        val splitOutputs = quote.splitOutputs?.takeIf { isSplit }?.map { output ->
            SwapRecordOutput(
                index = output.index,
                uuid = output.uuid,
                percent = output.percent,
                address = output.address,
                amountOut = output.amountOut.toPlainString(),
                status = SwapStatus.Depositing.name,
            )
        }

        val record = SwapRecord(
            accountId = confirmingAccountId ?: "",
            timestamp = System.currentTimeMillis(),
            providerId = swapQuote.venue.id,
            providerName = swapQuote.venue.name,
            tokenInUid = tokenIn.tokenQuery.id,
            tokenInCoinCode = tokenIn.coin.code,
            tokenInCoinUid = tokenIn.coin.uid,
            tokenInBadge = tokenIn.badge,
            tokenInBlockchainTypeUid = tokenIn.blockchainType.uid,
            tokenOutUid = tokenOut.tokenQuery.id,
            tokenOutCoinCode = tokenOut.coin.code,
            tokenOutCoinUid = tokenOut.coin.uid,
            tokenOutBadge = tokenOut.badge,
            tokenOutBlockchainTypeUid = tokenOut.blockchainType.uid,
            amountIn = amountIn.toPlainString(),
            amountOut = quote.amountOut.toPlainString(),
            amountOutMin = quote.amountOutMin?.toPlainString(),
            // A split's recipients are all typed addresses; the first stands in for the single
            // recipient field, the rest live in splitOutputs.
            recipientAddress = splitOutputs?.firstOrNull()?.address
                ?: recipient?.hex
                ?: SwapHelper.getReceiveAddressForToken(tokenOut),
            customRecipientAddress = recipient != null || splitOutputs != null,
            sourceAddress = SwapHelper.getSendingAddressForToken(tokenIn),
            transactionHash = null,
            providerSwapId = commitment.providerSwapId,
            fromAsset = quote.fromAsset,
            toAsset = quote.toAsset,
            depositAddress = quote.depositAddress,
            status = SwapStatus.Depositing.name,
            operation = (if (splitOutputs != null) SwapOperation.SplitSwap else SwapOperation.Swap).name,
            estimatedTime = quote.estimatedTime,
            splitOutputs = splitOutputs?.let { SwapRecordOutput.toJson(it) },
        )
        return App.swapRecordManager.save(record)
    }

    fun setRecipient(recipient: Address?) {
        // The swap that may have been sent pays the recipient on screen
        if (maybeSentCaution != null) return
        // A split's recipients were fixed before this screen.
        if (isSplit) return
        // a mandatory external recipient can be changed but never cleared
        if (recipient == null && recipientRequired) return

        this.recipient = recipient

        refresh()
    }

    fun setSlippage(slippage: BigDecimal) {
        if (maybeSentCaution != null) return
        if (slippage == this.slippage) return

        this.slippage = slippage

        refresh()
    }

    fun setMevProtectionEnabled(enabled: Boolean) {
        swapDefenseSystemService.setSwapProtectionEnabled(enabled)
    }

    companion object {
        fun init(
            quote: SwapProviderQuote,
            initialRecipient: Address? = null,
            splitRecipients: List<SplitSwapRecipient>? = null,
        ): CreationExtras.() -> SwapConfirmViewModel = {
            val sendTransactionService = SendTransactionServiceFactory.create(quote.tokenIn)

            SwapConfirmViewModel(
                quote.provider,
                quote.swapQuote,
                App.currencyManager,
                FiatService(App.marketKit),
                FiatService(App.marketKit),
                FiatService(App.marketKit),
                sendTransactionService,
                TimerService(),
                PriceImpactService(PriceImpactLevel.Normal),
                SwapDefenseSystemService(App.paidActionSettingsManager),
                App.backgroundManager,
                SwapWalletEnabler(App.walletManager, App.restoreSettingsManager),
                initialRecipient,
                splitRecipients,
            )
        }
    }
}


data class SwapConfirmUiState(
    val initialLoading: Boolean,
    val loading: Boolean,
    val tokenIn: Token,
    val tokenOut: Token,
    val amountIn: BigDecimal,
    val amountOut: BigDecimal?,
    val amountOutMin: BigDecimal?,
    val fiatAmountIn: BigDecimal?,
    val fiatAmountOut: BigDecimal?,
    val fiatAmountOutMin: BigDecimal?,
    val currency: Currency,
    val networkFee: SendModule.AmountData?,
    val cautions: List<CautionViewItem>,
    // The deposit may have been sent: the screen shows why and offers nothing to press
    val maybeSent: Boolean,
    val validQuote: Boolean,
    val priceImpact: BigDecimal?,
    val priceImpactLevel: PriceImpactLevel?,
    val quoteFields: List<DataField>,
    val transactionFields: List<DataField>,
    val hasSettings: Boolean,
    val hasNonceSettings: Boolean,
    val swapDefenseSystemMessage: DefenseSystemMessage?,
    val mevProtectionEnabled: Boolean,
    val supportsMevProtection: Boolean,
    val recipient: Address?,
    val recipientRequired: Boolean,
    val slippage: BigDecimal?,
    val estimatedTime: Long?,
    val error: Throwable?,
    // A split swap's outputs in recipient order, each with its fiat value; null for a single swap.
    val splitOutputs: List<SplitSwapOutput>? = null,
    val splitOutputFiatAmounts: List<BigDecimal?>? = null,
)
