package io.horizontalsystems.bankwallet.modules.balance

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.ui.extensions.Direction

class BalanceViewModel : ViewModel(), BalanceModule.IView, BalanceModule.IRouter {

    lateinit var delegate: BalanceModule.IViewDelegate

    val openSendDialog = SingleLiveEvent<String>()
    val openReceiveDialog = SingleLiveEvent<String>()
    val balanceColorLiveDate = MutableLiveData<Int>()
    val didRefreshLiveEvent = SingleLiveEvent<Void>()
    val openManageCoinsLiveEvent = SingleLiveEvent<Void>()
    val openSortTypeDialogLiveEvent = SingleLiveEvent<Void>()

    val reloadLiveEvent = SingleLiveEvent<Void>()
    val reloadHeaderLiveEvent = SingleLiveEvent<Void>()
    val reloadItemLiveEvent = SingleLiveEvent<Int>()
    val enabledCoinsCountLiveEvent = SingleLiveEvent<Int>()
    val sortButtonLabelLiveDate = MutableLiveData<Int>()
    val sortButtonDirectionLiveDate = MutableLiveData<Direction>()
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

    override fun openReceiveDialog(coin: String) {
        openReceiveDialog.value = coin
    }

    override fun openSendDialog(coin: String) {
        openSendDialog.value = coin
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

    override fun openSortTypeDialog() {
        openSortTypeDialogLiveEvent.call()
    }

    override fun setSortButtonLabel(titleRes: Int) {
        sortButtonLabelLiveDate.value = titleRes
    }

    override fun setSortButtonDirection(direction: Direction) {
        sortButtonDirectionLiveDate.value = direction
    }

    override fun setSortingOn(isOn: Boolean) {
        setSortingOnLiveEvent.postValue(isOn)
    }
}
