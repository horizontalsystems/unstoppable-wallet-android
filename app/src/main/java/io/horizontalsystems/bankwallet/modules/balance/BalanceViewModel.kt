package io.horizontalsystems.bankwallet.modules.balance

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.SingleLiveEvent

class BalanceViewModel : ViewModel(), BalanceModule.IView, BalanceModule.IRouter {

    lateinit var delegate: BalanceModule.IViewDelegate

    val openReceiveDialog = SingleLiveEvent<Wallet>()
    val openSendDialog = SingleLiveEvent<Wallet>()
    val openSendEvmDialog = SingleLiveEvent<Wallet>()
    val openSwap = SingleLiveEvent<Wallet>()
    val openManageCoinsLiveEvent = SingleLiveEvent<Void>()
    val openSortingTypeDialogLiveEvent = SingleLiveEvent<BalanceSortType>()
    val openChartModule = SingleLiveEvent<Coin>()
    val openEmail = SingleLiveEvent<Pair<String, String>>()

    val isSortOn = MutableLiveData<Boolean>()
    val setHeaderViewItem = MutableLiveData<BalanceHeaderViewItem>()
    val setViewItems = MutableLiveData<List<BalanceViewItem>>()
    val titleLiveData = MutableLiveData<String>()
    val showBackupAlert = SingleLiveEvent<Wallet>()
    val didRefreshLiveEvent = SingleLiveEvent<Void>()
    val setBalanceHidden = MutableLiveData<Pair<Boolean, Boolean>>()
    val showSyncError = SingleLiveEvent<Triple<Wallet, String, Boolean>>()
    val networkNotAvailable = SingleLiveEvent<Void>()


    // IRouter

    override fun openReceive(wallet: Wallet) {
        openReceiveDialog.postValue(wallet)
    }

    override fun openSend(wallet: Wallet) {
        when (wallet.coin.type) {
            CoinType.Ethereum, is CoinType.Erc20,
            CoinType.BinanceSmartChain, is CoinType.Bep20 -> {
                openSendEvmDialog.postValue(wallet)
            }
            else -> {
                openSendDialog.postValue(wallet)
            }
        }

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

    override fun openChart(coin: Coin) {
        openChartModule.postValue(coin)
    }

    override fun openEmail(emailAddress: String, errorMessage: String) {
        openEmail.postValue(Pair(emailAddress, errorMessage))
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

    override fun setTitle(v: String?) {
        titleLiveData.postValue(v)
    }

    override fun showBackupRequired(wallet: Wallet) {
        showBackupAlert.postValue(wallet)
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

    // ViewModel

    override fun onCleared() {
        delegate.onClear()
    }

}
