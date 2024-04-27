package cash.p.terminal.modules.multiswap

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import cash.p.terminal.core.App
import cash.p.terminal.core.HSCaution
import cash.p.terminal.core.ViewModelUiState
import cash.p.terminal.core.ethereum.CautionViewItem
import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.entities.Currency
import cash.p.terminal.modules.multiswap.providers.IMultiSwapProvider
import cash.p.terminal.modules.multiswap.sendtransaction.ISendTransactionService
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionServiceFactory
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionSettings
import cash.p.terminal.modules.multiswap.ui.DataField
import cash.p.terminal.modules.send.SendModule
import cash.p.terminal.modules.swap.SwapMainModule
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class SwapConfirmViewModel(
    private val swapProvider: IMultiSwapProvider,
    swapQuote: ISwapQuote,
    private val swapSettings: Map<String, Any?>,
    private val currencyManager: CurrencyManager,
    private val fiatServiceIn: FiatService,
    private val fiatServiceOut: FiatService,
    private val fiatServiceOutMin: FiatService,
    val sendTransactionService: ISendTransactionService,
    private val timerService: TimerService,
    private val priceImpactService: PriceImpactService
) : ViewModelUiState<SwapConfirmUiState>() {
    private var sendTransactionSettings: SendTransactionSettings? = null
    private val currency = currencyManager.baseCurrency
    private val tokenIn = swapQuote.tokenIn
    private val tokenOut = swapQuote.tokenOut
    private val amountIn = swapQuote.amountIn
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

                if (sendTransactionState.sendable) {
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
            validQuote = sendTransactionState.sendable,
            priceImpact = priceImpactState.priceImpact,
            priceImpactLevel = priceImpactState.priceImpactLevel,
            quoteFields = quoteFields,
            transactionFields = sendTransactionState.fields,
        )
    }

    override fun onCleared() {
        timerService.stop()
    }

    fun refresh() {
        loading = true
        emitState()

        fetchFinalQuote()
    }

    private fun fetchFinalQuote() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val finalQuote = swapProvider.fetchFinalQuote(tokenIn, tokenOut, amountIn, swapSettings, sendTransactionSettings)

                amountOut = finalQuote.amountOut
                amountOutMin = finalQuote.amountOutMin
                quoteFields = finalQuote.fields
                emitState()

                fiatServiceOut.setAmount(amountOut)
                fiatServiceOutMin.setAmount(amountOutMin)
                sendTransactionService.setSendTransactionData(finalQuote.sendTransactionData)

                priceImpactService.setPriceImpact(finalQuote.priceImpact, swapProvider.title)
            } catch (t: Throwable) {
//                Log.e("AAA", "fetchFinalQuote error", t)
            }
        }
    }

    suspend fun swap() = withContext(Dispatchers.Default) {
        sendTransactionService.sendTransaction()
    }

    companion object {
        fun init(quote: SwapProviderQuote, settings: Map<String, Any?>): CreationExtras.() -> SwapConfirmViewModel = {
            val sendTransactionService = SendTransactionServiceFactory.create(quote.tokenIn.blockchainType)

            SwapConfirmViewModel(
                quote.provider,
                quote.swapQuote,
                settings,
                App.currencyManager,
                FiatService(App.marketKit),
                FiatService(App.marketKit),
                FiatService(App.marketKit),
                sendTransactionService,
                TimerService(),
                PriceImpactService()
            )
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
    val priceImpactLevel: SwapMainModule.PriceImpactLevel?,
    val quoteFields: List<DataField>,
    val transactionFields: List<DataField>,
)