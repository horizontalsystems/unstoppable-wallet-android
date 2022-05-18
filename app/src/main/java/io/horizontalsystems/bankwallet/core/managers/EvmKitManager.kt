package io.horizontalsystems.bankwallet.core.managers

import android.os.Handler
import android.os.Looper
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.EvmBlockchain
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.models.*
import io.horizontalsystems.oneinchkit.OneInchKit
import io.horizontalsystems.uniswapkit.UniswapKit
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.net.URL

class EvmKitManager(
    val chain: Chain,
    backgroundManager: BackgroundManager,
    private val syncSourceManager: EvmSyncSourceManager
) : BackgroundManager.Listener {

    private val disposables = CompositeDisposable()

    init {
        backgroundManager.registerListener(this)

        syncSourceManager.syncSourceObservable
            .subscribeIO { blockchain ->
                handleUpdateNetwork(blockchain)
            }
            .let {
                disposables.add(it)
            }
    }

    private fun handleUpdateNetwork(blockchain: EvmBlockchain) {
        if (blockchain != evmKitWrapper?.blockchain) return

        stopEvmKit()

        evmKitUpdatedSubject.onNext(Unit)
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
    private val evmKitUpdatedSubject = PublishSubject.create<Unit>()

    val evmKitUpdatedObservable: Observable<Unit>
        get() = evmKitUpdatedSubject

    val statusInfo: Map<String, Any>?
        get() = evmKitWrapper?.evmKit?.statusInfo()

    @Synchronized
    fun getEvmKitWrapper(account: Account, blockchain: EvmBlockchain): EvmKitWrapper {
        if (this.evmKitWrapper != null && currentAccount != account) {
            stopEvmKit()
        }

        if (this.evmKitWrapper == null) {
            val accountType = account.type
            this.evmKitWrapper = when (accountType) {
                is AccountType.Mnemonic -> {
                    createKitInstance(accountType, account, blockchain)
                }
                is AccountType.Address -> {
                    createKitInstance(accountType, account, blockchain)
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
        account: Account,
        blockchain: EvmBlockchain
    ): EvmKitWrapper {
        val syncSource = syncSourceManager.getSyncSource(blockchain)
        val seed = accountType.seed
        val address = Signer.address(seed, chain)
        val signer = Signer.getInstance(seed, chain)

        val kit = EthereumKit.getInstance(
            App.instance,
            address,
            chain,
            syncSource.rpcSource,
            syncSource.transactionSource,
            account.id
        )

        Erc20Kit.addTransactionSyncer(kit)
        Erc20Kit.addDecorators(kit)

        UniswapKit.addDecorators(kit)
        OneInchKit.addDecorators(kit)

        kit.start()

        return EvmKitWrapper(kit, blockchain, signer)
    }

    private fun createKitInstance(
        accountType: AccountType.Address,
        account: Account,
        blockchain: EvmBlockchain
    ): EvmKitWrapper {
        val syncSource = syncSourceManager.getSyncSource(blockchain)
        val address = accountType.address

        val kit = EthereumKit.getInstance(
            App.instance,
            Address(address),
            chain,
            syncSource.rpcSource,
            syncSource.transactionSource,
            account.id
        )

        Erc20Kit.addTransactionSyncer(kit)
        Erc20Kit.addDecorators(kit)

        UniswapKit.addDecorators(kit)
        OneInchKit.addDecorators(kit)

        kit.start()

        return EvmKitWrapper(kit, blockchain, null)
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
        this.evmKitWrapper?.evmKit?.let { kit ->
            Handler(Looper.getMainLooper()).postDelayed({
                kit.refresh()
            }, 1000)
        }
    }

    override fun didEnterBackground() = Unit
}

val RpcSource.urls: List<URL>
    get() = when (this) {
        is RpcSource.WebSocket -> listOf(url)
        is RpcSource.Http -> urls
    }

class EvmKitWrapper(val evmKit: EthereumKit, val blockchain: EvmBlockchain, val signer: Signer?) {

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
