package io.horizontalsystems.bankwallet.modules.networksettings

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmSyncSourceManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.EvmBlockchain
import io.horizontalsystems.bankwallet.entities.EvmSyncSource
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class NetworkSettingsService(
    val account: Account,
    private val evmSyncSourceManager: EvmSyncSourceManager,
    private val evmBlockchainManager: EvmBlockchainManager
) : Clearable {
    private val disposables = CompositeDisposable()

    private val itemsSubject = PublishSubject.create<List<Item>>()
    var items = listOf<Item>()
        private set(value) {
            field = value
            
            itemsSubject.onNext(value)
        }

    val itemsObservable: Observable<List<Item>>
        get() = itemsSubject

    init {
        evmSyncSourceManager.syncSourceObservable
            .subscribeIO {
//                handleSettingsUpdated(it.first)
            }.let {
                disposables.add(it)
            }

        syncItems()
    }

    private fun handleSettingsUpdated(account: Account) {
        if (account == this.account) {
            syncItems()
        }
    }

    private fun syncItems() {
//        items = evmBlockchainManager.allBlockchains.map {
//            Item(it, evmSyncSourceManager.getSyncSource(account, it))
//        }
    }

    override fun clear() {
        disposables.clear()
    }

    enum class Blockchain {
        Ethereum, BinanceSmartChain
    }

    data class Item(val blockchain: EvmBlockchain, val syncSource: EvmSyncSource)
}

