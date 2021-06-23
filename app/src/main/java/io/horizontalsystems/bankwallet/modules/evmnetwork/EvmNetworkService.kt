package io.horizontalsystems.bankwallet.modules.evmnetwork

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.AccountSettingManager
import io.horizontalsystems.bankwallet.core.managers.EvmNetworkManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.EvmNetwork
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class EvmNetworkService(
    val blockchain: EvmNetworkModule.Blockchain,
    private val account: Account,
    private val evmNetworkManager: EvmNetworkManager,
    private val accountSettingManager: AccountSettingManager
) : Clearable {
    private val disposables = CompositeDisposable()

    private val itemsRelay = BehaviorSubject.create<List<Item>>()
    var items = listOf<Item>()
        private set(value) {
            field = value

            itemsRelay.onNext(value)
        }

    private val networks: List<EvmNetwork>
        get() = when (blockchain) {
            EvmNetworkModule.Blockchain.Ethereum -> evmNetworkManager.ethereumNetworks
            EvmNetworkModule.Blockchain.BinanceSmartChain -> evmNetworkManager.binanceSmartChainNetworks
        }

    private val currentNetwork: EvmNetwork
        get() = when (blockchain) {
            EvmNetworkModule.Blockchain.Ethereum -> {
                accountSettingManager.ethereumNetwork(account)
            }
            EvmNetworkModule.Blockchain.BinanceSmartChain -> {
                accountSettingManager.binanceSmartChainNetwork(account)
            }
        }

    init {
        syncItems()
    }

    private fun syncItems() {
        val currentNetworkId = currentNetwork.id

        items = networks.map { network ->
            Item(network, network.networkType.isMainNet, network.id == currentNetworkId)
        }
    }

    val itemsObservable: Observable<List<Item>>
        get() = itemsRelay

    fun isConfirmationRequired(id: String): Boolean {
        if (currentNetwork.id == id) return false
        val item = items.find { it.network.id == id } ?: return false

        return !item.isMainNet
    }

    fun setCurrentNetwork(id: String) {
        if (currentNetwork.id == id) return

        val network = items.find { it.network.id == id }?.network ?: return

        when (blockchain) {
            EvmNetworkModule.Blockchain.Ethereum -> {
                accountSettingManager.saveEth(network, account)
            }
            EvmNetworkModule.Blockchain.BinanceSmartChain -> {
                accountSettingManager.saveBsc(network, account)
            }
        }
    }

    override fun clear() {
        disposables.clear()
    }

    data class Item(val network: EvmNetwork, val isMainNet: Boolean, val selected: Boolean)

}
