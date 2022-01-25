package io.horizontalsystems.bankwallet.modules.balance2

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.BalanceHeaderViewItem
import io.horizontalsystems.bankwallet.modules.balance.BalanceService2
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItemFactory
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch

class BalanceViewModel(
    private val service: BalanceService2,
    private val balanceViewItemFactory: BalanceViewItemFactory
) : ViewModel() {

    var balanceViewItemsWrapper by mutableStateOf<Pair<BalanceHeaderViewItem, List<BalanceViewItem>>?>(null)
        private set

    var sortType by service::sortType

    private var expandedWallet: Wallet? = null
    private val disposables = CompositeDisposable()

    init {
        service.balanceItemsObservable
            .subscribeIO {
                refreshViewItems()
            }
            .let {
                disposables.add(it)
            }
    }

    private fun refreshViewItems() {
        val items = service.balanceItems.map { balanceItem ->
            balanceViewItemFactory.viewItem(
                balanceItem,
                service.baseCurrency,
                balanceItem.wallet == expandedWallet,
                service.balanceHidden
            )
        }

        val headerViewItem = balanceViewItemFactory.headerViewItem(
            service.balanceItems,
            service.baseCurrency,
            service.balanceHidden
        )

        viewModelScope.launch {
            balanceViewItemsWrapper = Pair(headerViewItem, items)
        }
    }


    override fun onCleared() {
        service.clear()
    }

    fun onBalanceClick() {
        service.balanceHidden = !service.balanceHidden

        refreshViewItems()
    }

    fun onItem(viewItem: BalanceViewItem) {
        expandedWallet = when {
            viewItem.wallet == expandedWallet -> null
            else -> viewItem.wallet
        }

        refreshViewItems()
    }

    fun getWalletForReceive(viewItem: BalanceViewItem) = when {
        viewItem.wallet.account.isBackedUp -> viewItem.wallet
        else -> throw BackupRequiredError(viewItem.wallet.account)
    }

//    fun onRefresh() {
//        if (_isRefreshing.value != null && _isRefreshing.value == true) {
//            return
//        }
//
//        viewModelScope.launch {
//            service.refresh()
//            // A fake 2 seconds 'refresh'
//            _isRefreshing.postValue(true)
//            delay(2300)
//            _isRefreshing.postValue(false)
//        }
//    }
//
//
//
//    fun onResume() {
//        rateAppService.onBalancePageActive()
//    }
//
//    fun onPause() {
//        rateAppService.onBalancePageInactive()
//    }
//
//    fun setSortType(value: BalanceSortType) {
//        service.sortType = value
//        sortTypeUpdatedLiveData.postValue(value)
//    }
//
//    override fun onCleared() {
//        activeAccountService.clear()
//        service.clear()
//        rateAppService.clear()
//    }
//
//
//    fun disable(viewItem: BalanceViewItem) {
//        service.disable(viewItem.wallet)
//    }
//
//    fun enable(wallet: Wallet) {
//        service.enable(wallet)
//    }
//
//    fun getSyncErrorDetails(viewItem: BalanceViewItem): SyncError = when {
//        service.networkAvailable -> {
//            SyncError.Dialog(
//                viewItem.wallet,
//                viewItem.errorMessage ?: "",
//                sourceChangeable(viewItem.wallet.coinType)
//            )
//        }
//        else -> {
//            SyncError.NetworkNotAvailable()
//        }
//    }
//
//    private fun sourceChangeable(coinType: CoinType) = when (coinType) {
//        is CoinType.Bitcoin,
//        is CoinType.BitcoinCash,
//        is CoinType.Dash,
//        is CoinType.Litecoin,
//        is CoinType.BinanceSmartChain,
//        is CoinType.Bep20,
//        is CoinType.Ethereum,
//        is CoinType.Erc20 -> true
//        else -> false
//    }
//
//    fun refreshByWallet(wallet: Wallet) {
//        service.refreshByWallet(wallet)
//    }
//
//    fun getWalletForReceive(viewItem: BalanceViewItem) = when {
//        viewItem.wallet.account.isBackedUp -> viewItem.wallet
//        else -> throw BackupRequiredError(viewItem.wallet.account)
//    }
//
//    fun onChangeSourceClick(wallet: Wallet) = when (wallet.coinType) {
//        CoinType.Bitcoin,
//        CoinType.BitcoinCash,
//        CoinType.Dash,
//        CoinType.Litecoin -> openPrivacySettingsLiveEvent.call()
//        CoinType.Ethereum,
//        is CoinType.Erc20 -> openEvmNetworkSettingsLiveEvent.postValue(
//            Pair(Blockchain.Ethereum, wallet.account)
//        )
//        CoinType.BinanceSmartChain,
//        is CoinType.Bep20 -> openEvmNetworkSettingsLiveEvent.postValue(
//            Pair(Blockchain.BinanceSmartChain, wallet.account)
//        )
//        else -> {
//        }
//    }
//
//    sealed class SyncError {
//        class NetworkNotAvailable : SyncError()
//        class Dialog(val wallet: Wallet, val errorMessage: String, val sourceChangeable: Boolean) :
//            SyncError()
//    }
}


class BackupRequiredError(val account: Account) : Error("Backup Required")
