package cash.p.terminal.featureStacking.ui.calculatorScreen

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.featureStacking.ui.staking.StackingType
import cash.p.terminal.network.domain.repository.PiratePlaceRepository
import cash.p.terminal.wallet.balance.BalanceService
import cash.p.terminal.wallet.balance.BalanceViewHelper
import cash.p.terminal.wallet.models.CoinPrice
import io.horizontalsystems.core.IAppNumberFormatter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class CalculatorViewModel(
    private val balanceService: BalanceService,
    private val piratePlaceRepository: PiratePlaceRepository,
    private val numberFormatter: IAppNumberFormatter
) : ViewModel() {

    private val _uiState = mutableStateOf(CalculatorUIState())
    val uiState: State<CalculatorUIState> get() = _uiState

    private var calculatorJob: Job? = null

    companion object {
        private const val DELAY = 300L
    }

    fun setCalculatorValue(value: String) {
        balanceService.start()
        _uiState.value = _uiState.value.copy(amount = value)
        val doubleValue = _uiState.value.amount.toDoubleOrNull()
        if (doubleValue != null) {
            calculatorJob?.cancel()
            calculatorJob = viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                Log.d("CalculatorViewModel", "getCalculatorData error: ${throwable.message}")
            }) {
                delay(DELAY)
                loadCalculatedData(doubleValue)
            }
        }
    }

    private suspend fun loadCalculatedData(doubleValue: Double) {
        balanceService.balanceItemsFlow.collectLatest { items ->
            items?.find { it.wallet.coin.code == StackingType.PCASH.value }
                ?.let { pcashItem ->
                    val items = piratePlaceRepository.getCalculatorData(
                        coin = uiState.value.coin,
                        amount = doubleValue
                    ).items.map {
                        val amountBigDecimal = it.amount.toBigDecimal()
                        CalculatorItem(
                            period = it.periodType,
                            amount = "+" + numberFormatter.formatNumberShort(
                                amountBigDecimal,
                                5
                            ),
                            amountSecondary = "+" + BalanceViewHelper.currencyValue(
                                balance = amountBigDecimal,
                                coinPrice = pcashItem.coinPrice,
                                visible = true,
                                fullFormat = false,
                                currency = balanceService.baseCurrency,
                                dimmed = false
                            ).value
                        )
                    }
                    _uiState.value = uiState.value.copy(
                        calculateResult = items,
                        coinSecondary = pcashItem.coinPrice?.currencyCode.orEmpty(),
                        coinExchange = buildExchangeRateString(
                            amount = doubleValue,
                            coinPrice = pcashItem.coinPrice,
                            coin = uiState.value.coin
                        )
                    )
                }
        }
    }

    private fun buildExchangeRateString(
        amount: Double,
        coinPrice: CoinPrice?,
        coin: String
    ): String {
        val amountBigDecimal = amount.toBigDecimal()
        val amountSecondary = BalanceViewHelper.currencyValue(
            balance = amountBigDecimal,
            coinPrice = coinPrice,
            visible = true,
            fullFormat = false,
            currency = balanceService.baseCurrency,
            dimmed = false
        ).value
        return "${amountBigDecimal.toPlainString()} ${coin.uppercase()} = $amountSecondary"
    }

    override fun onCleared() {
        balanceService.clear()
    }
}