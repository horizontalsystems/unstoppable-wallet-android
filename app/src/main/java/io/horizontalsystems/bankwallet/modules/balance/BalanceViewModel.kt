package io.horizontalsystems.bankwallet.modules.balance

import androidx.lifecycle.MutableLiveData
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
    val openSwap = SingleLiveEvent<Wallet>()
    val openManageCoinsLiveEvent = SingleLiveEvent<Void>()
    val openSortingTypeDialogLiveEvent = SingleLiveEvent<BalanceSortType>()
    val openBackup = SingleLiveEvent<Pair<Account, Int>>()
    val openChartModule = SingleLiveEvent<Coin>()
    val openContactPage = SingleLiveEvent<Void>()

    val isSortOn = MutableLiveData<Boolean>()
    val setHeaderViewItem = MutableLiveData<BalanceHeaderViewItem>()
    val setViewItems = MutableLiveData<List<BalanceViewItem>>()
    val showBackupAlert = SingleLiveEvent<Pair<Coin, PredefinedAccountType>>()
    val didRefreshLiveEvent = SingleLiveEvent<Void>()
    val setBalanceHidden = MutableLiveData<Pair<Boolean, Boolean>>()
    val showSyncError = SingleLiveEvent<Triple<Wallet, String, Boolean>>()
    val networkNotAvailable = SingleLiveEvent<Void>()
    val showErrorMessageCopied = SingleLiveEvent<Void>()


    // IRouter

    override fun openReceive(wallet: Wallet) {
        openReceiveDialog.postValue(wallet)
    }

    override fun openSend(wallet: Wallet) {
        openSendDialog.postValue(wallet)
    }

    override fun openSwap(wallet: Wallet) {
        openSwap.postValue(wallet)
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

    override fun openContactPage() {
        openContactPage.call()
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
        setBalanceHidden.postValue(Pair(hidden, animate))
    }

    override fun showSyncErrorDialog(wallet: Wallet, errorMessage: String, sourceChangeable: Boolean) {
        showSyncError.postValue(Triple(wallet, errorMessage, sourceChangeable))
    }

    override fun showNetworkNotAvailable() {
        networkNotAvailable.call()
    }

    override fun showErrorMessageCopied() {
        showErrorMessageCopied.call()
    }

    // ViewModel

    override fun onCleared() {
        delegate.onClear()
    }

}
