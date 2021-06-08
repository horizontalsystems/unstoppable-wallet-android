package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.storage.AccountSettingRecordStorage
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountSettingRecord
import io.horizontalsystems.bankwallet.entities.EvmNetwork
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class AccountSettingManager(val storage: AccountSettingRecordStorage, val evmNetworkManager: EvmNetworkManager) {

    private val ethereumNetworkKey = "ethereum-network"
    private val binanceSmartChainNetworkKey = "binance-smart-chain-network"

    private val ethereumNetworkRelay = PublishSubject.create<Pair<Account, EvmNetwork>>()
    private val binanceSmartChainNetworkRelay = PublishSubject.create<Pair<Account, EvmNetwork>>()

    private fun evmNetwork(account: Account, networks: List<EvmNetwork>, key: String): EvmNetwork {
        return storage.accountSetting(account.id, key)?.let { setting ->
            networks.firstOrNull { it.id == setting.value }
        } ?: networks.first()
    }

    private fun save(network: EvmNetwork, account: Account, key: String) {
        val record = AccountSettingRecord(account.id, key, network.id)
        storage.save(record)
    }


    val ethereumNetworkObservable: Observable<Pair<Account, EvmNetwork>>
        get() = ethereumNetworkRelay

    val binanceSmartChainNetworkObservable: Observable<Pair<Account, EvmNetwork>>
        get() = binanceSmartChainNetworkRelay

    fun ethereumNetwork(account: Account): EvmNetwork {
        return evmNetwork(account, evmNetworkManager.ethereumNetworks, ethereumNetworkKey)
    }

    fun saveEth(ethereumNetwork: EvmNetwork, account: Account) {
        save(ethereumNetwork, account, ethereumNetworkKey)
        ethereumNetworkRelay.onNext(Pair(account, ethereumNetwork))
    }

    fun binanceSmartChainNetwork(account: Account): EvmNetwork {
        return evmNetwork(account, evmNetworkManager.binanceSmartChainNetworks, binanceSmartChainNetworkKey)
    }

    fun saveBsc(binanceSmartChainNetwork: EvmNetwork, account: Account) {
        save(binanceSmartChainNetwork, account, binanceSmartChainNetworkKey)
        binanceSmartChainNetworkRelay.onNext(Pair(account, binanceSmartChainNetwork))
    }
}