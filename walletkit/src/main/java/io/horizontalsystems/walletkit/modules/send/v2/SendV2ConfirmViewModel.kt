package io.horizontalsystems.walletkit.modules.send.v2

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAdapterManager
import io.horizontalsystems.walletkit.core.IBalanceAdapter
import io.horizontalsystems.walletkit.core.HSCaution
import io.horizontalsystems.walletkit.core.LocalizedException
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.chain.SendChainSettings
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.core.managers.RecentAddressManager
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.CurrencyValue
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.contacts.ContactsRepository
import io.horizontalsystems.walletkit.modules.contacts.model.Contact
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionResult
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceFactory
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataField
import io.horizontalsystems.walletkit.modules.send.SendResult
import io.horizontalsystems.walletkit.modules.xrate.XRateService
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException

data class SendV2ConfirmUiState(
    val wallet: Wallet,
    val amount: BigDecimal,
    val address: Address,
    val contact: Contact?,
    val memo: String?,
    val rate: CurrencyValue?,
    val fee: BigDecimal?,
    val feeCoin: Coin,
    val feeCoinDecimals: Int,
    val feeCoinRate: CurrencyValue?,
    val fields: List<DataField>,
    val cautions: List<CautionViewItem>,
    val sendable: Boolean,
    val loading: Boolean,
    val hasSettings: Boolean,
    val hasNonceSettings: Boolean,
    val networkFeeInfoRes: Int?,
    val error: Throwable?,
)

/**
 * Confirmation of a plain transfer for any blockchain type. The chain's send-transaction
 * service owns the fee, its settings, cautions and the send itself; this view model feeds
 * it the transfer and presents its state.
 *
 * It is also the one place that handles sending the whole balance of a coin that pays its
 * own fee: the requested amount is submitted first to learn the fee, then reduced so that
 * amount plus fee fits the balance, and resubmitted. Services report the fee for the
 * requested amount; only those whose maximum is not "amount minus fee" answer
 * [AbstractSendTransactionService.maxSendableAmount].
 */
class SendV2ConfirmViewModel(
    private val wallet: Wallet,
    private val amount: BigDecimal,
    private val address: Address,
    private val memo: String?,
    private val chainSettings: SendChainSettings?,
    val sendTransactionService: AbstractSendTransactionService,
    private val xRateService: XRateService,
    private val contactsRepository: ContactsRepository,
    private val recentAddressManager: RecentAddressManager,
    adapterManager: IAdapterManager,
) : ViewModelUiState<SendV2ConfirmUiState>() {

    var sendResult by mutableStateOf<SendResult?>(null)
        private set

    private val token = wallet.token
    private val chainPlugin = ChainRegistry[token.blockchainType]
    private var serviceState = sendTransactionService.stateFlow.value

    // Sending everything: the fee has to come out of the amount, since the balance cannot
    // cover both. Detected from the amount alone, so a typed full balance counts as well.
    private val sendMax = feePaidFromAsset(token.type) && run {
        val available = chainPlugin?.sendAvailableBalance(token, chainSettings)
            ?: adapterManager.getAdapterForWallet<IBalanceAdapter>(wallet)?.balanceData?.available
        available != null && amount.compareTo(available) == 0
    }
    private var submittedAmount: BigDecimal? = null
    private var adjustedAmount: BigDecimal? = null
    private var adjusting = sendMax
    // Targets already submitted in this session. A fee-settings edit yields a new target and
    // is followed; a fee that alternates between amounts revisits one and is stopped there.
    private val triedTargets = mutableSetOf<BigDecimal>()
    private var maxSendCaution: CautionViewItem? = null
    private var rate = xRateService.getRate(token.coin.uid)
    private var feeCoinRate: CurrencyValue? = null
    private var error: Throwable? = null

    // The chain service and its sub-services are plain objects that both their own
    // collectors and each submitted transfer mutate. One confined thread for all of that
    // keeps their state consistent, and the mutex keeps submissions from interleaving.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val serviceDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val serviceScope = CoroutineScope(viewModelScope.coroutineContext + serviceDispatcher)
    private val submitMutex = Mutex()

    private val contact = contactsRepository.getContactsFiltered(
        token.blockchainType,
        addressQuery = address.hex
    ).firstOrNull()

    init {
        viewModelScope.launch {
            xRateService.getRateFlow(token.coin.uid).collect {
                rate = it
                refreshFeeCoinRate()
                emitState()
            }
        }
        viewModelScope.launch {
            sendTransactionService.stateFlow.collect {
                serviceState = it
                refreshFeeCoinRate()
                if (sendMax) reconcileMaxSend()
                emitState()
            }
        }

        refreshFeeCoinRate()
        sendTransactionService.start(serviceScope)

        viewModelScope.launch { submit(amount) }
    }

    // Services may reach the network while taking the transfer (fee estimates, account
    // lookups), so this never runs on the main thread; see serviceDispatcher.
    private suspend fun submit(value: BigDecimal) = withContext(serviceDispatcher) {
        submitMutex.withLock {
            // A previous attempt may have failed (node error); this one starts clean.
            if (error != null) {
                error = null
                emitState()
            }
            try {
                val data = chainPlugin?.sendTransactionData(token, value, address.hex, memo, chainSettings)
                    ?: throw UnsupportedOperationException(token.blockchainType.uid)
                submittedAmount = value
                sendTransactionService.setSendTransactionData(data)
            } catch (e: Throwable) {
                error = e
                // Nothing will settle the adjustment now; let the screen show the failure.
                adjusting = false
                emitState()
            }
        }
    }

    // Runs on every service update while sending the maximum. The service's own cautions
    // are hidden until the submitted amount matches the target: the first, full-amount round
    // legitimately reports a shortfall that the reduced amount then clears.
    private fun reconcileMaxSend() {
        if (serviceState.loading) return

        val fee = feeCoinValue()?.takeIf { it.coin.uid == token.coin.uid }?.value
        val target = sendTransactionService.maxSendableAmount() ?: fee?.let { amount - it }
        if (target == null) {
            // The service settled without a fee (estimate failed, or the full amount cannot
            // be built): nothing to adjust to, and its own cautions must show.
            adjusting = false
            return
        }

        if (target <= BigDecimal.ZERO) {
            maxSendCaution = CautionViewItem(
                title = Translator.getString(R.string.EthereumTransaction_Error_InsufficientBalance_Title),
                text = Translator.getString(R.string.EthereumTransaction_Error_InsufficientBalanceForFee, token.coin.code),
                type = CautionViewItem.Type.Error,
            )
            adjustedAmount = null
            adjusting = false
            return
        }
        maxSendCaution = null

        if (submittedAmount?.compareTo(target) == 0) {
            adjustedAmount = target
            adjusting = false
            return
        }

        val key = target.stripTrailingZeros()
        if (key in triedTargets || triedTargets.size >= MAX_ADJUSTMENT_ROUNDS) {
            // Chasing itself; the last submitted estimate stands.
            adjusting = false
            return
        }
        triedTargets.add(key)
        adjusting = true
        adjustedAmount = target
        viewModelScope.launch { submit(target) }
    }

    private fun feePaidFromAsset(type: TokenType) = when (type) {
        TokenType.Native,
        is TokenType.Derived,
        is TokenType.AddressTyped -> true
        else -> false
    }

    private fun feeCoinValue() = serviceState.networkFee?.primary?.coinValue
    private fun feeCoin(): Coin = feeCoinValue()?.coin ?: token.coin

    // The fee is not always paid in the sent token (ERC20 fees are in the chain's coin), so
    // its rate is looked up separately once the service reports the fee coin.
    private fun refreshFeeCoinRate() {
        val feeCoin = feeCoin()
        feeCoinRate = if (feeCoin.uid == token.coin.uid) rate else xRateService.getRate(feeCoin.uid)
    }

    override fun createState() = SendV2ConfirmUiState(
        wallet = wallet,
        amount = adjustedAmount ?: amount,
        address = address,
        contact = contact,
        memo = memo,
        rate = rate,
        fee = serviceState.networkFee?.primary?.coinValue?.value,
        feeCoin = feeCoin(),
        feeCoinDecimals = feeCoinValue()?.decimal ?: token.decimals,
        feeCoinRate = feeCoinRate,
        fields = serviceState.fields,
        cautions = if (adjusting) listOf() else listOfNotNull(maxSendCaution) + serviceState.cautions,
        sendable = serviceState.sendable && !adjusting && maxSendCaution == null,
        loading = serviceState.loading || adjusting,
        hasSettings = sendTransactionService.hasSettings,
        hasNonceSettings = sendTransactionService.hasNonceSettings,
        networkFeeInfoRes = sendTransactionService.networkFeeInfoRes,
        error = error,
    )

    fun onClickSend() {
        viewModelScope.launch(Dispatchers.IO) {
            send()
        }
    }

    /** The failure has been shown; a later recomposition must not show it again. */
    fun onFailureShown() {
        if (sendResult is SendResult.Failed) {
            sendResult = null
        }
    }

    // The service's fields are read while broadcasting, so this takes the same confined
    // thread and lock as the submissions that write them.
    private suspend fun send() = withContext(serviceDispatcher) {
        submitMutex.withLock { broadcast() }
    }

    private suspend fun broadcast() {
        sendResult = SendResult.Sending
        val result = try {
            sendTransactionService.sendTransaction()
        } catch (e: Throwable) {
            sendResult = SendResult.Failed(createCaution(e))
            return
        }
        sendResult = SendResult.Sent(txHash = txHash(result))

        // Bookkeeping after a broadcast transaction; its failure is not the send's.
        try {
            recentAddressManager.setRecentAddress(address, token.blockchainType)
        } catch (e: Throwable) {
            Log.w("SendV2ConfirmViewModel", "recent address not saved", e)
        }
    }

    private fun txHash(result: SendTransactionResult): String? = when (result) {
        is SendTransactionResult.Evm -> result.transactionHash
        is SendTransactionResult.Btc -> result.transactionRecord?.transactionHash
        is SendTransactionResult.Tron -> result.txHash
        is SendTransactionResult.Stellar -> result.txHash
        is SendTransactionResult.Xrp -> result.txHash
        is SendTransactionResult.Solana -> result.txHash
        is SendTransactionResult.Zcash -> result.transactionHash
        is SendTransactionResult.Monero -> result.txHash
        is SendTransactionResult.Thorchain -> result.txHash
        is SendTransactionResult.Zano -> result.txHash
        SendTransactionResult.Ton -> null
    }

    private fun createCaution(error: Throwable) = when (error) {
        is UnknownHostException -> HSCaution(TranslatableString.ResString(R.string.Hud_Text_NoInternet))
        is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
        else -> HSCaution(TranslatableString.PlainString(error.message ?: error.javaClass.simpleName))
    }

    private companion object {
        // Every entry is a real resubmission and estimate; fee edits are the only realistic
        // way to accumulate them, and this is far more than a user makes in one sitting.
        const val MAX_ADJUSTMENT_ROUNDS = 20
    }

    class Factory(
        private val input: SendV2ConfirmPage.Input,
        private val chainSettings: SendChainSettings?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SendV2ConfirmViewModel(
                wallet = input.wallet,
                amount = input.amount,
                address = input.address,
                memo = input.memo,
                chainSettings = chainSettings,
                sendTransactionService = SendTransactionServiceFactory.create(input.wallet.token),
                xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency),
                contactsRepository = App.contactsRepository,
                recentAddressManager = App.recentAddressManager,
                adapterManager = App.adapterManager,
            ) as T
        }
    }
}
