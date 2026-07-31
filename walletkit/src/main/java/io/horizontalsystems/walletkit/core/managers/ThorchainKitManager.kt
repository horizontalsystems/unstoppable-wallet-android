package io.horizontalsystems.walletkit.core.managers

import android.util.Log
import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.BackgroundManager
import io.horizontalsystems.walletkit.core.BackgroundManagerState
import io.horizontalsystems.walletkit.core.UnsupportedAccountException
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.thorchainkit.ThorchainKit
import io.horizontalsystems.thorchainkit.network.Network
import io.horizontalsystems.thorchainkit.transaction.Signer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ThorchainKitManager(
    private val backgroundManager: BackgroundManager,
    private val rpcSourceManager: ThorchainRpcSourceManager,
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null
    private var sourceJob: Job? = null
    private val _kitStartedFlow = MutableStateFlow(false)
    val kitStartedFlow: StateFlow<Boolean> = _kitStartedFlow

    // Signals that the running kit was torn down because the user changed the RPC provider;
    // WalletManager reloads Thorchain wallets on this, which rebuilds the adapter + kit with
    // the newly selected source. tryEmit stays synchronous so it fires before handleUpdateNetwork
    // cancels the collector coroutine it runs in.
    private val _kitStoppedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val kitStoppedFlow: SharedFlow<Unit> = _kitStoppedFlow.asSharedFlow()

    var thorchainKitWrapper: ThorchainKitWrapper? = null
        private set(value) {
            field = value

            _kitStartedFlow.update { value != null }
        }

    private var useCount = 0
    var currentAccount: Account? = null
        private set

    val statusInfo: Map<String, Any>?
        get() = thorchainKitWrapper?.thorchainKit?.statusInfo()

    @Synchronized
    fun getThorchainKitWrapper(account: Account): ThorchainKitWrapper {
        if (this.thorchainKitWrapper != null && currentAccount != account) {
            stop()
        }

        if (this.thorchainKitWrapper == null) {
            val accountType = account.type
            this.thorchainKitWrapper = when (accountType) {
                is AccountType.Mnemonic -> {
                    createKitInstance(accountType, account)
                }

                else -> throw UnsupportedAccountException()
            }
            start()
            useCount = 0
            currentAccount = account
        }

        useCount++
        return this.thorchainKitWrapper!!
    }

    private fun createKitInstance(accountType: AccountType.Mnemonic, account: Account): ThorchainKitWrapper {
        // User-selected thornode provider first, then the others as failover. Overrides the
        // kit's built-in Network defaults; every URL ends in "/" as Retrofit requires.
        val thornodeUrls = rpcSourceManager.thornodeUrls()
        val kit = ThorchainKit.getInstance(
            App.instance,
            accountType.seed,
            Network.Mainnet,
            account.id,
            thornodeUrls = thornodeUrls,
        )

        return ThorchainKitWrapper(kit)
    }

    fun getSigner(accountType: AccountType): Signer = when (accountType) {
        is AccountType.Mnemonic -> Signer.getInstance(accountType.seed, Network.Mainnet)
        else -> throw UnsupportedAccountException()
    }

    fun getAddress(accountType: AccountType): String = when (accountType) {
        is AccountType.Mnemonic -> ThorchainKit.getAddress(accountType.seed, Network.Mainnet).toString()
        else -> throw UnsupportedAccountException()
    }

    @Synchronized
    fun unlink(account: Account) {
        if (account == currentAccount) {
            useCount -= 1

            if (useCount < 1) {
                stop()
            }
        }
    }

    private fun handleUpdateNetwork() {
        stop()
        _kitStoppedFlow.tryEmit(Unit)
    }

    private fun stop() {
        thorchainKitWrapper?.thorchainKit?.stop()
        job?.cancel()
        sourceJob?.cancel()
        thorchainKitWrapper = null
        currentAccount = null
    }

    private fun start() {
        thorchainKitWrapper?.thorchainKit?.start()
        // Re-established on every kit creation, mirroring SolanaKitManager: the collector only
        // lives while a kit exists, and reloadWallets recreates it after a provider change.
        sourceJob = scope.launch {
            rpcSourceManager.rpcSourceUpdatedFlow.collect {
                handleUpdateNetwork()
            }
        }
        job = scope.launch {
            backgroundManager.stateFlow.collect { state ->
                // a throw here would cancel the shared scope for good — start() reuses
                // it after stop(), so background handling would silently die forever
                try {
                    when (state) {
                        // Pause while backgrounded: Android cuts network for the process,
                        // so timer syncs only fail with UnknownHostException and leave the
                        // kit showing NotSynced when the app comes back
                        BackgroundManagerState.EnterBackground -> {
                            thorchainKitWrapper?.thorchainKit?.pause()
                        }

                        BackgroundManagerState.EnterForeground -> {
                            thorchainKitWrapper?.thorchainKit?.let { kit ->
                                delay(1000)
                                kit.resume()
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Log.e("ThorchainKitManager", "background state handling error: ${e.message}", e)
                }
            }
        }
    }
}

class ThorchainKitWrapper(val thorchainKit: ThorchainKit)

fun ThorchainKit.SyncState.toAdapterState(): AdapterState = when (this) {
    is ThorchainKit.SyncState.NotSynced -> AdapterState.NotSynced(error)
    is ThorchainKit.SyncState.Synced -> AdapterState.Synced
    is ThorchainKit.SyncState.Syncing -> AdapterState.Syncing()
}
