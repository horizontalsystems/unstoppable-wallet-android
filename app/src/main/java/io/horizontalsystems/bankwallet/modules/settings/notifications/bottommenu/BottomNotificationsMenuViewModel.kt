package io.horizontalsystems.bankwallet.modules.settings.notifications.bottommenu

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IPriceAlertManager
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.horizontalsystems.coinkit.models.CoinType

class BottomNotificationsMenuViewModel(
        private val coinType: CoinType,
        private val coinName: String,
        private val priceAlertManager: IPriceAlertManager,
        private val mode: NotificationMenuMode) : ViewModel() {

    val menuItemsLiveData = MutableLiveData<List<NotificationMenuViewItem>>()
    private val alertStates = priceAlertManager.getAlertStates(coinType)
    private var changeState: PriceAlert.ChangeState = alertStates.first
    private var trendState: PriceAlert.TrendState = alertStates.second

    init {
        setItems()
    }

    fun onOptionClick(item: NotificationMenuViewItem) {
        if (item.enabled) {
            return
        }

        if (item.type == NotificationViewItemType.Option) {
            when (item.optionValue) {
                OptionValue.ChangeOff,
                OptionValue.Change2,
                OptionValue.Change5,
                OptionValue.Change10 -> {
                    changeState = getChangeState(item.optionValue)
                }
                OptionValue.TrendOff,
                OptionValue.TrendShort,
                OptionValue.TrendLong -> {
                    trendState = getTrendState(item.optionValue)
                }
            }
        }

        setItems()
        priceAlertManager.savePriceAlert(coinType, coinName, changeState, trendState)
    }

    private fun setItems() {
        val items = when (mode) {
            NotificationMenuMode.All -> getFullList()
            NotificationMenuMode.Change -> getChangeList()
            NotificationMenuMode.Trend -> getTrendList()
        }

        menuItemsLiveData.postValue(items)
    }

    private fun getFullList(): List<NotificationMenuViewItem> {
        val items = mutableListOf<NotificationMenuViewItem>()
        items.add(NotificationMenuViewItem(R.string.NotificationBottomMenu_Change24h, NotificationViewItemType.SmallHeader))
        items.addAll(getChangeList())
        items.add(NotificationMenuViewItem(R.string.NotificationBottomMenu_PriceTrendChange, NotificationViewItemType.BigHeader))
        items.addAll(getTrendList())

        return items
    }

    private fun getChangeList(): List<NotificationMenuViewItem> {
        return listOf(
                NotificationMenuViewItem(R.string.SettingsNotifications_Off, NotificationViewItemType.Option, OptionValue.ChangeOff, changeState == PriceAlert.ChangeState.OFF),
                NotificationMenuViewItem(R.string.NotificationBottomMenu_2, NotificationViewItemType.Option, OptionValue.Change2, changeState == PriceAlert.ChangeState.PERCENT_2),
                NotificationMenuViewItem(R.string.NotificationBottomMenu_5, NotificationViewItemType.Option, OptionValue.Change5, changeState == PriceAlert.ChangeState.PERCENT_5),
                NotificationMenuViewItem(R.string.NotificationBottomMenu_10, NotificationViewItemType.Option, OptionValue.Change10, changeState == PriceAlert.ChangeState.PERCENT_10)
        )
    }

    private fun getTrendList(): List<NotificationMenuViewItem> {
        return listOf(
                NotificationMenuViewItem(R.string.SettingsNotifications_Off, NotificationViewItemType.Option, OptionValue.TrendOff, trendState == PriceAlert.TrendState.OFF),
                NotificationMenuViewItem(R.string.NotificationBottomMenu_ShortTerm, NotificationViewItemType.Option, OptionValue.TrendShort, trendState == PriceAlert.TrendState.SHORT),
                NotificationMenuViewItem(R.string.NotificationBottomMenu_LongTerm, NotificationViewItemType.Option, OptionValue.TrendLong, trendState == PriceAlert.TrendState.LONG)
        )
    }

    private fun getChangeState(optionValue: OptionValue?): PriceAlert.ChangeState {
        return when (optionValue) {
            OptionValue.Change2 -> PriceAlert.ChangeState.PERCENT_2
            OptionValue.Change5 -> PriceAlert.ChangeState.PERCENT_5
            OptionValue.Change10 -> PriceAlert.ChangeState.PERCENT_10
            else -> PriceAlert.ChangeState.OFF
        }
    }

    private fun getTrendState(optionValue: OptionValue?): PriceAlert.TrendState {
        return when (optionValue) {
            OptionValue.TrendShort -> PriceAlert.TrendState.SHORT
            OptionValue.TrendLong -> PriceAlert.TrendState.LONG
            else -> PriceAlert.TrendState.OFF
        }
    }

}

data class NotificationMenuViewItem(
        @StringRes val title: Int,
        val type: NotificationViewItemType,
        val optionValue: OptionValue? = null,
        val enabled: Boolean = false)

enum class NotificationViewItemType {
    SmallHeader,
    BigHeader,
    Option
}

enum class OptionValue {
    Change2, Change5, Change10, ChangeOff, TrendOff, TrendShort, TrendLong
}
