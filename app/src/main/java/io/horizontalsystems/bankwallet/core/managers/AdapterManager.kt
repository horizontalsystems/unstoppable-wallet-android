package io.horizontalsystems.bankwallet.core.managers

import android.os.Handler
import android.os.HandlerThread
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.adapters.BaseEvmAdapter
import io.horizontalsystems.bankwallet.core.adapters.BinanceAdapter
import io.horizontalsystems.bankwallet.core.factories.AdapterFactory
import io.horizontalsystems.bankwallet.entities.ConfiguredCoin
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource.*
import io.horizontalsystems.bankwallet.modules.transactions.TransactionWallet
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.ConcurrentHashMap

class AdapterManager(
        private val walletManager: IWalletManager,
        private val adapterFactory: AdapterFactory,
        private val ethereumKitManager: EvmKitManager,
        private val binanceSmartChainKitManager: EvmKitManager,
        private val binanceKitManager: BinanceKitManager
) : IAdapterManager, HandlerThread("A") {

    private val handler: Handler
    private val disposables = CompositeDisposable()
    private val adaptersReadySubject = PublishSubject.create<Unit>()
    private val adaptersMap = ConcurrentHashMap<Wallet, IAdapter>()
    private var ethereumTransactionsAdapter: ITransactionsAdapter? = null
    private var bscTransactionsAdapter: ITransactionsAdapter? = null


    override val adaptersReadyObservable: Flowable<Unit> = adaptersReadySubject.toFlowable(BackpressureStrategy.BUFFER)

    init {
        start()
        handler = Handler(looper)

        disposables.add(walletManager.activeWalletsUpdatedObservable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { wallets ->
                    initAdapters(wallets)
                }
        )

        ethereumKitManager.evmKitUpdatedObservable
            .subscribeIO {
                handleUpdatedEthereumKit()
            }
            .let {
                disposables.add(it)
            }

        binanceSmartChainKitManager.evmKitUpdatedObservable
            .subscribeIO {
                handleUpdatedBinanceSmartChainKit()
            }
            .let {
                disposables.add(it)
            }
    }

    private fun handleUpdatedEthereumKit() {
        handleUpdatedKit {
            it.coin.type is CoinType.Ethereum || it.coin.type is CoinType.Erc20
        }
    }

    private fun handleUpdatedBinanceSmartChainKit() {
        handleUpdatedKit {
            it.coin.type is CoinType.BinanceSmartChain || it.coin.type is CoinType.Bep20
        }
    }

    private fun handleUpdatedKit(filter: (Wallet) -> Boolean) {
        val wallets = adaptersMap.keys().toList().filter(filter)

        if (wallets.isEmpty()) return

        wallets.forEach {
            adaptersMap[it]?.stop()
            adaptersMap.remove(it)
        }

        initAdapters(walletManager.activeWallets)
    }

    override fun preloadAdapters() {
        handler.post {
            initAdapters(walletManager.activeWallets)
        }
    }

    override fun refresh() {
        handler.post {
            adaptersMap.values.forEach { it.refresh() }
        }

        ethereumKitManager.evmKit?.refresh()
        binanceSmartChainKitManager.evmKit?.refresh()
        binanceKitManager.binanceKit?.refresh()
    }

    @Synchronized
    private fun initAdapters(wallets: List<Wallet>) {
        ethereumTransactionsAdapter = evmTransactionAdapter(wallets, Blockchain.Ethereum)
        bscTransactionsAdapter = evmTransactionAdapter(wallets, Blockchain.BinanceSmartChain)

        val disabledWallets = adaptersMap.keys.subtract(wallets)

        wallets.forEach { wallet ->
            if (!adaptersMap.containsKey(wallet)) {
                adapterFactory.adapter(wallet)?.let { adapter ->
                    adaptersMap[wallet] = adapter

                    adapter.start()
                }
            }
        }

        adaptersReadySubject.onNext(Unit)

        disabledWallets.forEach { wallet ->
            adaptersMap.remove(wallet)?.let { disabledAdapter ->
                disabledAdapter.stop()
                adapterFactory.unlinkAdapter(wallet)
            }
        }

    }

    private fun evmTransactionAdapter(
        wallets: List<Wallet>,
        blockchain: Blockchain
    ): ITransactionsAdapter? {
        wallets.forEach { wallet ->
            when (wallet.coin.type) {
                CoinType.Ethereum, is CoinType.Erc20 -> {
                    if (blockchain == Blockchain.Ethereum) {
                        return adapterFactory.ethereumTransactionsAdapter(wallet.account)
                    }
                }
                CoinType.BinanceSmartChain, is CoinType.Bep20 -> {
                    if (blockchain == Blockchain.BinanceSmartChain) {
                        return adapterFactory.bscTransactionsAdapter(wallet.account)
                    }
                }
                else -> {
                }
            }
        }
        return null
    }

    /**
     * Partial refresh of adapters
     * For the given list of wallets do:
     * - remove corresponding adapters from adaptersMap and stop them
     * - create new adapters, start and add them to adaptersMap
     * - trigger adaptersReadySubject
     */
    @Synchronized
    override fun refreshAdapters(wallets: List<Wallet>) {
        handler.post {
            val walletsToRefresh = wallets.filter { adaptersMap.containsKey(it) }

            //remove and stop adapters
            walletsToRefresh.forEach { wallet ->
                adaptersMap.remove(wallet)?.let { previousAdapter ->
                    previousAdapter.stop()
                    adapterFactory.unlinkAdapter(wallet)
                }
            }

            //add and start new adapters
            walletsToRefresh.forEach { wallet ->
                adapterFactory.adapter(wallet)?.let { adapter ->
                    adaptersMap[wallet] = adapter
                    adapter.start()
                }
            }

            adaptersReadySubject.onNext(Unit)
        }
    }

    override fun refreshByWallet(wallet: Wallet) {
        val adapter = adaptersMap[wallet] ?: return

        when (adapter) {
            is BinanceAdapter -> binanceKitManager.binanceKit?.refresh()
            is BaseEvmAdapter -> {
                when (wallet.coin.type) {
                    CoinType.Ethereum, is CoinType.Erc20 -> ethereumKitManager.evmKit?.refresh()
                    CoinType.BinanceSmartChain, is CoinType.Bep20 -> binanceSmartChainKitManager.evmKit?.refresh()
                }
            }
            else -> adapter.refresh()
        }

    }

    override fun getAdapterForWallet(wallet: Wallet): IAdapter? {
        return adaptersMap[wallet]
    }

    override fun getAdapterForCoin(coin: Coin): IAdapter? {
        return walletManager.activeWallets.firstOrNull { it.coin == coin }?.let { wallet ->
            adaptersMap[wallet]
        }
    }

    override fun getTransactionsAdapterForWallet(wallet: TransactionWallet): ITransactionsAdapter? {
        return when (wallet.source.blockchain) {
            Blockchain.Ethereum -> ethereumTransactionsAdapter
            Blockchain.BinanceSmartChain -> bscTransactionsAdapter
            else -> {
                wallet.coin?.let { coin ->
                    val configuredCoin = ConfiguredCoin(coin, wallet.source.coinSettings)
                    adaptersMap[Wallet(
                        configuredCoin,
                        wallet.source.account
                    )]?.let { it as? ITransactionsAdapter }
                }
            }
        }
    }

    override fun getBalanceAdapterForWallet(wallet: Wallet): IBalanceAdapter? {
        return adaptersMap[wallet]?.let { it as? IBalanceAdapter }
    }

    override fun getReceiveAdapterForWallet(wallet: Wallet): IReceiveAdapter? {
        return adaptersMap[wallet]?.let { it as? IReceiveAdapter }
    }

}
