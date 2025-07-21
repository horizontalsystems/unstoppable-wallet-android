package cash.p.terminal.core.managers

import cash.p.terminal.core.App
import cash.p.terminal.core.UnsupportedAccountException
import cash.p.terminal.core.UnsupportedException
import cash.p.terminal.core.storage.HardwarePublicKeyStorage
import cash.p.terminal.tangem.signer.HardwareWalletStellarSigner
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.horizontalsystems.core.entities.BlockchainType
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
import kotlinx.coroutines.runBlocking

class StellarKitManager(
    private val backgroundManager: BackgroundManager,
    private val hardwarePublicKeyStorage: HardwarePublicKeyStorage
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
                is AccountType.HardwareCard,
                is AccountType.StellarSecretKey -> {
                    createKitInstance(accountType, account)
                }

                is AccountType.BitcoinAddress,
                is AccountType.Cex,
                is AccountType.EvmAddress,
                is AccountType.EvmPrivateKey,
                is AccountType.HdExtendedKey,
                is AccountType.MnemonicMonero,
                is AccountType.SolanaAddress,
                is AccountType.TonAddress,
                is AccountType.TronAddress,
                is AccountType.ZCashUfvKey -> throw UnsupportedAccountException()
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
        val kit = if(accountType is AccountType.HardwareCard) {
            val hardwarePublicKey = runBlocking {
                hardwarePublicKeyStorage.getKey(account.id, BlockchainType.Stellar, TokenType.Native)
            } ?: throw UnsupportedException("Hardware card does not have a public key for Stellar")

            val stellarWallet = HardwareWalletStellarSigner(
                hardwarePublicKey = hardwarePublicKey
            )
            StellarKit.getInstance(stellarWallet, Network.MainNet, App.instance, account.id)
        } else {
            StellarKit.getInstance(accountType.toStellarWallet(), Network.MainNet, App.instance, account.id)
        }

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