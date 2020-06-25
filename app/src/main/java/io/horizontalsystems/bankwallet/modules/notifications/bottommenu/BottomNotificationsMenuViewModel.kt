package io.horizontalsystems.bankwallet.modules.notifications.bottommenu

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IPriceAlertManager
import io.horizontalsystems.bankwallet.entities.PriceAlert

class BottomNotificationsMenuViewModel(
        private val coinCode: String,
        private val priceAlertManager: IPriceAlertManager,
        private val mode: NotificationMenuMode) : ViewModel() {

    val menuItemsLiveData = MutableLiveData<List<NotifMenuViewItem>>()
    private var changeState:PriceAlert.ChangeState = PriceAlert.ChangeState.OFF
    private var trendState:PriceAlert.TrendState = PriceAlert.TrendState.OFF

    init {
        val priceAlert = priceAlertManager.priceAlert(coinCode)
        priceAlert.changeState?.let {
            changeState = it
        }
        priceAlert.trendState?.let {
            trendState = it
        }
        setItems(mode)
    }

    private fun setItems(mode: NotificationMenuMode) {
        val items = when (mode) {
            NotificationMenuMode.All -> getFullList()
            NotificationMenuMode.Change -> getChangeList()
            NotificationMenuMode.Trend -> getTrendList()
        }

        menuItemsLiveData.postValue(items)
    }

    private fun getFullList(): List<NotifMenuViewItem> {
        val items = mutableListOf<NotifMenuViewItem>()
        items.add(NotifMenuViewItem(R.string.NotificationBottomMenu_Change24h, NotifViewItemType.SmallHeader))
        items.addAll(getChangeList())
        items.add(NotifMenuViewItem(R.string.NotificationBottomMenu_PriceTrendChange, NotifViewItemType.BigHeader))
        items.addAll(getTrendList())

        return items
    }

    private fun getChangeList(): List<NotifMenuViewItem> {
        return listOf(
                NotifMenuViewItem(R.string.NotificationBottomMenu_Off, NotifViewItemType.Option, OptionValue.ChangeOff, changeState == PriceAlert.ChangeState.OFF),
                NotifMenuViewItem(R.string.NotificationBottomMenu_2, NotifViewItemType.Option, OptionValue.Change2, changeState == PriceAlert.ChangeState.PERCENT_2),
                NotifMenuViewItem(R.string.NotificationBottomMenu_5, NotifViewItemType.Option, OptionValue.Change5, changeState == PriceAlert.ChangeState.PERCENT_5),
                NotifMenuViewItem(R.string.NotificationBottomMenu_10, NotifViewItemType.Option, OptionValue.Change10, changeState == PriceAlert.ChangeState.PERCENT_10)
        )
    }

    private fun getTrendList(): List<NotifMenuViewItem> {
        return listOf(
                NotifMenuViewItem(R.string.NotificationBottomMenu_Off, NotifViewItemType.Option, OptionValue.TrendOff, trendState == PriceAlert.TrendState.OFF),
                NotifMenuViewItem(R.string.NotificationBottomMenu_ShortTerm, NotifViewItemType.Option, OptionValue.TrendShort, trendState == PriceAlert.TrendState.SHORT),
                NotifMenuViewItem(R.string.NotificationBottomMenu_LongTerm, NotifViewItemType.Option, OptionValue.TrendLong, trendState == PriceAlert.TrendState.LONG)
        )
    }

    fun onOptionClick(item: NotifMenuViewItem) {
        if (item.enabled){
            return
        }

        if (item.type == NotifViewItemType.Option) {
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

        setItems(mode)
        priceAlertManager.savePriceAlert(PriceAlert(coinCode, changeState, trendState))
    }

    private fun getChangeState(optionValue: OptionValue?): PriceAlert.ChangeState{
        return when(optionValue){
            OptionValue.Change2 -> PriceAlert.ChangeState.PERCENT_2
            OptionValue.Change5 -> PriceAlert.ChangeState.PERCENT_5
            OptionValue.Change10 -> PriceAlert.ChangeState.PERCENT_10
            else -> PriceAlert.ChangeState.OFF
        }
    }

    private fun getTrendState(optionValue: OptionValue?): PriceAlert.TrendState{
        return when(optionValue){
            OptionValue.TrendShort -> PriceAlert.TrendState.SHORT
            OptionValue.TrendLong -> PriceAlert.TrendState.LONG
            else -> PriceAlert.TrendState.OFF
        }
    }

}

data class NotifMenuViewItem(@StringRes val title: Int, val type: NotifViewItemType, val optionValue: OptionValue? = null, val enabled: Boolean = false)
enum class NotifViewItemType{
    SmallHeader,
    BigHeader,
    Option
}
enum class OptionValue{
    Change2, Change5, Change10, ChangeOff, TrendOff, TrendShort, TrendLong
}
