package io.horizontalsystems.walletkit.core.managers

import android.util.Log
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.xrpkit.XrpKit
import io.horizontalsystems.xrpkit.XrpWallet
import io.horizontalsystems.xrpkit.models.TrustLine
import io.horizontalsystems.xrpkit.network.Network
import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.BackgroundManager
import io.horizontalsystems.walletkit.core.BackgroundManagerState
import io.horizontalsystems.walletkit.core.UnsupportedAccountException
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One XrpKit per active account, shared by every wallet of that account, refreshed on foreground. */
class XrpKitManager(
    private val backgroundManager: BackgroundManager,
    private val rpcSourceManager: XrpRpcSourceManager,
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null
    private var sourceJob: Job? = null
    private val _kitStartedFlow = MutableStateFlow(false)
    val kitStartedFlow: StateFlow<Boolean> = _kitStartedFlow

    // Signals that the running kit was torn down because the user changed the RPC provider;
    // WalletManager reloads XRP wallets on this, which rebuilds the adapters and the kit with
    // the newly selected source. tryEmit stays synchronous so it fires before
    // handleUpdateNetwork cancels the collector coroutine it runs in.
    private val _kitStoppedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val kitStoppedFlow: SharedFlow<Unit> = _kitStoppedFlow.asSharedFlow()

    var kitWrapper: XrpKitWrapper? = null
        private set(value) {
            field = value
            _kitStartedFlow.update { value != null }
        }

    private var useCount = 0
    var currentAccount: Account? = null
        private set

    val statusInfo: Map<String, Any>?
        get() = kitWrapper?.kit?.statusInfo()

    @Synchronized
    fun getKitWrapper(account: Account): XrpKitWrapper {
        if (this.kitWrapper != null && currentAccount != account) {
            stop()
        }

        if (this.kitWrapper == null) {
            val accountType = account.type
            this.kitWrapper = when (accountType) {
                is AccountType.Mnemonic,
                is AccountType.XrpAddress -> createKitInstance(accountType, account)

                else -> throw UnsupportedAccountException()
            }
            scope.launch {
                start()
            }
            useCount = 0
            currentAccount = account
        }

        useCount++
        return this.kitWrapper!!
    }

    private fun createKitInstance(accountType: AccountType, account: Account): XrpKitWrapper {
        val kit = XrpKit.getInstance(
            App.instance,
            accountType.toXrpWallet(),
            Network.MainNet,
            account.id,
            rpcUrls = rpcSourceManager.rpcUrls(),
        )
        return XrpKitWrapper(kit)
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
        kitWrapper?.kit?.stop()
        job?.cancel()
        sourceJob?.cancel()
        kitWrapper = null
        currentAccount = null
    }

    private fun start() {
        kitWrapper?.kit?.start()
        // Re-established on every kit creation: the collector only lives while a kit exists,
        // and reloadWallets recreates it after a provider change.
        // Both collectors share `scope`, which start() reuses after stop(): a throw in either
        // would cancel it for good and silently end lifecycle handling for every later kit.
        sourceJob = scope.launch {
            rpcSourceManager.rpcSourceUpdatedFlow.collect {
                try {
                    handleUpdateNetwork()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Log.e(TAG, "rpc source update handling error: ${e.message}", e)
                }
            }
        }
        job = scope.launch {
            backgroundManager.stateFlow.collectLatest { state ->
                try {
                    when (state) {
                        BackgroundManagerState.EnterForeground -> {
                            kitWrapper?.kit?.let { kit ->
                                kit.resume()
                                delay(1000)
                                kit.refresh()
                            }
                        }
                        BackgroundManagerState.EnterBackground -> {
                            kitWrapper?.kit?.pause()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Log.e(TAG, "background state handling error: ${e.message}", e)
                }
            }
        }
    }

    fun getAddress(accountType: AccountType): String {
        return XrpKit.getAddress(accountType.toXrpWallet())
    }

    companion object {
        private const val TAG = "XrpKitManager"
    }
}

class XrpKitWrapper(val kit: XrpKit) {
    /**
     * Trust lines the kit had in storage when this wrapper was created, before the kit was
     * started: the baseline XrpAccountManager's restore-time auto-enable gate is keyed on.
     */
    val persistedTrustLines: List<TrustLine> = kit.trustLines
}

fun XrpKit.SyncState.toAdapterState(): AdapterState = when (this) {
    is XrpKit.SyncState.NotSynced -> AdapterState.NotSynced(error)
    is XrpKit.SyncState.Synced -> AdapterState.Synced
    is XrpKit.SyncState.Syncing -> AdapterState.Syncing()
}

fun AccountType.toXrpWallet(): XrpWallet = when (this) {
    is AccountType.Mnemonic -> XrpWallet.Seed(seed)
    is AccountType.XrpAddress -> XrpWallet.WatchOnly(address)
    else -> throw IllegalArgumentException("Account type ${this.javaClass.simpleName} can not be converted to XrpWallet")
}

val TokenType.XrpAsset.displayCode: String
    get() = XrpKit.displayCurrencyCode(currency)
