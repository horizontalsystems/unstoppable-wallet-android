package io.horizontalsystems.walletkit.core.managers

import android.util.Log
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.nearkit.NearKit
import io.horizontalsystems.nearkit.NearWallet
import io.horizontalsystems.nearkit.network.Network
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

/** One NearKit per active account, shared by every wallet of that account, refreshed on foreground. */
class NearKitManager(
    private val backgroundManager: BackgroundManager,
    private val rpcSourceManager: NearRpcSourceManager,
    private val restoreSettingsManager: RestoreSettingsManager,
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null
    private var sourceJob: Job? = null
    private val _kitStartedFlow = MutableStateFlow(false)
    val kitStartedFlow: StateFlow<Boolean> = _kitStartedFlow

    // Signals that the running kit was torn down because the user changed the RPC provider;
    // WalletManager reloads NEAR wallets on this, which rebuilds the adapters and the kit with
    // the newly selected source. tryEmit stays synchronous so it fires before
    // handleUpdateNetwork cancels the collector coroutine it runs in.
    private val _kitStoppedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val kitStoppedFlow: SharedFlow<Unit> = _kitStoppedFlow.asSharedFlow()

    var kitWrapper: NearKitWrapper? = null
        private set(value) {
            field = value
            _kitStartedFlow.update { value != null }
        }

    private var useCount = 0
    var currentAccount: Account? = null
        private set
    private var currentWallet: NearWallet? = null

    val statusInfo: Map<String, Any>?
        get() = kitWrapper?.kit?.statusInfo()

    @Synchronized
    fun getKitWrapper(account: Account): NearKitWrapper {
        val nearWallet = nearWallet(account)

        // The chosen NEAR account is a restore setting, not part of Account, so a change of it
        // alone must also replace the kit.
        if (this.kitWrapper != null && (currentAccount != account || currentWallet != nearWallet)) {
            stop()
        }

        if (this.kitWrapper == null) {
            this.kitWrapper = createKitInstance(nearWallet, account)
            scope.launch {
                start()
            }
            useCount = 0
            currentAccount = account
            currentWallet = nearWallet
        }

        useCount++
        return this.kitWrapper!!
    }

    private fun createKitInstance(nearWallet: NearWallet, account: Account): NearKitWrapper {
        val kit = NearKit.getInstance(
            App.instance,
            nearWallet,
            Network.MainNet,
            account.id,
            rpcUrls = rpcSourceManager.rpcUrls(),
            fastNearApiKey = App.appConfigProvider.fastNearApiKey,
        )
        return NearKitWrapper(kit)
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

    @Synchronized
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
        currentWallet = null
    }

    private fun start() {
        kitWrapper?.kit?.start()
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

    fun getAddress(account: Account): String =
        NearKit.accountId(nearWallet(account))

    private fun nearWallet(account: Account): NearWallet {
        val accountType = account.type
        if (accountType !is AccountType.Mnemonic && accountType !is AccountType.NearAddress) {
            throw UnsupportedAccountException()
        }
        val nearAccountId = restoreSettingsManager.settings(account, BlockchainType.Near).nearAccountId
        return accountType.toNearWallet(nearAccountId)
    }

    companion object {
        private const val TAG = "NearKitManager"
    }
}

class NearKitWrapper(val kit: NearKit) {
    /** Token contracts of the enabled NEP-141 wallets; their balances are read from the contracts every sync. */
    private val watchedTokens = mutableSetOf<String>()

    @Synchronized
    fun watchToken(contractId: String) {
        if (watchedTokens.add(contractId)) kit.watchTokens(watchedTokens.toSet())
    }

    @Synchronized
    fun unwatchToken(contractId: String) {
        if (watchedTokens.remove(contractId)) kit.watchTokens(watchedTokens.toSet())
    }
}

fun NearKit.SyncState.toAdapterState(): AdapterState = when (this) {
    is NearKit.SyncState.NotSynced -> AdapterState.NotSynced(error)
    is NearKit.SyncState.Synced -> AdapterState.Synced
    is NearKit.SyncState.Syncing -> AdapterState.Syncing()
}

/**
 * Mnemonic accounts use the key at m/44'/397'/0', as MyNearWallet and Trust Wallet do, on [nearAccountId]
 * when the user chose a named account it controls, else on the key's implicit account.
 */
fun AccountType.toNearWallet(nearAccountId: String? = null): NearWallet = when (this) {
    is AccountType.Mnemonic -> NearWallet.Seed(seed, nearAccountId)
    is AccountType.NearAddress -> NearWallet.WatchOnly(address)
    else -> throw IllegalArgumentException("Account type ${this.javaClass.simpleName} can not be converted to NearWallet")
}
