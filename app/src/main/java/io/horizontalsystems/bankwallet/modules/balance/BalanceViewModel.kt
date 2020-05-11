package io.horizontalsystems.bankwallet.modules.balance

import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.entities.Wallet

class BalanceViewModel : ViewModel(), BalanceModule.IView, BalanceModule.IRouter {

    lateinit var delegate: BalanceModule.IViewDelegate

    val openReceiveDialog = SingleLiveEvent<Wallet>()
    val openSendDialog = SingleLiveEvent<Wallet>()
    val openManageCoinsLiveEvent = SingleLiveEvent<Void>()
    val openSortingTypeDialogLiveEvent = SingleLiveEvent<BalanceSortType>()
    val openBackup = SingleLiveEvent<Pair<Account, Int>>()
    val openChartModule = SingleLiveEvent<Coin>()

    val isSortOn = SingleLiveEvent<Boolean>()
    val setHeaderViewItem = SingleLiveEvent<BalanceHeaderViewItem>()
    val setViewItems = SingleLiveEvent<List<BalanceViewItem>>()
    val showBackupAlert = SingleLiveEvent<Pair<Coin, PredefinedAccountType>>()
    val didRefreshLiveEvent = SingleLiveEvent<Void>()
    val setBalanceHiddenLiveEvent = SingleLiveEvent<Pair<Boolean, Boolean>>()
    val showSyncError = SingleLiveEvent<Coin>()

    fun init() {
        BalanceModule.init(this, this)

        delegate.onLoad()
    }

    // IRouter

    override fun openReceive(wallet: Wallet) {
        openReceiveDialog.postValue(wallet)
    }

    override fun openSend(wallet: Wallet) {
        openSendDialog.postValue(wallet)
    }

    override fun openManageCoins() {
        openManageCoinsLiveEvent.call()
    }

    override fun openSortTypeDialog(sortingType: BalanceSortType) {
        openSortingTypeDialogLiveEvent.postValue(sortingType)
    }

    override fun openBackup(account: Account, coinCodesStringRes: Int) {
        openBackup.postValue(Pair(account, coinCodesStringRes))
    }

    override fun openChart(coin: Coin) {
        openChartModule.postValue(coin)
    }

    // IView

    override fun set(sortIsOn: Boolean) {
        isSortOn.postValue(sortIsOn)
    }

    override fun set(headerViewItem: BalanceHeaderViewItem) {
        setHeaderViewItem.postValue(headerViewItem)
    }

    override fun set(viewItems: List<BalanceViewItem>) {
        setViewItems.postValue(viewItems)
    }

    override fun showBackupRequired(coin: Coin, predefinedAccountType: PredefinedAccountType) {
        showBackupAlert.postValue(Pair(coin, predefinedAccountType))
    }

    override fun didRefresh() {
        didRefreshLiveEvent.postValue(null)
    }

    override fun setBalanceHidden(hidden: Boolean, animate: Boolean) {
        setBalanceHiddenLiveEvent.postValue(Pair(hidden, animate))
    }

    override fun showSyncErrorDialog(coin: Coin) {
        showSyncError.postValue(coin)
    }

    // ViewModel

    override fun onCleared() {
        delegate.onClear()
    }

}
