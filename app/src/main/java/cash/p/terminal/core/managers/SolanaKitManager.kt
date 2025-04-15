package cash.p.terminal.core.managers

import android.util.Log
import cash.p.terminal.core.App
import cash.p.terminal.core.UnsupportedAccountException
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountType
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.horizontalsystems.solanakit.Signer
import io.horizontalsystems.solanakit.SolanaKit
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class SolanaKitManager(
    private val appConfigProvider: AppConfigProvider,
    private val rpcSourceManager: SolanaRpcSourceManager,
    private val walletManager: SolanaWalletManager,
    private val backgroundManager: BackgroundManager
) {

    private val coroutineScope =
        CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            Log.d("SolanaKitManager", "Coroutine error", throwable)
        })
    private var backgroundEventListenerJob: Job? = null
    private var rpcUpdatedJob: Job? = null
    private var tokenAccountJob: Job? = null

    var solanaKitWrapper: SolanaKitWrapper? = null

    private var useCount = 0
    var currentAccount: Account? = null
        private set
    private val solanaKitStoppedSubject = PublishSubject.create<Unit>()

    val kitStoppedObservable: Observable<Unit>
        get() = solanaKitStoppedSubject

    val statusInfo: Map<String, Any>?
        get() = solanaKitWrapper?.solanaKit?.statusInfo()

    private fun handleUpdateNetwork() {
        stopKit()

        solanaKitStoppedSubject.onNext(Unit)
    }

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

                is AccountType.SolanaAddress -> {
                    createKitInstance(accountType, account)
                }

                else -> throw UnsupportedAccountException()
            }
            startKit()
            subscribeToEvents()
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
        val address = Signer.address(seed)
        val signer = Signer.getInstance(seed)

        val kit = SolanaKit.getInstance(
            application = App.instance,
            addressString = address,
            rpcSource = rpcSourceManager.rpcSource,
            walletId = account.id
        )

        return SolanaKitWrapper(kit, signer)
    }

    private fun createKitInstance(
        accountType: AccountType.SolanaAddress,
        account: Account
    ): SolanaKitWrapper {
        val address = accountType.address

        val kit = SolanaKit.getInstance(
            application = App.instance,
            addressString = address,
            rpcSource = rpcSourceManager.rpcSource,
            walletId = account.id
        )

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
        tokenAccountJob?.cancel()
    }

    private fun startKit() {
        solanaKitWrapper?.solanaKit?.let { kit ->
            tokenAccountJob = coroutineScope.launch {
                kit.start()
                kit.fungibleTokenAccountsFlow.collect {
                    walletManager.add(it)
                }
            }
        }
    }

    private fun subscribeToEvents() {
        backgroundEventListenerJob = coroutineScope.launch {
            backgroundManager.stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterForeground) {
                    startKit()
                } else if (state == BackgroundManagerState.EnterBackground) {
                    stopKit()
                }
            }
        }
        rpcUpdatedJob = coroutineScope.launch {
            rpcSourceManager.rpcSourceUpdateObservable.asFlow().collect {
                handleUpdateNetwork()
            }
        }
    }

}

class SolanaKitWrapper(val solanaKit: SolanaKit, val signer: Signer?)