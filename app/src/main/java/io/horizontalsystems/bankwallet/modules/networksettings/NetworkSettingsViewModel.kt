package io.horizontalsystems.bankwallet.modules.networksettings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.blockchainLogo
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.evmnetwork.EvmNetworkModule
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.marketkit.models.CoinType
import io.reactivex.disposables.CompositeDisposable

class NetworkSettingsViewModel(private val service: NetworkSettingsService): ViewModel() {

    private val disposables = CompositeDisposable()

    val viewItemsLiveData = MutableLiveData(listOf<ViewItem>())
    val openEvmNetworkLiveEvent = SingleLiveEvent<Pair<EvmNetworkModule.Blockchain, Account>>()

    init {
        service.itemsObservable
            .subscribeIO {
                sync(it)
            }
            .let {
                disposables.add(it)
            }

        sync(service.items)
    }

    private fun sync(items: List<NetworkSettingsService.Item>) {
        val viewItems = items.map { item ->
            ViewItem(
                iconResource(item.blockchain),
                title(item.blockchain),
                item.value,
                item
            )
        }

        viewItemsLiveData.postValue(viewItems)
    }

    private fun iconResource(blockchain: NetworkSettingsService.Blockchain) = when (blockchain) {
        NetworkSettingsService.Blockchain.Ethereum -> CoinType.Ethereum.blockchainLogo
        NetworkSettingsService.Blockchain.BinanceSmartChain -> CoinType.BinanceSmartChain.blockchainLogo
    }

    private fun title(blockchain: NetworkSettingsService.Blockchain) = when (blockchain) {
        NetworkSettingsService.Blockchain.Ethereum -> "Ethereum"
        NetworkSettingsService.Blockchain.BinanceSmartChain -> "BSC"
    }

    data class ViewItem(
        val iconResId: Int,
        val title: String,
        val value: String,
        val item: NetworkSettingsService.Item
    )

    fun onSelect(viewItem: ViewItem) {
        val evmNetworkBlockchain = when (viewItem.item.blockchain) {
            NetworkSettingsService.Blockchain.Ethereum -> EvmNetworkModule.Blockchain.Ethereum
            NetworkSettingsService.Blockchain.BinanceSmartChain -> EvmNetworkModule.Blockchain.BinanceSmartChain
        }

        openEvmNetworkLiveEvent.postValue(Pair(evmNetworkBlockchain, service.account))
    }

    override fun onCleared() {
        service.clear()
        disposables.clear()
    }
}
