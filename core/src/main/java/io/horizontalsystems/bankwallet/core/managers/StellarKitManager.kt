package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BackgroundManager
import io.horizontalsystems.bankwallet.core.BackgroundManagerState
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.stellarkit.Network
import io.horizontalsystems.stellarkit.StellarKit
import io.horizontalsystems.stellarkit.StellarWallet
import io.horizontalsystems.stellarkit.SyncState
import io.horizontalsystems.stellarkit.room.StellarAsset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StellarKitManager(
    private val backgroundManager: BackgroundManager,
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null
    private val _kitStartedFlow = MutableStateFlow(false)
    val kitStartedFlow: StateFlow<Boolean> = _kitStartedFlow

    var stellarKitWrapper: StellarKitWrapper? = null
        private set(value) {
            field = value

            _kitStartedFlow.update { value != null }
        }

    private var useCount = 0
    var currentAccount: Account? = null
        private set

    val statusInfo: Map<String, Any>?
        get() = stellarKitWrapper?.stellarKit?.statusInfo()

    @Synchronized
    fun getStellarKitWrapper(account: Account): StellarKitWrapper {
        if (this.stellarKitWrapper != null && currentAccount != account) {
            stop()
        }

        if (this.stellarKitWrapper == null) {
            val accountType = account.type
            this.stellarKitWrapper = when (accountType) {
                is AccountType.Mnemonic,
                is AccountType.StellarAddress,
                is AccountType.StellarSecretKey -> {
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
        return this.stellarKitWrapper!!
    }

    private fun createKitInstance(accountType: AccountType, account: Account): StellarKitWrapper {
        val kit = StellarKit.getInstance(accountType.toStellarWallet(), Network.MainNet, App.instance, account.id)

        return StellarKitWrapper(kit)
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
        stellarKitWrapper?.stellarKit?.stop()
        job?.cancel()
        stellarKitWrapper = null
        currentAccount = null
    }

    private suspend fun start() {
        stellarKitWrapper?.stellarKit?.start()
        job = scope.launch {
            backgroundManager.stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterForeground) {
                    stellarKitWrapper?.stellarKit?.let { kit ->
                        delay(1000)
                        kit.refresh()
                    }
                }
            }
        }
    }

    fun getAddress(accountType: AccountType): String {
        return StellarKit.getAccountId(accountType.toStellarWallet())
    }
}

class StellarKitWrapper(val stellarKit: StellarKit)

fun StellarKit.statusInfo(): Map<String, Any> =
    buildMap {
        put("Sync State", syncStateFlow.value.toAdapterState())
        put("Operation Sync State", operationsSyncStateFlow.value.toAdapterState())
    }

val StellarAsset.Asset.tokenType
    get() = TokenType.Asset(code, issuer)

fun SyncState.toAdapterState(): AdapterState = when (this) {
    is SyncState.NotSynced -> AdapterState.NotSynced(error)
    is SyncState.Synced -> AdapterState.Synced
    is SyncState.Syncing -> AdapterState.Syncing()
}

fun AccountType.toStellarWallet() = when (this) {
    is AccountType.Mnemonic -> StellarWallet.Seed(seed)
    is AccountType.StellarAddress -> StellarWallet.WatchOnly(address)
    is AccountType.StellarSecretKey -> StellarWallet.SecretKey(key)
    else -> throw IllegalArgumentException("Account type ${this.javaClass.simpleName} can not be converted to StellarWallet.Wallet")
}
