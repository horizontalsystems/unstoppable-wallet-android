package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.factories.AdapterFactory
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import java.util.concurrent.ConcurrentHashMap

class AdapterManager(
    private val walletManager: IWalletManager,
    private val adapterFactory: AdapterFactory,
    private val btcBlockchainManager: BtcBlockchainManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val solanaKitManager: SolanaKitManager,
    private val tronKitManager: TronKitManager,
    private val tonKitManager: TonKitManager
) : IAdapterManager {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val adaptersReadySubject = PublishSubject.create<Map<Wallet, IAdapter>>()
    private val adaptersMap = ConcurrentHashMap<Wallet, IAdapter>()

    override val adaptersReadyObservable: Flowable<Map<Wallet, IAdapter>> =
        adaptersReadySubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun startAdapterManager() {
        coroutineScope.launch {
            walletManager.activeWalletsUpdatedObservable.asFlow().collect { wallets ->
                initAdapters(wallets)
            }
        }
        coroutineScope.launch {
            btcBlockchainManager.restoreModeUpdatedObservable.asFlow().collect {
                handleUpdatedRestoreMode(it)
            }
        }
        coroutineScope.launch {
            solanaKitManager.kitStoppedObservable.asFlow().collect {
                handleUpdatedKit(BlockchainType.Solana)
            }
        }
        for (blockchain in evmBlockchainManager.allBlockchains) {
            coroutineScope.launch {
                evmBlockchainManager.getEvmKitManager(blockchain.type).evmKitUpdatedObservable.asFlow()
                    .collect {
                        handleUpdatedKit(blockchain.type)
                    }
            }
        }
    }

    private fun handleUpdatedKit(blockchainType: BlockchainType) {
        val wallets = adaptersMap.keys().toList().filter {
            it.token.blockchain.type == blockchainType
        }

        if (wallets.isEmpty()) return

        wallets.forEach {
            adaptersMap[it]?.stop()
            adaptersMap.remove(it)
        }

        initAdapters(walletManager.activeWallets)
    }

    private fun handleUpdatedRestoreMode(blockchainType: BlockchainType) {
        val wallets = adaptersMap.keys().toList().filter {
            it.token.blockchainType == blockchainType
        }

        if (wallets.isEmpty()) return

        wallets.forEach {
            adaptersMap[it]?.stop()
            adaptersMap.remove(it)
        }

        initAdapters(walletManager.activeWallets)
    }

    override suspend fun refresh() {
        adaptersMap.values.forEach { it.refresh() }

        for (blockchain in evmBlockchainManager.allBlockchains) {
            evmBlockchainManager.getEvmKitManager(blockchain.type).evmKitWrapper?.evmKit?.refresh()
        }

        solanaKitManager.solanaKitWrapper?.solanaKit?.refresh()
        tronKitManager.tronKitWrapper?.tronKit?.refresh()
        tonKitManager.tonKitWrapper?.tonKit?.refresh()
    }

    @Synchronized
    private fun initAdapters(wallets: List<Wallet>) {
        val currentAdapters = adaptersMap.toMutableMap()
        adaptersMap.clear()

        wallets.forEach { wallet ->
            var adapter = currentAdapters.remove(wallet)
            if (adapter == null) {
                adapterFactory.getAdapterOrNull(wallet)?.let {
                    it.start()

                    adapter = it
                }
            }

            adapter?.let {
                adaptersMap[wallet] = it
            }
        }

        adaptersReadySubject.onNext(adaptersMap)

        currentAdapters.forEach { (wallet, adapter) ->
            adapter.stop()
            adapterFactory.unlinkAdapter(wallet)
        }
    }

    override fun refreshByWallet(wallet: Wallet) {
        val blockchain = evmBlockchainManager.getBlockchain(wallet.token)

        if (blockchain != null) {
            evmBlockchainManager.getEvmKitManager(blockchain.type).evmKitWrapper?.evmKit?.refresh()
        } else {
            adaptersMap[wallet]?.refresh()
        }
    }

    override fun getAdapterForWallet(wallet: Wallet): IAdapter? {
        return adaptersMap[wallet]
    }

    override fun getAdapterForToken(token: Token): IAdapter? {
        return walletManager.activeWallets.firstOrNull { it.token == token }
            ?.let { wallet ->
                adaptersMap[wallet]
            }
    }

    override fun getBalanceAdapterForWallet(wallet: Wallet): IBalanceAdapter? {
        return adaptersMap[wallet]?.let { it as? IBalanceAdapter }
    }

    override fun getReceiveAdapterForWallet(wallet: Wallet): IReceiveAdapter? {
        return adaptersMap[wallet]?.let { it as? IReceiveAdapter }
    }

}
