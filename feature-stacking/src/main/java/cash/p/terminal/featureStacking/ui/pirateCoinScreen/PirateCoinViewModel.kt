package cash.p.terminal.featureStacking.ui.pirateCoinScreen

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.featureStacking.R
import cash.p.terminal.featureStacking.ui.entities.PayoutViewItem
import cash.p.terminal.featureStacking.ui.staking.StackingType
import cash.p.terminal.network.domain.repository.PiratePlaceRepository
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.balance.BalanceService
import cash.p.terminal.wallet.balance.BalanceViewHelper
import cash.p.terminal.wallet.models.CoinPrice
import io.horizontalsystems.core.helpers.DateHelper
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.Calendar
import java.util.Date

internal class PirateCoinViewModel(
    private val walletManager: IWalletManager,
    private val adapterManager: IAdapterManager,
    private val piratePlaceRepository: PiratePlaceRepository,
    private val balanceService: BalanceService
) : ViewModel() {

    private val _uiState = mutableStateOf(PirateCoinUIState())
    val uiState: State<PirateCoinUIState> get() = _uiState

    companion object {
        internal const val MIN_STACKING_AMOUNT = 100
    }

    fun loadBalance() {
        balanceService.start()
        val wallet = walletManager.activeWallets.find { it.coin.code == StackingType.PCASH.value }
        val receiveAddress: String = wallet?.let {
            adapterManager.getReceiveAdapterForWallet(wallet)?.receiveAddress ?: ""
        } ?: ""
        val balance: BigDecimal = wallet?.let {
            adapterManager.getBalanceAdapterForWallet(wallet)?.balanceData?.total
        } ?: BigDecimal.ZERO
        _uiState.value = uiState.value.copy(
            balance = balance,
            token = wallet?.token,
            receiveAddress = receiveAddress
        )

        viewModelScope.launch {
            balanceService.balanceItemsFlow.collectLatest { items ->
                items?.find { it.wallet.coin.code == StackingType.PCASH.value }?.let { item ->
                    loadInvestmentData(
                        balance = item.balanceData.total,
                        wallet = wallet,
                        coinPrice = item.coinPrice
                    )
                    uiState.value.receiveAddress?.let { loadPayouts(it) }
                }
            }
        }
    }

    private fun loadInvestmentData(balance: BigDecimal, wallet: Wallet?, coinPrice: CoinPrice?) =
        viewModelScope.launch(
            CoroutineExceptionHandler { _, throwable ->
                Log.e("PirateCoinViewModel", "Error loading investment data", throwable)
                _uiState.value = uiState.value.copy(
                    unpaid = BigDecimal.ZERO
                )
            }) {
            var unpaid: BigDecimal = BigDecimal.ZERO
            var totalIncome: BigDecimal = BigDecimal.ZERO
            var secondaryAmount = ""
            var address = ""
            if (wallet != null) {
                adapterManager.getReceiveAdapterForWallet(wallet)?.receiveAddress?.let { receiveAddress ->
                    val investmentData = piratePlaceRepository.getInvestmentData(receiveAddress)
                    unpaid = investmentData.unrealizedValue.toBigDecimal()
                    totalIncome = investmentData.mint.toBigDecimal()
                    address = receiveAddress
                }
                secondaryAmount = BalanceViewHelper.currencyValue(
                    balance = balance,
                    coinPrice = coinPrice,
                    visible = true,
                    fullFormat = false,
                    currency = balanceService.baseCurrency,
                    dimmed = false
                ).value
            }
            val totalIncomeSecondary = BalanceViewHelper.currencyValue(
                balance = totalIncome,
                coinPrice = coinPrice,
                visible = true,
                fullFormat = false,
                currency = balanceService.baseCurrency,
                dimmed = false
            ).value
            val unpaidSecondary = BalanceViewHelper.currencyValue(
                balance = unpaid,
                coinPrice = coinPrice,
                visible = true,
                fullFormat = false,
                currency = balanceService.baseCurrency,
                dimmed = false
            ).value
            _uiState.value = uiState.value.copy(
                unpaid = unpaid,
                secondaryAmount = secondaryAmount,
                totalIncome = totalIncome,
                totalIncomeSecondary = totalIncomeSecondary,
                unpaidSecondary = unpaidSecondary
            )
        }

    private suspend fun loadPayouts(address: String) {
        val payouts = piratePlaceRepository.getStakeData(address).stakes.map {
            val date = Date(it.createdAt)
            PayoutViewItem(
                id = it.id,
                date = formatDate(date).uppercase(),
                time = DateHelper.getOnlyTimeWithSec(date),
                payoutType = it.type,
                amount = it.amount.toBigDecimal(),
                amountSecondary = BalanceViewHelper.currencyValue(
                    balance = it.amount.toBigDecimal(),
                    coinPrice = null,
                    visible = true,
                    fullFormat = false,
                    currency = balanceService.baseCurrency,
                    dimmed = false
                ).value
            )
        }.groupBy { it.date }
        _uiState.value = uiState.value.copy(
            payoutItems = payouts
        )
    }

    private fun formatDate(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date

        val today = Calendar.getInstance()
        if (calendar[Calendar.YEAR] == today[Calendar.YEAR] && calendar[Calendar.DAY_OF_YEAR] == today[Calendar.DAY_OF_YEAR]) {
            return Translator.getString(R.string.Timestamp_Today)
        }

        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_MONTH, -1)
        if (calendar[Calendar.YEAR] == yesterday[Calendar.YEAR] && calendar[Calendar.DAY_OF_YEAR] == yesterday[Calendar.DAY_OF_YEAR]) {
            return Translator.getString(R.string.Timestamp_Yesterday)
        }

        return DateHelper.shortDate(date, "MMMM d", "MMMM d, yyyy")
    }

    override fun onCleared() {
        balanceService.clear()
    }
}