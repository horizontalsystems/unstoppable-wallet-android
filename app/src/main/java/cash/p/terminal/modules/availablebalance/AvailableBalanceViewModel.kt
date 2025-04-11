package cash.p.terminal.modules.availablebalance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.modules.amount.AmountInputType
import cash.p.terminal.wallet.managers.IBalanceHiddenManager
import io.horizontalsystems.core.entities.CurrencyValue
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.java.KoinJavaComponent.inject
import java.math.BigDecimal

class AvailableBalanceViewModel(
    private val coinCode: String,
    private val coinDecimal: Int,
    private val fiatDecimal: Int,
) : ViewModel() {

    private val balanceHiddenManager: IBalanceHiddenManager by inject(IBalanceHiddenManager::class.java)

    var amountInputType: AmountInputType? = null
    var availableBalance: BigDecimal? = null
    var xRate: CurrencyValue? = null

    var formatted by mutableStateOf<String?>(null)
        private set

    val balanceHidden: StateFlow<Boolean> = balanceHiddenManager.balanceHiddenFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            balanceHiddenManager.balanceHidden
        )

    fun refreshFormatted() {
        val tmpAvailableBalance = availableBalance
        val tmpAmountInputMode = amountInputType

        formatted = when {
            tmpAvailableBalance == null || tmpAmountInputMode == null -> null
            tmpAmountInputMode == AmountInputType.COIN -> {
                App.numberFormatter.formatCoinFull(tmpAvailableBalance, coinCode, coinDecimal)
            }

            tmpAmountInputMode == AmountInputType.CURRENCY -> {
                xRate
                    ?.let {
                        it.copy(value = tmpAvailableBalance.times(it.value))
                    }
                    ?.getFormattedFull()
            }

            else -> null
        }
    }

    fun toggleHideBalance() {
        HudHelper.vibrate(App.instance)
        balanceHiddenManager.toggleBalanceHidden()
    }
}

object AvailableBalanceModule {

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val coinCode: String,
        private val coinDecimal: Int,
        private val fiatDecimal: Int,
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AvailableBalanceViewModel(
                coinCode,
                coinDecimal,
                fiatDecimal
            ) as T
        }
    }
}
