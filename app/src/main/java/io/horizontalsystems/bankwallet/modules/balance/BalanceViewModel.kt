package io.horizontalsystems.bankwallet.modules.balance

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Wallet

class BalanceViewModel : ViewModel(), BalanceModule.IView, BalanceModule.IRouter {

    lateinit var delegate: BalanceModule.IViewDelegate

    val openSendDialog = SingleLiveEvent<Wallet>()
    val openReceiveDialog = SingleLiveEvent<Wallet>()
    val didRefreshLiveEvent = SingleLiveEvent<Void>()
    val openManageCoinsLiveEvent = SingleLiveEvent<Void>()
    val openSortingTypeDialogLiveEvent = SingleLiveEvent<BalanceSortType>()

    val reloadLiveEvent = SingleLiveEvent<Void>()
    val reloadHeaderLiveEvent = SingleLiveEvent<Void>()
    val reloadItemLiveEvent = SingleLiveEvent<Int>()
    val setSortingOnLiveEvent = SingleLiveEvent<Boolean>()
    val showBackupAlert = SingleLiveEvent<Unit>()
    val openBackup = SingleLiveEvent<Pair<Account, Int>>()
    val openChartModule = SingleLiveEvent<Coin>()
    val setChartButtonEnabled = SingleLiveEvent<Boolean>()

    fun init() {
        BalanceModule.init(this, this)

        delegate.viewDidLoad()
    }

    override fun reload() {
        reloadLiveEvent.postValue(null)
    }

    override fun updateItem(position: Int) {
        reloadItemLiveEvent.value = position
    }

    override fun updateHeader() {
        reloadHeaderLiveEvent.postValue(null)
    }

    override fun openReceiveDialog(wallet: Wallet) {
        openReceiveDialog.value = wallet
    }

    override fun openSendDialog(wallet: Wallet) {
        openSendDialog.value = wallet
    }

    fun onReceiveClicked(position: Int) {
        delegate.onReceive(position)
    }

    fun onSendClicked(position: Int) {
        delegate.onPay(position)
    }

    override fun didRefresh() {
        didRefreshLiveEvent.call()
    }

    override fun openManageCoins() {
        openManageCoinsLiveEvent.call()
    }

    override fun onCleared() {
        delegate.onClear()
    }

    override fun openSortTypeDialog(sortingType: BalanceSortType) {
        openSortingTypeDialogLiveEvent.postValue(sortingType)
    }

    override fun setSortingOn(isOn: Boolean) {
        setSortingOnLiveEvent.postValue(isOn)
    }

    override fun showBackupAlert() {
        showBackupAlert.call()
    }

    override fun openBackup(account: Account, coinCodesStringRes: Int) {
        openBackup.value = Pair(account, coinCodesStringRes)
    }

    override fun openChart(coin: Coin) {
        openChartModule.postValue(coin)
    }

    override fun setChartButtonState(enabled: Boolean) {
        setChartButtonEnabled.value = enabled
    }
}
