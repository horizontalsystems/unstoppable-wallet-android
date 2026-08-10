package io.horizontalsystems.walletkit.modules.multiswap

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import io.horizontalsystems.walletkit.modules.multiswap.providers.MAYA_PROVIDER_ID
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.BackgroundManager
import io.horizontalsystems.walletkit.core.BackgroundManagerState
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.badge
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.core.managers.CurrencyManager
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.core.toHexString
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.entities.SwapRecord
import io.horizontalsystems.walletkit.modules.multiswap.history.SwapStatus
import io.horizontalsystems.walletkit.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.RetryableSwapError
import io.horizontalsystems.walletkit.modules.multiswap.providers.SwapHelper
import io.horizontalsystems.walletkit.modules.multiswap.providers.SwapProviderType
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionResult
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceFactory
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataField
import io.horizontalsystems.walletkit.modules.send.SendModule
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
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
    val sendTransactionService: AbstractSendTransactionService,
    private val timerService: TimerService,
    private val priceImpactService: PriceImpactService,
    private val swapDefenseSystemService: SwapDefenseSystemService,
    private val backgroundManager: BackgroundManager,
    initialRecipient: Address? = null,
) : ViewModelUiState<SwapConfirmUiState>() {
    private var sendTransactionSettings: SendTransactionSettings? = null
    private val currency = currencyManager.baseCurrency
    private val tokenIn = swapQuote.tokenIn
    private val tokenOut = swapQuote.tokenOut

    // tokenOut the account can't hold — the recipient came from SwapRecipientPage,
    // is mandatory and must not be cleared (there is no own-wallet address to fall back to)
    val recipientRequired = !SwapHelper.isTokenReceivableByAccount(tokenOut)

    // only Maya delivers ZEC to shielded/unified receivers; every other route needs
    // a transparent recipient address
    val zcashTransparentRecipientOnly =
        tokenOut.blockchainType == BlockchainType.Zcash && swapProvider.id != MAYA_PROVIDER_ID
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
    private var expired = false
    private var isInBackground = true
    private var timerState = timerService.stateFlow.value
    private var sendTransactionState = sendTransactionService.stateFlow.value
    private var priceImpactState = priceImpactService.stateFlow.value
    private var swapDefenseState = swapDefenseSystemService.stateFlow.value

    private var amountOut: BigDecimal? = null
    private var amountOutMin: BigDecimal? = null
    private var estimatedTime: Long? = null
    private var quoteFields: List<DataField> = listOf()
    private var providerSwapId: String? = null
    private var fromAsset: String? = null
    private var toAsset: String? = null
    private var depositAddress: String? = null
    private var fetchFinalQuoteJob: Job? = null

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

        viewModelScope.launch {
            sendTransactionService.sendTransactionSettingsFlow.collect {
                sendTransactionSettings = it

                if (!isInBackground) {
                    fetchFinalQuote()
                }
            }
        }

        viewModelScope.launch {
            sendTransactionService.stateFlow.collect { transactionState ->
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

        viewModelScope.launch {
            timerService.stateFlow.collect {
                timerState = it

                if (timerState.timeout) {
                    expired = true
                    emitState()
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

    private fun handleUpdatedPriceImpactState(priceImpactState: PriceImpactService.State) {
        this.priceImpactState = priceImpactState

        swapDefenseSystemService.setPriceImpact(priceImpactState.priceImpact, priceImpactState.priceImpactLevel)

        emitState()
    }

    override fun createState() = SwapConfirmUiState(
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
        networkFee = sendTransactionState.networkFee,
        cautions = sendTransactionState.cautions,
        validQuote = error == null && sendTransactionState.sendable,
        expired = expired,
        priceImpact = priceImpactState.priceImpact,
        priceImpactLevel = priceImpactState.priceImpactLevel,
        quoteFields = quoteFields,
        transactionFields = sendTransactionState.fields,
        hasSettings = sendTransactionService.hasSettings,
        hasNonceSettings = sendTransactionService.hasNonceSettings,
        swapDefenseSystemMessage = swapDefenseState.systemMessage,
        mevProtectionEnabled = swapDefenseState.mevProtectionEnabled,
        supportsMevProtection = sendTransactionService.supportsMevProtection &&
                swapProvider.mevProtectionAllowed(tokenIn, tokenOut),
        mevProtectionActionAllowed = swapDefenseState.mevProtectionActionAllowed,
        recipient = recipient,
        recipientRequired = recipientRequired,
        slippage = slippage,
        estimatedTime = estimatedTime,
        providerType = swapProvider.type,
        error = error
    )

    override fun onCleared() {
        timerService.stop()
    }

    fun refresh(silent: Boolean = false) {
        if (isInBackground) return

        expired = false

        if (!silent) {
            loading = true
            emitState()
        }

        sendTransactionService.refreshUuid()
        fetchFinalQuote()

        stat(page = StatPage.SwapConfirmation, event = StatEvent.Refresh)
    }

    private fun fetchFinalQuote() {
        fetchFinalQuoteJob?.cancel()
        fetchFinalQuoteJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                error = null

                val finalQuote = swapProvider.fetchFinalQuote(
                    tokenIn,
                    tokenOut,
                    amountIn,
                    sendTransactionSettings,
                    swapQuote,
                    recipient,
                    slippage ?: IMultiSwapProvider.DEFAULT_SLIPPAGE,
                )

                ensureActive()

                expired = false
                amountOut = finalQuote.amountOut
                amountOutMin = finalQuote.amountOutMin
                estimatedTime = finalQuote.estimatedTime
                quoteFields = finalQuote.fields
                slippage = finalQuote.slippage
                providerSwapId = finalQuote.providerSwapId
                fromAsset = finalQuote.fromAsset
                toAsset = finalQuote.toAsset
                depositAddress = finalQuote.depositAddress
                emitState()

                fiatServiceOut.setAmount(amountOut)
                fiatServiceOutMin.setAmount(amountOutMin)
                sendTransactionService.setSendTransactionData(finalQuote.sendTransactionData)

                priceImpactService.setProviderTitle(swapProvider.title)
            } catch (e: CancellationException) {
                throw e
            } catch (e: RetryableSwapError) {
                // in this case we should keep state as loading
                // temp solution. need find better one
            } catch (t: Throwable) {
                loading = false
                initialLoading = false
                error = t

                emitState()
            }
        }
    }

    suspend fun swap() = withContext(Dispatchers.Default) {
        timerService.stop()

        stat(page = StatPage.SwapConfirmation, event = StatEvent.Send)

        val result = sendTransactionService.sendTransaction(swapDefenseState.mevProtectionEnabled)
        saveSwapRecord(result)

        // remember the delivery address so the recipient page can suggest it next time
        recipient?.let {
            App.recentAddressManager.setRecentAddress(it, tokenOut.blockchainType)
        }

        result
    }

    private suspend fun saveSwapRecord(result: SendTransactionResult) {
        // The broadcast hash becomes the record's inboundTxHash — required for tracking DEX
        // swaps (Jupiter, LI.FI), where the server polls the provider's status by the source
        // chain transaction.
        val transactionHash = when (result) {
            is SendTransactionResult.Evm -> result.transactionHash
            is SendTransactionResult.Btc -> result.transactionRecord?.transactionHash
            is SendTransactionResult.Zcash -> result.transactionHash
            is SendTransactionResult.Solana -> result.txHash
            is SendTransactionResult.Tron -> result.txHash
            is SendTransactionResult.Thorchain -> result.txHash
            is SendTransactionResult.Stellar -> result.txHash
            else -> null
        }

        val record = SwapRecord(
            accountId = App.accountManager.activeAccount?.id ?: "",
            timestamp = System.currentTimeMillis(),
            providerId = swapProvider.id,
            providerName = swapProvider.title,
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
            amountOut = amountOut?.toPlainString(),
            amountOutMin = amountOutMin?.toPlainString(),
            recipientAddress = recipient?.hex ?: SwapHelper.getReceiveAddressForToken(tokenOut),
            customRecipientAddress = recipient != null,
            sourceAddress = SwapHelper.getSendingAddressForToken(tokenIn),
            transactionHash = transactionHash,
            providerSwapId = providerSwapId,
            fromAsset = fromAsset,
            toAsset = toAsset,
            depositAddress = depositAddress,
            status = SwapStatus.Depositing.name,
            estimatedTime = estimatedTime,
        )
        App.swapRecordManager.save(record)
    }

    fun setRecipient(recipient: Address?) {
        // a mandatory external recipient can be changed but never cleared
        if (recipient == null && recipientRequired) return

        this.recipient = recipient

        refresh()
    }

    fun setSlippage(slippage: BigDecimal) {
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
                SwapDefenseSystemService(
                    sendTransactionService.supportsMevProtection &&
                            quote.provider.mevProtectionAllowed(quote.tokenIn, quote.tokenOut),
                    App.paidActionSettingsManager
                ),
                App.backgroundManager,
                initialRecipient,
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
    val validQuote: Boolean,
    val expired: Boolean,
    val priceImpact: BigDecimal?,
    val priceImpactLevel: PriceImpactLevel?,
    val quoteFields: List<DataField>,
    val transactionFields: List<DataField>,
    val hasSettings: Boolean,
    val hasNonceSettings: Boolean,
    val swapDefenseSystemMessage: DefenseSystemMessage?,
    val mevProtectionEnabled: Boolean,
    val supportsMevProtection: Boolean,
    val mevProtectionActionAllowed: Boolean,
    val recipient: Address?,
    val recipientRequired: Boolean,
    val slippage: BigDecimal?,
    val estimatedTime: Long?,
    val providerType: SwapProviderType,
    val error: Throwable?,
)
