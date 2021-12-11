package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.EvmNetwork
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.oneinchkit.OneInchKit
import io.horizontalsystems.uniswapkit.UniswapKit
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.net.URL

interface IEvmNetworkProvider {
    val evmNetworkObservable: Observable<Pair<Account, EvmNetwork>>

    fun getEvmNetwork(account: Account): EvmNetwork
}

class EvmNetworkProviderBsc(private val accountSettingManager: AccountSettingManager) : IEvmNetworkProvider {
    override val evmNetworkObservable: Observable<Pair<Account, EvmNetwork>>
        get() = accountSettingManager.binanceSmartChainNetworkObservable

    override fun getEvmNetwork(account: Account) = accountSettingManager.binanceSmartChainNetwork(account)
}

class EvmNetworkProviderEth(private val accountSettingManager: AccountSettingManager) : IEvmNetworkProvider {
    override val evmNetworkObservable: Observable<Pair<Account, EvmNetwork>>
        get() = accountSettingManager.ethereumNetworkObservable

    override fun getEvmNetwork(account: Account) = accountSettingManager.ethereumNetwork(account)
}

class EvmKitManager(
    private val etherscanApiKey: String,
    private val backgroundManager: BackgroundManager,
    private val evmNetworkProvider: IEvmNetworkProvider
) : BackgroundManager.Listener {

    private val disposables = CompositeDisposable()

    init {
        backgroundManager.registerListener(this)

        evmNetworkProvider.evmNetworkObservable
            .subscribeIO { (account, _) ->
                handleUpdateNetwork(account)
            }
            .let {
                disposables.add(it)
            }
    }

    private fun handleUpdateNetwork(account: Account) {
        if (account != currentAccount) return

        stopEvmKit()

        evmKitUpdatedRelay.onNext(Unit)
    }

    var evmKit: EthereumKit? = null
        private set
    private var useCount = 0
    private var currentAccount: Account? = null
    private val evmKitUpdatedRelay = PublishSubject.create<Unit>()

    val evmKitUpdatedObservable: Observable<Unit>
        get() = evmKitUpdatedRelay

    val statusInfo: Map<String, Any>?
        get() = evmKit?.statusInfo()

    @Synchronized
    fun evmKit(account: Account): EthereumKit {
        if (this.evmKit != null && currentAccount != account) {
            stopEvmKit()
        }

        if (this.evmKit == null) {
            if (account.type !is AccountType.Mnemonic)
                throw UnsupportedAccountException()

            useCount = 0

            this.evmKit = createKitInstance(account.type, account)
            currentAccount = account
        }

        useCount++
        return this.evmKit!!
    }

    private fun createKitInstance(accountType: AccountType.Mnemonic, account: Account): EthereumKit {
        val evmNetwork = evmNetworkProvider.getEvmNetwork(account)
        val kit = EthereumKit.getInstance(
            App.instance,
            accountType.words,
            accountType.passphrase,
            evmNetwork.networkType,
            evmNetwork.syncSource,
            etherscanApiKey,
            account.id
        )

        Erc20Kit.addTransactionSyncer(kit)
        Erc20Kit.addDecorator(kit)

        UniswapKit.addDecorator(kit)
        UniswapKit.addTransactionWatcher(kit)

        OneInchKit.addDecorator(kit)
        OneInchKit.addTransactionWatcher(kit)

        kit.start()

        return kit
    }

    @Synchronized
    fun unlink(account: Account) {
        if (account == currentAccount) {
            useCount -= 1

            if (useCount < 1) {
                stopEvmKit()
            }
        }
    }

    private fun stopEvmKit() {
        evmKit?.stop()
        evmKit = null
        currentAccount = null
    }

    //
    // BackgroundManager.Listener
    //

    override fun willEnterForeground() {
        super.willEnterForeground()
        this.evmKit?.onEnterForeground()
    }

    override fun didEnterBackground() {
        super.didEnterBackground()
        this.evmKit?.onEnterBackground()
    }
}

val EthereumKit.SyncSource.urls: List<URL>
    get() = when (this) {
        is EthereumKit.SyncSource.WebSocket -> listOf(url)
        is EthereumKit.SyncSource.Http -> urls
    }
