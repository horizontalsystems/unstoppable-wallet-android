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
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.oneinchkit.OneInchKit
import io.horizontalsystems.uniswapkit.UniswapKit
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.net.URL

interface IEvmNetworkProvider {
    val evmNetworkObservable: Observable<Pair<Account, EvmNetwork>>

    fun getEvmNetwork(account: Account): EvmNetwork
}

class EvmNetworkProviderBsc(private val accountSettingManager: AccountSettingManager) :
    IEvmNetworkProvider {
    override val evmNetworkObservable: Observable<Pair<Account, EvmNetwork>>
        get() = accountSettingManager.binanceSmartChainNetworkObservable

    override fun getEvmNetwork(account: Account) =
        accountSettingManager.binanceSmartChainNetwork(account)
}

class EvmNetworkProviderEth(private val accountSettingManager: AccountSettingManager) :
    IEvmNetworkProvider {
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

    private val kitStartedSubject = BehaviorSubject.createDefault(false)
    val kitStartedObservable: Observable<Boolean> = kitStartedSubject

    var evmKitWrapper: EvmKitWrapper? = null
        private set(value) {
            field = value

            kitStartedSubject.onNext(value != null)
        }

    private var useCount = 0
    var currentAccount: Account? = null
        private set
    private val evmKitUpdatedRelay = PublishSubject.create<Unit>()

    val evmKitUpdatedObservable: Observable<Unit>
        get() = evmKitUpdatedRelay

    val statusInfo: Map<String, Any>?
        get() = evmKitWrapper?.evmKit?.statusInfo()

    @Synchronized
    fun evmKit(account: Account): EthereumKit{
        return evmKitWrapper(account).evmKit
    }

    @Synchronized
    fun signer(account: Account): Signer? {
        return evmKitWrapper(account).signer
    }

    @Synchronized
    fun evmKitWrapper(account: Account): EvmKitWrapper {
        if (this.evmKitWrapper != null && currentAccount != account) {
            stopEvmKit()
        }

        if (this.evmKitWrapper == null) {
            val accountType = account.type
            this.evmKitWrapper = when (accountType) {
                is AccountType.Mnemonic -> {
                    createKitInstance(accountType, account)
                }
                is AccountType.Address -> {
                    createKitInstance(accountType, account)
                }
                else -> throw UnsupportedAccountException()
            }
            useCount = 0
            currentAccount = account
        }

        useCount++
        return this.evmKitWrapper!!
    }

    private fun createKitInstance(
        accountType: AccountType.Mnemonic,
        account: Account
    ): EvmKitWrapper {
        val evmNetwork = evmNetworkProvider.getEvmNetwork(account)
        val seed = Mnemonic().toSeed(accountType.words, accountType.passphrase)
        val address = Signer.address(seed, evmNetwork.networkType)
        val signer = Signer.getInstance(seed, evmNetwork.networkType)

        val kit = EthereumKit.getInstance(
            App.instance,
            address,
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

        val wrapper = EvmKitWrapper(kit, signer)

        return wrapper
    }

    private fun createKitInstance(
        accountType: AccountType.Address,
        account: Account
    ): EvmKitWrapper {
        val evmNetwork = evmNetworkProvider.getEvmNetwork(account)
        val address = accountType.address

        val kit = EthereumKit.getInstance(
            App.instance,
            Address(address),
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

        val wrapper = EvmKitWrapper(kit, null)

        return wrapper
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
        evmKitWrapper?.evmKit?.stop()
        evmKitWrapper = null
        currentAccount = null
    }

    //
    // BackgroundManager.Listener
    //

    override fun willEnterForeground() {
        super.willEnterForeground()
        this.evmKitWrapper?.evmKit?.onEnterForeground()
    }

    override fun didEnterBackground() {
        super.didEnterBackground()
        this.evmKitWrapper?.evmKit?.onEnterBackground()
    }
}

val EthereumKit.SyncSource.urls: List<URL>
    get() = when (this) {
        is EthereumKit.SyncSource.WebSocket -> listOf(url)
        is EthereumKit.SyncSource.Http -> urls
    }

class EvmKitWrapper(val evmKit: EthereumKit, val signer: Signer?) {

    fun sendSingle(
        transactionData: TransactionData,
        gasPrice: GasPrice,
        gasLimit: Long,
        nonce: Long? = null
    ): Single<FullTransaction> {
        return if (signer != null) {
            evmKit.rawTransaction(transactionData, gasPrice, gasLimit, nonce)
                .flatMap { rawTransaction ->
                    val signature = signer.signature(rawTransaction)
                    evmKit.send(rawTransaction, signature)
                }
        } else {
            Single.error(Exception())
        }
    }

}
