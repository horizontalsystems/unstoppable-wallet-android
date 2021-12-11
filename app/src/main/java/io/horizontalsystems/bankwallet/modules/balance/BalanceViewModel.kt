package io.horizontalsystems.bankwallet.modules.balance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.evmnetwork.EvmNetworkModule.Blockchain
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.marketkit.models.CoinType
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BalanceViewModel(
    private val service: BalanceService2,
    private val rateAppService: RateAppService,
    private val activeAccountService: ActiveAccountService,
    private val balanceViewItemFactory: BalanceViewItemFactory,
    val reportEmail: String
) : ViewModel() {

    val titleLiveData = MutableLiveData<String>()
    val headerViewItemLiveData = MutableLiveData<BalanceHeaderViewItem>()
    val disabledWalletLiveData = SingleLiveEvent<Wallet>()
    val openPrivacySettingsLiveEvent = SingleLiveEvent<Unit>()
    val openEvmNetworkSettingsLiveEvent = SingleLiveEvent<Pair<Blockchain, Account>>()
    val sortTypeUpdatedLiveData = MutableLiveData(service.sortType)

    private val _balanceViewItemsLiveData = MutableLiveData<List<BalanceViewItem>>()
    val balanceViewItems: LiveData<List<BalanceViewItem>> = _balanceViewItemsLiveData

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private var disposables = CompositeDisposable()

    private var expandedWallet: Wallet? = null

    init {
        service.disabledWalletSubject
            .subscribeIO {
                disabledWalletLiveData.postValue(it)
            }
            .let {
                disposables.add(it)
            }

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
        _balanceViewItemsLiveData.postValue(service.balanceItems.map { balanceItem ->
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
                service.balanceItems,
                service.baseCurrency,
                service.balanceHidden
            )
        )
    }

    fun onRefresh() {
        if (_isRefreshing.value != null && _isRefreshing.value == true) {
            return
        }

        viewModelScope.launch {
            service.refresh()
            // A fake 2 seconds 'refresh'
            _isRefreshing.postValue(true)
            delay(2300)
            _isRefreshing.postValue(false)
        }
    }

    fun onBalanceClick() {
        service.balanceHidden = !service.balanceHidden

        refreshViewItems()
        refreshHeaderViewItem()
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

    fun setSortType(value: BalanceSortType) {
        service.sortType = value
        sortTypeUpdatedLiveData.postValue(value)
    }

    fun getSyncErrorDetails(viewItem: BalanceViewItem): SyncError = when {
        service.networkAvailable -> {
            SyncError.Dialog(
                viewItem.wallet,
                viewItem.errorMessage ?: "",
                sourceChangeable(viewItem.wallet.coinType)
            )
        }
        else -> {
            SyncError.NetworkNotAvailable()
        }
    }

    private fun sourceChangeable(coinType: CoinType) = when (coinType) {
        is CoinType.Bitcoin,
        is CoinType.BitcoinCash,
        is CoinType.Dash,
        is CoinType.Litecoin,
        is CoinType.BinanceSmartChain,
        is CoinType.Bep20,
        is CoinType.Ethereum,
        is CoinType.Erc20 -> true
        else -> false
    }

    fun refreshByWallet(wallet: Wallet) {
        service.refreshByWallet(wallet)
    }

    override fun onCleared() {
        activeAccountService.clear()
        service.clear()
        rateAppService.clear()
    }

    fun getWalletForReceive(viewItem: BalanceViewItem) = when {
        viewItem.wallet.account.isBackedUp -> viewItem.wallet
        else -> throw BackupRequiredError(viewItem.wallet.account)
    }

    fun disable(viewItem: BalanceViewItem) {
        service.disable(viewItem.wallet)
    }

    fun enable(wallet: Wallet) {
        service.enable(wallet)
    }

    fun onChangeSourceClick(wallet: Wallet) = when (wallet.coinType) {
        CoinType.Bitcoin,
        CoinType.BitcoinCash,
        CoinType.Dash,
        CoinType.Litecoin -> openPrivacySettingsLiveEvent.call()
        CoinType.Ethereum,
        is CoinType.Erc20 -> openEvmNetworkSettingsLiveEvent.postValue(
            Pair(Blockchain.Ethereum, wallet.account)
        )
        CoinType.BinanceSmartChain,
        is CoinType.Bep20 -> openEvmNetworkSettingsLiveEvent.postValue(
            Pair(Blockchain.BinanceSmartChain, wallet.account)
        )
        else -> {
        }
    }

    sealed class SyncError {
        class NetworkNotAvailable : SyncError()
        class Dialog(val wallet: Wallet, val errorMessage: String, val sourceChangeable: Boolean) :
            SyncError()
    }

    class BackupRequiredError(val account: Account) : Error("Backup Required")

}
