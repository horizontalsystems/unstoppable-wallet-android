package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.OneInchException
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.FeeType
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceFactory
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataField
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.uiv3.components.message.DefenseSystemMessage
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
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
    private val swapDefenseSystemService: SwapDefenseSystemService
) : ViewModelUiState<SwapConfirmUiState>() {
    private var sendTransactionSettings: SendTransactionSettings? = null
    private val currency = currencyManager.baseCurrency
    private val tokenIn = swapQuote.tokenIn
    private val tokenOut = swapQuote.tokenOut
    private val amountIn = swapQuote.amountIn
    private var fiatAmountIn: BigDecimal? = null
    private var recipient: Address? = null
    private var slippage: BigDecimal = BigDecimal.ONE

    private var fiatAmountOut: BigDecimal? = null
    private var fiatAmountOutMin: BigDecimal? = null

    private var error: Throwable? = null
    private var initialLoading = true
    private var loading = true
    private var timerState = timerService.stateFlow.value
    private var sendTransactionState = sendTransactionService.stateFlow.value
    private var priceImpactState = priceImpactService.stateFlow.value
    private var swapDefenseState = swapDefenseSystemService.stateFlow.value

    private var amountOut: BigDecimal? = null
    private var amountOutMin: BigDecimal? = null
    private var estimatedTime: Long? = null
    private var quoteFields: List<DataField> = listOf()
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

                fetchFinalQuote()
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

        sendTransactionService.start(viewModelScope)
        swapDefenseSystemService.start(viewModelScope)

        fetchFinalQuote()
    }

    private fun handleUpdatedPriceImpactState(priceImpactState: PriceImpactService.State) {
        this.priceImpactState = priceImpactState

        swapDefenseSystemService.setPriceImpact(priceImpactState.priceImpact, priceImpactState.priceImpactLevel)

        emitState()
    }

    override fun createState(): SwapConfirmUiState {
        var cautions = sendTransactionState.cautions

        error?.let {
            cautions += CautionViewItem(it.javaClass.simpleName, it.message ?: "", CautionViewItem.Type.Error)
        }

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
            networkFee = sendTransactionState.networkFee,
            extraFees = sendTransactionState.extraFees,
            cautions = cautions,
            validQuote = error == null && sendTransactionState.sendable,
            priceImpact = priceImpactState.priceImpact,
            priceImpactLevel = priceImpactState.priceImpactLevel,
            quoteFields = quoteFields,
            transactionFields = sendTransactionState.fields,
            hasSettings = sendTransactionService.hasSettings,
            hasNonceSettings = sendTransactionService.hasNonceSettings,
            swapDefenseSystemMessage = swapDefenseState.systemMessage,
            recipient = recipient,
            slippage = slippage,
            estimatedTime = estimatedTime,
        )
    }

    override fun onCleared() {
        timerService.stop()
    }

    fun refresh(silent: Boolean = false) {
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
                    slippage
                )

                ensureActive()

                amountOut = finalQuote.amountOut
                amountOutMin = finalQuote.amountOutMin
                estimatedTime = finalQuote.estimatedTime
                quoteFields = finalQuote.fields
                emitState()

                fiatServiceOut.setAmount(amountOut)
                fiatServiceOutMin.setAmount(amountOutMin)
                sendTransactionService.setSendTransactionData(finalQuote.sendTransactionData)

                priceImpactService.setProviderTitle(swapProvider.title)
            } catch (e: CancellationException) {
                throw e
            } catch (e: OneInchException) {
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
        stat(page = StatPage.SwapConfirmation, event = StatEvent.Send)

        sendTransactionService.sendTransaction(swapDefenseState.mevProtectionEnabled)
    }

    fun setRecipient(recipient: Address?) {
        this.recipient = recipient

        refresh()
    }

    fun setSlippage(slippage: BigDecimal) {
        this.slippage = slippage

        refresh()
    }

    companion object {
        fun init(quote: SwapProviderQuote): CreationExtras.() -> SwapConfirmViewModel = {
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
                PriceImpactService(),
                SwapDefenseSystemService(sendTransactionService.supportsMevProtection)
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
    val priceImpact: BigDecimal?,
    val priceImpactLevel: PriceImpactLevel?,
    val quoteFields: List<DataField>,
    val transactionFields: List<DataField>,
    val extraFees: Map<FeeType, SendModule.AmountData>,
    val hasSettings: Boolean,
    val hasNonceSettings: Boolean,
    val swapDefenseSystemMessage: DefenseSystemMessage?,
    val recipient: Address?,
    val slippage: BigDecimal,
    val estimatedTime: Long?,
) {
    val totalFee by lazy {
        val networkFiatValue = networkFee?.secondary  ?: return@lazy null
        val networkFee = networkFiatValue.value
        val extraFeeValues = extraFees.mapNotNull { it.value.secondary?.value }
        if (extraFeeValues.isEmpty()) return@lazy null
        val totalValue = networkFee + extraFeeValues.sumOf { it }

        CurrencyValue(
            networkFiatValue.currencyValue.currency,
            totalValue
        )
    }
}
