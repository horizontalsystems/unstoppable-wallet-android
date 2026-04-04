package cash.p.terminal.modules.multiswap

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.getKoinInstance
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.storage.PendingMultiSwapStorage
import cash.p.terminal.entities.PendingMultiSwap
import cash.p.terminal.modules.send.BaseSendViewModel
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.core.HSCaution
import cash.p.terminal.core.ethereum.CautionViewItem
import cash.p.terminal.modules.multiswap.providers.ChangeNowProvider
import cash.p.terminal.modules.multiswap.providers.IMultiSwapProvider
import cash.p.terminal.modules.multiswap.providers.QuickexProvider
import cash.p.terminal.modules.multiswap.sendtransaction.ISendTransactionService
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionData
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionResult
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionServiceState
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionSettings
import cash.p.terminal.modules.multiswap.sendtransaction.SwapTransactionServiceFactory
import cash.p.terminal.modules.multiswap.ui.DataField
import cash.p.terminal.modules.send.SendModule
import cash.p.terminal.modules.send.SendResult
import cash.p.terminal.network.changenow.data.entity.BackendChangeNowResponseError
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.wallet.Token
import com.tangem.common.core.TangemSdkError
import io.horizontalsystems.bitcoincore.managers.SendValueErrors
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.core.entities.Currency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import java.math.BigDecimal
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class SwapConfirmViewModel(
    private val swapProvider: IMultiSwapProvider,
    private val swapQuote: ISwapQuote,
    private val swapSettings: Map<String, Any?>,
    currencyManager: CurrencyManager,
    private val fiatServiceIn: FiatService,
    private val fiatServiceOut: FiatService,
    private val fiatServiceOutMin: FiatService,
    val sendTransactionService: ISendTransactionService<*>,
    private val timerService: TimerService,
    private val priceImpactService: PriceImpactService,
    wallet: Wallet,
    adapterManager: IAdapterManager,
    private val multiSwapLegInfo: MultiSwapLegInfo? = null,
) : BaseSendViewModel<SwapConfirmUiState>(wallet, adapterManager) {
    private val accountId: String = wallet.account.id
    private val localStorage: ILocalStorage by inject(ILocalStorage::class.java)
    private val pendingMultiSwapStorage: PendingMultiSwapStorage by inject(PendingMultiSwapStorage::class.java)

    var sendResult by mutableStateOf<SendResult?>(null)
        private set

    var completedMultiSwapId by mutableStateOf<String?>(null)
        private set

    override fun getEstimatedFee(): BigDecimal? = sendTransactionState.networkFee?.primary?.value
    override fun onSendRequested() = executeSwap()

    private var sendTransactionSettings: SendTransactionSettings? = null
    private val currency = currencyManager.baseCurrency
    private val tokenIn = swapQuote.tokenIn
    private val tokenOut = swapQuote.tokenOut
    private var amountIn = swapQuote.amountIn
    private var fiatAmountIn: BigDecimal? = null

    private var fiatAmountOut: BigDecimal? = null
    private var fiatAmountOutMin: BigDecimal? = null

    private var loading = true
    private var timerState = timerService.stateFlow.value
    private var sendTransactionState = sendTransactionService.stateFlow.value
    private var priceImpactState = priceImpactService.stateFlow.value

    private var amountOut: BigDecimal? = null
    private var amountOutMin: BigDecimal? = null
    private var quoteFields: List<DataField> = listOf()
    private var criticalError: String? = null
    private var isAdvancedSettingsAvailable: Boolean = sendTransactionService.hasSettings()
    private var fetchJob: Job? = null
    private val mevProtectionAvailable =
        swapProvider.mevProtectionAvailable && sendTransactionService.mevProtectionAvailable

    init {
        fiatServiceIn.setCurrency(currency)
        fiatServiceIn.setToken(tokenIn)
        fiatServiceIn.setAmount(amountIn)
        addCloseable(fiatServiceIn)

        fiatServiceOut.setCurrency(currency)
        fiatServiceOut.setToken(tokenOut)
        fiatServiceOut.setAmount(amountOut)
        addCloseable(fiatServiceOut)

        fiatServiceOutMin.setCurrency(currency)
        fiatServiceOutMin.setToken(tokenOut)
        fiatServiceOutMin.setAmount(amountOutMin)
        addCloseable(fiatServiceOutMin)

        viewModelScope.launch {
            fiatServiceIn.stateFlow.collect {
                fiatAmountIn = it.fiatAmount
                emitState()
            }
        }

        viewModelScope.launch {
            fiatServiceOut.stateFlow.collect {
                fiatAmountOut = it.fiatAmount
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

                fetchFinalQuote()
            }
        }

        viewModelScope.launch {
            sendTransactionService.stateFlow.collect { transactionState ->
                sendTransactionState = transactionState

                loading = transactionState.loading

                emitState()

                if (isSendable() && needUseTimer()) {
                    timerService.start(10)
                }
            }
        }

        viewModelScope.launch {
            timerService.stateFlow.collect {
                timerState = it

                emitState()
            }
        }

        viewModelScope.launch {
            priceImpactService.stateFlow.collect {
                handleUpdatedPriceImpactState(it)
            }
        }

        viewModelScope.launch {
            sendTransactionService.stateFlow.collectLatest {
                if (it.availableBalance != null && it.availableBalance < amountIn) {
                    amountIn = it.availableBalance
                    fiatServiceIn.setAmount(amountIn)
                    refresh()
                }
            }
        }

        sendTransactionService.start(viewModelScope)

        fetchFinalQuote()
    }

    private fun handleUpdatedPriceImpactState(priceImpactState: PriceImpactService.State) {
        this.priceImpactState = priceImpactState

        emitState()
    }

    override fun createState(): SwapConfirmUiState {
        var cautions = sendTransactionState.cautions

        if (cautions.isEmpty()) {
            priceImpactState.priceImpactCaution?.let { hsCaution ->
                cautions = listOf(
                    CautionViewItem(
                        hsCaution.s.toString(),
                        hsCaution.description.toString(),
                        when (hsCaution.type) {
                            HSCaution.Type.Error -> CautionViewItem.Type.Error
                            HSCaution.Type.Warning -> CautionViewItem.Type.Warning
                        }
                    )
                )
            }
        }

        return SwapConfirmUiState(
            expiresIn = timerState.remaining,
            expired = timerState.timeout,
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
            cautions = cautions,
            feeCaution = sendTransactionState.feeCaution,
            validQuote = isSendable(),
            priceImpact = priceImpactState.priceImpact,
            priceImpactLevel = priceImpactState.priceImpactLevel,
            quoteFields = quoteFields,
            transactionFields = sendTransactionState.fields,
            criticalError = criticalError,
            isAdvancedSettingsAvailable = isAdvancedSettingsAvailable,
            mevProtectionAvailable = mevProtectionAvailable,
            mevProtectionEnabled = localStorage.swapMevProtectionEnabled && mevProtectionAvailable,
        )
    }

    private fun isSendable(): Boolean {
        return swapProvider is ChangeNowProvider ||
                swapProvider is QuickexProvider ||
                sendTransactionState.sendable
    }

    private fun needUseTimer() =
        swapProvider !is ChangeNowProvider &&
        swapProvider !is QuickexProvider

    override fun onCleared() {
        timerService.stop()
    }

    fun refresh() {
        loading = true
        emitState()

        fetchFinalQuote()
    }

    private fun fetchFinalQuote() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val finalQuote = swapProvider.fetchFinalQuote(
                    tokenIn = tokenIn,
                    tokenOut = tokenOut,
                    amountIn = amountIn,
                    swapSettings = swapSettings,
                    sendTransactionSettings = sendTransactionSettings,
                    swapQuote = swapQuote
                )

                amountOut = finalQuote.amountOut
                amountOutMin = finalQuote.amountOutMin
                quoteFields = finalQuote.fields
                criticalError = null

                fiatServiceOut.setAmount(amountOut)
                fiatServiceOutMin.setAmount(amountOutMin)
                sendTransactionService.setSendTransactionData(finalQuote.sendTransactionData)

                priceImpactService.setPriceImpact(finalQuote.priceImpact?.negate(), swapProvider.title)

                emitState()
            } catch (e: BackendChangeNowResponseError) {
                e.printStackTrace()
                loading = false
                criticalError = when (e.error) {
                    BackendChangeNowResponseError.NOT_VALID_REFUND_ADDRESS -> {
                        Translator.getString(R.string.unsupported_refund_address)
                    }

                    BackendChangeNowResponseError.NOT_VALID_ADDRESS -> {
                        Translator.getString(R.string.unsupported_address)
                    }

                    else -> {
                        Translator.getString(R.string.unexpected_error)
                    }
                }
                emitState()
            } catch (_: CancellationException) {
                Timber.w("fetchFinalQuote was cancelled")
            } catch (t: Throwable) {
                Timber.e(t, "fetchFinalQuote error")
                loading = false
                criticalError = Translator.getString(R.string.unexpected_error)
                emitState()
            }
        }
    }

    suspend fun swap() = withContext(Dispatchers.Default) {
        sendTransactionService.sendTransaction(uiState.mevProtectionEnabled)
    }

    fun toggleMevProtection(enabled: Boolean) {
        localStorage.swapMevProtectionEnabled = enabled

        emitState()
    }

    private fun executeSwap() {
        if (sendResult == SendResult.Sending) return
        sendResult = SendResult.Sending

        viewModelScope.launch {
            try {
                val result = swap()
                handleMultiSwapCompletion(result)
                onTransactionCompleted(result)

                sendResult = if (result is SendTransactionResult.Btc && result.isQueued) {
                    SendResult.SentButQueued()
                } else {
                    SendResult.Sent()
                }
            } catch (e: TangemSdkError.UserCancelled) {
                // User cancelled - just reset state, no error message
                sendResult = null
            } catch (e: TangemSdkError) {
                // Other Tangem errors - reset state
                sendResult = null
            } catch (t: Throwable) {
                val caution = if (t.cause is SendValueErrors.InsufficientUnspentOutputs) {
                    HSCaution(TranslatableString.ResString(R.string.EthereumTransaction_Error_InsufficientBalance_Title))
                } else {
                    HSCaution(TranslatableString.PlainString(t.javaClass.simpleName))
                }
                sendResult = SendResult.Failed(caution)
            }
        }
    }

    private suspend fun handleMultiSwapCompletion(result: SendTransactionResult) {
        val legInfo = multiSwapLegInfo ?: return
        when (legInfo) {
            is MultiSwapLegInfo.Leg1 -> createPendingMultiSwap(legInfo, result)
            is MultiSwapLegInfo.Leg2 -> updatePendingMultiSwapLeg2(legInfo)
        }
    }

    private suspend fun createPendingMultiSwap(
        legInfo: MultiSwapLegInfo.Leg1,
        result: SendTransactionResult,
    ) {
        val id = UUID.randomUUID().toString()
        val record = PendingMultiSwap(
            id = id,
            accountId = accountId,
            createdAt = System.currentTimeMillis(),
            coinUidIn = legInfo.coinUidIn,
            blockchainTypeIn = legInfo.blockchainTypeIn,
            amountIn = legInfo.amountIn,
            coinUidIntermediate = legInfo.coinUidIntermediate,
            blockchainTypeIntermediate = legInfo.blockchainTypeIntermediate,
            coinUidOut = legInfo.coinUidOut,
            blockchainTypeOut = legInfo.blockchainTypeOut,
            leg1ProviderId = legInfo.leg1ProviderId,
            leg1IsOffChain = swapProvider is ChangeNowProvider || swapProvider is QuickexProvider,
            leg1TransactionId = result.getRecordUid(),
            leg1AmountOut = amountOut,
            leg1Status = PendingMultiSwap.STATUS_EXECUTING,
            leg2ProviderId = legInfo.leg2ProviderId,
            leg2IsOffChain = legInfo.leg2IsOffChain,
            leg2TransactionId = null,
            leg2AmountOut = null,
            leg2Status = PendingMultiSwap.STATUS_PENDING,
            expectedAmountOut = legInfo.expectedAmountOut,
        )
        pendingMultiSwapStorage.insert(record)
        swapProvider.getProviderTransactionId()?.let { providerTxId ->
            pendingMultiSwapStorage.setLeg1ProviderTransactionId(id, providerTxId)
        }
        completedMultiSwapId = id
    }

    private suspend fun updatePendingMultiSwapLeg2(
        legInfo: MultiSwapLegInfo.Leg2,
    ) {
        pendingMultiSwapStorage.delete(legInfo.pendingMultiSwapId)
        completedMultiSwapId = legInfo.pendingMultiSwapId
    }

    fun onTransactionCompleted(result: SendTransactionResult) {
        if (swapProvider is ChangeNowProvider) {
            swapProvider.onTransactionCompleted(result)
        } else if (swapProvider is QuickexProvider) {
            swapProvider.onTransactionCompleted(result)
        }
    }

    companion object {

        fun provideFactory(
            quote: SwapProviderQuote,
            settings: Map<String, Any?>,
            navController: NavController,
            multiSwapLegInfo: MultiSwapLegInfo? = null,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val wallet = App.walletManager.activeWallets
                    .find { it.token == quote.tokenIn }

                val sendTransactionService = try {
                    checkNotNull(wallet) { "Wallet not found for ${quote.tokenIn}" }
                    SwapTransactionServiceFactory.create(quote.tokenIn, quote.provider)
                } catch (e: Exception) {
                    Toast.makeText(App.instance, R.string.unsupported_token, Toast.LENGTH_SHORT)
                        .show()
                    navController.popBackStack()

                    // Build a dummy service (sendable=false) so the ViewModel is
                    // inoperable while the screen navigates back.
                    object : ISendTransactionService<Nothing>(quote.tokenIn) {
                        override fun start(coroutineScope: CoroutineScope) = Unit
                        override suspend fun setSendTransactionData(data: SendTransactionData) =
                            Unit

                        override fun hasSettings(): Boolean = false

                        @Composable
                        override fun GetSettingsContent(navController: NavController) = Unit
                        override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult =
                            SendTransactionResult.Solana(SendResult.Sending)

                        override val sendTransactionSettingsFlow: StateFlow<SendTransactionSettings>
                            get() = MutableStateFlow<SendTransactionSettings>(
                                SendTransactionSettings.Common
                            )

                        override fun createState(): SendTransactionServiceState =
                            SendTransactionServiceState(
                                availableBalance = null,
                                networkFee = null,
                                cautions = listOf(),
                                sendable = false,
                                loading = false,
                                fields = listOf(),
                                extraFees = mapOf()
                            )
                    }
                }

                // When wallet is null the dummy service above (sendable=false)
                // prevents any swap execution while the screen navigates back.
                val marketKit: MarketKitWrapper = getKoinInstance()
                return SwapConfirmViewModel(
                    swapProvider = quote.provider,
                    swapQuote = quote.swapQuote,
                    swapSettings = settings,
                    currencyManager = App.currencyManager,
                    fiatServiceIn = FiatService(marketKit),
                    fiatServiceOut = FiatService(marketKit),
                    fiatServiceOutMin = FiatService(marketKit),
                    sendTransactionService = sendTransactionService,
                    timerService = TimerService(),
                    priceImpactService = PriceImpactService(),
                    wallet = wallet ?: App.walletManager.activeWallets.first(),
                    adapterManager = App.adapterManager,
                    multiSwapLegInfo = multiSwapLegInfo,
                ) as T
            }
        }
    }
}

sealed class MultiSwapLegInfo {
    data class Leg1(
        val coinUidIn: String,
        val blockchainTypeIn: String,
        val amountIn: BigDecimal,
        val coinUidIntermediate: String,
        val blockchainTypeIntermediate: String,
        val coinUidOut: String,
        val blockchainTypeOut: String,
        val leg1ProviderId: String,
        val leg2ProviderId: String,
        val leg2IsOffChain: Boolean,
        val expectedAmountOut: BigDecimal,
    ) : MultiSwapLegInfo()

    data class Leg2(
        val pendingMultiSwapId: String,
    ) : MultiSwapLegInfo()
}

data class SwapConfirmUiState(
    val expiresIn: Long?,
    val expired: Boolean,
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
    val feeCaution: CautionViewItem? = null,
    val validQuote: Boolean,
    val priceImpact: BigDecimal?,
    val priceImpactLevel: PriceImpactLevel?,
    val quoteFields: List<DataField>,
    val transactionFields: List<DataField>,
    val criticalError: String? = null,
    var isAdvancedSettingsAvailable: Boolean,
    val mevProtectionAvailable: Boolean,
    val mevProtectionEnabled: Boolean,
)
