package io.horizontalsystems.bankwallet.modules.balance

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Wallet

class BalanceViewModel : ViewModel(), BalanceModule.IView, BalanceModule.IRouter {

    lateinit var delegate: BalanceModule.IViewDelegate

    val openSendDialog = SingleLiveEvent<Wallet>()
    val openReceiveDialog = SingleLiveEvent<Wallet>()
    val balanceColorLiveDate = MutableLiveData<Int>()
    val didRefreshLiveEvent = SingleLiveEvent<Void>()
    val openManageCoinsLiveEvent = SingleLiveEvent<Void>()
    val openSortingTypeDialogLiveEvent = SingleLiveEvent<BalanceSortType>()

    val reloadLiveEvent = SingleLiveEvent<Void>()
    val reloadHeaderLiveEvent = SingleLiveEvent<Void>()
    val reloadItemLiveEvent = SingleLiveEvent<Int>()
    val enabledCoinsCountLiveEvent = SingleLiveEvent<Int>()
    val setSortingOnLiveEvent = SingleLiveEvent<Boolean>()

    fun init() {
        BalanceModule.init(this, this)

        delegate.viewDidLoad()
    }

    override fun enabledCoinsCount(size: Int) {
        enabledCoinsCountLiveEvent.value = size
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
}
