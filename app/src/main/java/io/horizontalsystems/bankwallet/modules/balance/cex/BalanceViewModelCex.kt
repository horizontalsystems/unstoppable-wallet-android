package io.horizontalsystems.bankwallet.modules.balance.cex

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.modules.balance.BalanceSortType
import io.horizontalsystems.bankwallet.modules.balance.DeemedValue
import io.horizontalsystems.bankwallet.modules.balance.ITotalBalance
import io.horizontalsystems.bankwallet.modules.balance.TotalBalance
import kotlinx.coroutines.launch
import java.math.BigDecimal

class BalanceViewModelCex(
    private val totalBalance: TotalBalance,
    private val localStorage: ILocalStorage,
) : ViewModel(), ITotalBalance by totalBalance {

    val sortTypes =
        listOf(BalanceSortType.Value, BalanceSortType.Name, BalanceSortType.PercentGrowth)
    var sortType by mutableStateOf(localStorage.sortType)
        private set

    private var expandedItemId: String? = null
    private var isRefreshing = false
    private var items: List<BalanceCexViewItem> = listOf(
        BalanceCexViewItem(
            coinIconUrl = "",
            coinIconPlaceholder = R.drawable.coin_placeholder,
            coinCode = "BTC",
            badge = null,
            primaryValue = DeemedValue("primaryValue"),
            exchangeValue = DeemedValue("exchangeValue"),
            diff = BigDecimal.ONE,
            secondaryValue = DeemedValue("secondaryValue"),
            expanded = false,
            hasCoinInfo = true,
            coinUid = ""
        )
    )

    var uiState by mutableStateOf(
        UiState(
            isRefreshing = isRefreshing,
            items = items
        )
    )
        private set

    init {
        totalBalance.start(viewModelScope)
    }

    private fun sortAndEmitItems() {
        viewModelScope.launch {
            uiState = UiState(
                isRefreshing = isRefreshing,
                items = items
            )
        }
    }

    override fun onCleared() {
        totalBalance.stop()
    }

    fun onSelectSortType(sortType: BalanceSortType) {
        this.sortType = sortType
        localStorage.sortType = sortType

        sortAndEmitItems()
    }

    fun onRefresh() {

    }

    fun onClickItem(viewItem: BalanceCexViewItem) {
        expandedItemId = when {
            viewItem.id == expandedItemId -> null
            else -> viewItem.id
        }

        items = items.map {
            it.copy(expanded = it.id == expandedItemId)
        }

        sortAndEmitItems()
    }
}

data class UiState(val isRefreshing: Boolean, val items: List<BalanceCexViewItem>)

data class BalanceCexViewItem(
    val coinIconUrl: String,
    val coinIconPlaceholder: Int,
    val coinCode: String,
    val badge: String?,
    val primaryValue: DeemedValue<String>,
    val exchangeValue: DeemedValue<String>,
    val diff: BigDecimal?,
    val secondaryValue: DeemedValue<String>,
    val expanded: Boolean,
    val hasCoinInfo: Boolean,
    val coinUid: String,
) {
    val id: String = "uniqId"
}
