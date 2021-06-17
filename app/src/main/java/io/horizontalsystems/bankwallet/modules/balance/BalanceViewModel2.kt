package io.horizontalsystems.bankwallet.modules.balance

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.coinkit.models.CoinType
import io.reactivex.disposables.CompositeDisposable

class BalanceViewModel2(
    private val service: BalanceService,
    private val rateAppService: RateAppService,
    private val activeAccountService: ActiveAccountService,
    private val balanceViewItemFactory: BalanceViewItemFactory
) : ViewModel() {

    val titleLiveData = MutableLiveData<String>()
    val headerViewItemLiveData = MutableLiveData<BalanceHeaderViewItem>()
    val balanceViewItemsLiveData = MutableLiveData<List<BalanceViewItem>>()

    private var disposables = CompositeDisposable()

    var sortType: BalanceSortType
        get() = service.sortType
        set(value) {
            service.sortType = value
        }

    private var expandedWallet: Wallet? = null

    init {
        activeAccountService.activeAccountObservable
            .subscribeIO {
                titleLiveData.postValue(it.name)
            }
            .let {
                disposables.add(it)
            }

        service.balanceItemsObservable
            .subscribeIO {
                refreshViewItems()
                refreshHeaderViewItem()
            }
            .let {
                disposables.add(it)
            }
    }

    private fun refreshViewItems() {
        balanceViewItemsLiveData.postValue(service.balanceItemsSorted.map { balanceItem ->
            balanceViewItemFactory.viewItem(
                balanceItem,
                service.baseCurrency,
                balanceItem.wallet == expandedWallet,
                service.balanceHidden
            )
        })
    }

    private fun refreshHeaderViewItem() {
        headerViewItemLiveData.postValue(
            balanceViewItemFactory.headerViewItem(
                service.balanceItemsSorted,
                service.baseCurrency,
                service.balanceHidden
            )
        )
    }

    fun onRefresh() {
        service.refresh()
    }

    fun onBalanceClick() {
        service.balanceHidden = !service.balanceHidden
    }

    fun onItem(viewItem: BalanceViewItem) {
        expandedWallet = when {
            viewItem.wallet == expandedWallet -> null
            else -> viewItem.wallet
        }

        refreshViewItems()
    }

    fun onResume() {
        rateAppService.onBalancePageActive()
    }

    fun onPause() {
        rateAppService.onBalancePageInactive()
    }

    fun getSyncErrorDetails(viewItem: BalanceViewItem): SyncError = when {
        service.networkAvailable -> {
            SyncError.Dialog(viewItem.wallet, viewItem.errorMessage ?: "", sourceChangeable(viewItem.wallet.coin.type))
        }
        else -> {
            SyncError.NetworkNotAvailable()
        }
    }

    private fun sourceChangeable(coinType: CoinType) = when (coinType) {
        is CoinType.Bep2,
        is CoinType.Ethereum,
        is CoinType.Erc20 -> false
        else -> true
    }

    fun refreshByWallet(wallet: Wallet) {
        TODO("Not yet implemented")
    }

    fun onReportClick(errorMessage: Any) {
        TODO("Not yet implemented")
    }

    override fun onCleared() {
        activeAccountService.clear()
        service.clear()
        rateAppService.clear()
    }

    sealed class SyncError {
        class NetworkNotAvailable : SyncError()
        class Dialog(val wallet: Wallet, val errorMessage: String, val sourceChangeable: Boolean) : SyncError()
    }

}
