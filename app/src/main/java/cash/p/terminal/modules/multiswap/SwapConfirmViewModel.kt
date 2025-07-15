package cash.p.terminal.modules.multiswap

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.HSCaution
import cash.p.terminal.core.ethereum.CautionViewItem

import cash.p.terminal.modules.multiswap.providers.IMultiSwapProvider
import cash.p.terminal.modules.multiswap.providers.changenow.ChangeNowProvider
import cash.p.terminal.modules.multiswap.sendtransaction.ISendTransactionService
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionData
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionResult
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionServiceFactory
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionServiceState
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionSettings
import cash.p.terminal.modules.multiswap.ui.DataField
import cash.p.terminal.modules.send.SendModule
import cash.p.terminal.modules.send.SendResult
import cash.p.terminal.network.changenow.data.entity.BackendChangeNowResponseError
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.wallet.Token
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.core.ViewModelUiState
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.entities.Currency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

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
    private val priceImpactService: PriceImpactService
) : ViewModelUiState<SwapConfirmUiState>() {
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
    private var isAdvancedSettingsAvailable: Boolean = tokenIn.blockchainType != BlockchainType.Dogecoin

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
            validQuote = isSendable(),
            priceImpact = priceImpactState.priceImpact,
            priceImpactLevel = priceImpactState.priceImpactLevel,
            quoteFields = quoteFields,
            transactionFields = sendTransactionState.fields,
            criticalError = criticalError,
            isAdvancedSettingsAvailable = isAdvancedSettingsAvailable
        )
    }

    private fun isSendable(): Boolean {
        return swapProvider is ChangeNowProvider || sendTransactionState.sendable
    }

    private fun needUseTimer() = swapProvider !is ChangeNowProvider

    override fun onCleared() {
        timerService.stop()
    }

    fun refresh() {
        loading = true
        emitState()

        fetchFinalQuote()
    }

    private fun fetchFinalQuote() {
        viewModelScope.launch(Dispatchers.IO) {
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
                loading = false
                criticalError = null

                fiatServiceOut.setAmount(amountOut)
                fiatServiceOutMin.setAmount(amountOutMin)
                sendTransactionService.setSendTransactionData(finalQuote.sendTransactionData)

                priceImpactService.setPriceImpact(finalQuote.priceImpact, swapProvider.title)
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
            } catch (t: Throwable) {
                Log.e("AAA", "fetchFinalQuote error", t)
                loading = false
                criticalError = Translator.getString(R.string.unexpected_error)
                emitState()
            }
        }
    }

    suspend fun swap() = withContext(Dispatchers.Default) {
        sendTransactionService.sendTransaction()
    }

    fun onTransactionCompleted(result: SendTransactionResult) {
        if (swapProvider is ChangeNowProvider) {
            swapProvider.onTransactionCompleted(result)
        }
    }

    companion object {

        fun provideFactory(
            quote: SwapProviderQuote,
            settings: Map<String, Any?>,
            navController: NavController
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val sendTransactionService = try {
                    SendTransactionServiceFactory.create(quote.tokenIn)
                } catch (e: Exception) {
                    Toast.makeText(App.instance, R.string.unsupported_token, Toast.LENGTH_SHORT)
                        .show()
                    navController.popBackStack()

                    // Build a dummy service to avoid null pointer exceptions
                    object : ISendTransactionService<Nothing>(quote.tokenIn) {
                        override fun start(coroutineScope: CoroutineScope) = Unit
                        override fun setSendTransactionData(data: SendTransactionData) = Unit

                        @Composable
                        override fun GetSettingsContent(navController: NavController) = Unit
                        override suspend fun sendTransaction(): SendTransactionResult =
                            SendTransactionResult.Common(SendResult.Sending)

                        override val sendTransactionSettingsFlow: StateFlow<SendTransactionSettings>
                            get() = MutableStateFlow<SendTransactionSettings>(
                                SendTransactionSettings.Common
                            )

                        override fun createState(): SendTransactionServiceState =
                            SendTransactionServiceState(
                                null,
                                null,
                                listOf(),
                                false,
                                false,
                                listOf()
                            )
                    }
                }
                return SwapConfirmViewModel(
                    swapProvider = quote.provider,
                    swapQuote = quote.swapQuote,
                    swapSettings = settings,
                    currencyManager = App.currencyManager,
                    fiatServiceIn = FiatService(App.marketKit),
                    fiatServiceOut = FiatService(App.marketKit),
                    fiatServiceOutMin = FiatService(App.marketKit),
                    sendTransactionService = sendTransactionService,
                    timerService = TimerService(),
                    priceImpactService = PriceImpactService()
                ) as T
            }
        }
    }
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
    val validQuote: Boolean,
    val priceImpact: BigDecimal?,
    val priceImpactLevel: PriceImpactLevel?,
    val quoteFields: List<DataField>,
    val transactionFields: List<DataField>,
    val criticalError: String? = null,
    var isAdvancedSettingsAvailable: Boolean
)