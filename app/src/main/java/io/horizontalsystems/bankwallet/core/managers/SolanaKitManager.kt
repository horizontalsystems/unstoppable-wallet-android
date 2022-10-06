package io.horizontalsystems.bankwallet.core.managers

import android.os.Handler
import android.os.Looper
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.solanakit.Signer
import io.horizontalsystems.solanakit.SolanaKit
import io.horizontalsystems.solanakit.models.RpcSource
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class SolanaKitManager(
    val rpcSource: RpcSource,
    backgroundManager: BackgroundManager
) : BackgroundManager.Listener {

    private val disposables = CompositeDisposable()

    init {
        backgroundManager.registerListener(this)
    }

    private fun handleUpdateNetwork(blockchainType: BlockchainType) {
        stopKit()

        solanaKitUpdatedSubject.onNext(Unit)
    }

    private val kitStartedSubject = BehaviorSubject.createDefault(false)
    val kitStartedObservable: Observable<Boolean> = kitStartedSubject

    var solanaKitWrapper: SolanaKitWrapper? = null
        private set(value) {
            field = value

            kitStartedSubject.onNext(value != null)
        }

    private var useCount = 0
    var currentAccount: Account? = null
        private set
    private val solanaKitUpdatedSubject = PublishSubject.create<Unit>()

    val kitUpdatedObservable: Observable<Unit>
        get() = solanaKitUpdatedSubject

    val statusInfo: Map<String, Any>?
        get() = LinkedHashMap<String, Any>()
//        get() = solanaKitWrapper?.solanaKit?.statusInfo()

    @Synchronized
    fun getSolanaKitWrapper(account: Account): SolanaKitWrapper {
        if (this.solanaKitWrapper != null && currentAccount != account) {
            stopKit()
        }

        if (this.solanaKitWrapper == null) {
            val accountType = account.type
            this.solanaKitWrapper = when (accountType) {
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
        return this.solanaKitWrapper!!
    }

    private fun createKitInstance(
        accountType: AccountType.Mnemonic,
        account: Account
    ): SolanaKitWrapper {
        val seed = accountType.seed
        val publicKey = Signer.address(seed)
        val signer = Signer.getInstance(seed)

        val kit = SolanaKit.getInstance(
            App.instance,
            publicKey,
            rpcSource,
            account.id
        )

        kit.start()

        return SolanaKitWrapper(kit, signer)
    }

    private fun createKitInstance(
        accountType: AccountType.Address,
        account: Account
    ): SolanaKitWrapper {
        val address = accountType.address

        val kit = SolanaKit.getInstance(
            App.instance,
            address,
            rpcSource,
            account.id
        )

        kit.start()

        return SolanaKitWrapper(kit, null)
    }

    @Synchronized
    fun unlink(account: Account) {
        if (account == currentAccount) {
            useCount -= 1

            if (useCount < 1) {
                stopKit()
            }
        }
    }

    private fun stopKit() {
        solanaKitWrapper?.solanaKit?.stop()
        solanaKitWrapper = null
        currentAccount = null
    }

    //
    // BackgroundManager.Listener
    //

    override fun willEnterForeground() {
        this.solanaKitWrapper?.solanaKit?.let { kit ->
            Handler(Looper.getMainLooper()).postDelayed({
                kit.refresh()
            }, 1000)
        }
    }

    override fun didEnterBackground() = Unit
}

class SolanaKitWrapper(val solanaKit: SolanaKit, val signer: Signer?)