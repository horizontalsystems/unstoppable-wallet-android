package io.horizontalsystems.walletkit.core.managers

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ThorchainKitManager(
    private val backgroundManager: BackgroundManager,
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null
    private val _kitStartedFlow = MutableStateFlow(false)
    val kitStartedFlow: StateFlow<Boolean> = _kitStartedFlow

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
            scope.launch {
                start()
            }
            useCount = 0
            currentAccount = account
        }

        useCount++
        return this.thorchainKitWrapper!!
    }

    private fun createKitInstance(accountType: AccountType.Mnemonic, account: Account): ThorchainKitWrapper {
        val kit = ThorchainKit.getInstance(App.instance, accountType.seed, Network.Mainnet, account.id)

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

    private fun stop() {
        thorchainKitWrapper?.thorchainKit?.stop()
        job?.cancel()
        thorchainKitWrapper = null
        currentAccount = null
    }

    private suspend fun start() {
        thorchainKitWrapper?.thorchainKit?.start()
        job = scope.launch {
            backgroundManager.stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterForeground) {
                    thorchainKitWrapper?.thorchainKit?.let { kit ->
                        delay(1000)
                        kit.refresh()
                    }
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
