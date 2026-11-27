package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.core.IWalletStorage
import io.horizontalsystems.walletkit.core.collectSafely
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.EnabledWallet
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import kotlinx.coroutines.flow.catch
import timber.log.Timber
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class WalletManager(
    private val accountManager: IAccountManager,
    private val storage: IWalletStorage,
) {

    val activeWallets: List<Wallet>
        @Synchronized get() = walletsSet.toList()

    // SharedFlow, not StateFlow: equal lists are re-emitted on purpose as a
    // "rebuild adapters" trigger (see refreshActiveWallets/reloadWallets);
    // StateFlow's equality dedup would swallow them. Every emission must reach
    // the collectors: reloadWallets emits the list without a chain's wallets and
    // then the full list back to back, and AdapterManager only rebuilds those
    // adapters when it sees the first one. With room for a single value the
    // first was dropped and the adapters stayed on the kit just stopped.
    // DROP_OLDEST keeps tryEmit non-blocking on the main thread; 64 values
    // never overflow in practice.
    private val _activeWalletsUpdatedFlow = MutableSharedFlow<List<Wallet>>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val activeWalletsUpdatedFlow = _activeWalletsUpdatedFlow.asSharedFlow()

    private val walletsSet = mutableSetOf<Wallet>()
    private val loaded = CompletableDeferred<Unit>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        coroutineScope.launch {
            accountManager.activeAccountStateFlow.collect { activeAccountState ->
                if (activeAccountState is ActiveAccountState.ActiveAccount) {
                    handleUpdated(activeAccountState.account)
                }
            }
        }
    }

    fun save(wallets: List<Wallet>) {
        handle(wallets, listOf())
    }

    fun delete(wallets: List<Wallet>) {
        handle(listOf(), wallets)
    }

    @Synchronized
    fun handle(newWallets: List<Wallet>, deletedWallets: List<Wallet>) {
        storage.save(newWallets)
        storage.delete(deletedWallets)

        val activeAccount = accountManager.activeAccount
        walletsSet.addAll(newWallets.filter { it.account == activeAccount })
        walletsSet.removeAll(deletedWallets)
        notifyActiveWallets()
    }

    @Synchronized
    fun clear() {
        storage.clear()
        walletsSet.clear()
        notifyActiveWallets()
        coroutineScope.cancel()
    }

    private fun notifyActiveWallets() {
        _activeWalletsUpdatedFlow.tryEmit(walletsSet.toList())
    }

    // Re-emit the current active wallets to (re)initialize adapters without tearing existing ones
    // down. Used after Monero startup Auto-Select resolves the fastest node so the Monero adapter
    // is created once (existing adapters are reused, none are deleted).
    @Synchronized
    fun refreshActiveWallets() {
        notifyActiveWallets()
    }

    @Synchronized
    private fun handleUpdated(activeAccount: Account?) {
        val activeWallets = activeAccount?.let { storage.wallets(it) } ?: listOf()

        walletsSet.clear()
        walletsSet.addAll(activeWallets)
        loaded.complete(Unit)
        notifyActiveWallets()
    }

    /**
     * Suspends until the active account's wallets have been read for the first time. Before
     * that [activeWallets] is empty, which at app start says nothing about what is in use.
     */
    suspend fun awaitLoaded() = loaded.await()

    fun saveEnabledWallets(enabledWallets: List<EnabledWallet>) {
        storage.handle(enabledWallets)
        handleUpdated(accountManager.activeAccount)
    }

    fun start(
        restoreSettingsManager: RestoreSettingsManager,
        btcBlockchainManager: BtcBlockchainManager,
        evmBlockchainManager: EvmBlockchainManager,
    ) {
        coroutineScope.launch {
            restoreSettingsManager.settingsUpdatedFlow.collect { blockchainType ->
                reloadWallets(blockchainType)
            }
        }
        ChainRegistry.all.forEach { plugin ->
            plugin.walletReloadTrigger?.let { trigger ->
                coroutineScope.launch {
                    trigger
                        .catch { Timber.e(it, "Chain plugin %s trigger failed", plugin.blockchainType.uid) }
                        .collect {
                            try {
                                reloadWallets(plugin.blockchainType)
                            } catch (e: Throwable) {
                                Timber.e(e, "Reloading %s wallets failed", plugin.blockchainType.uid)
                            }
                        }
                }
            }
        }
        coroutineScope.launch {
            btcBlockchainManager.restoreModeUpdatedFlow.collectSafely { blockchainType ->
                reloadWallets(blockchainType)
            }
        }
    }

    @Synchronized
    private fun reloadWallets(blockchainType: BlockchainType) {
        val walletsToReAdd = walletsSet.filter { it.token.blockchainType == blockchainType }
        if (walletsToReAdd.isEmpty()) return

        delete(walletsToReAdd)
        save(walletsToReAdd)
    }

}
