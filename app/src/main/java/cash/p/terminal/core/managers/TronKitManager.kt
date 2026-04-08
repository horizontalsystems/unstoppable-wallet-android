package cash.p.terminal.core.managers

import android.os.Handler
import android.os.Looper
import cash.p.terminal.core.App
import cash.p.terminal.core.onPollingStarted
import cash.p.terminal.core.onPollingStopped
import cash.p.terminal.core.UnsupportedAccountException
import cash.p.terminal.core.UnsupportedException
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.core.storage.HardwarePublicKeyStorage
import cash.p.terminal.core.utils.TronAddressParser
import cash.p.terminal.tangem.signer.HardwareWalletTronSigner
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountType
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.tronkit.TronKit
import io.horizontalsystems.tronkit.models.Address
import io.horizontalsystems.tronkit.network.Network
import io.horizontalsystems.tronkit.transaction.Signer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

class TronKitManager(
    private val backgroundManager: BackgroundManager,
    private val hardwarePublicKeyStorage: HardwarePublicKeyStorage,
    private val backgroundKeepAliveManager: BackgroundKeepAliveManager,
) {
    private val lifecycleMutex = Mutex()
    private val pollingSessionCount = AtomicInteger(0)
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null
    private val network = Network.Mainnet
    private val _kitStartedFlow = MutableStateFlow(false)
    val kitStartedFlow: StateFlow<Boolean> = _kitStartedFlow

    var tronKitWrapper: TronKitWrapper? = null
        private set(value) {
            field = value

            _kitStartedFlow.update { value != null }
        }

    private var useCount = 0
    var currentAccount: Account? = null
        private set

    val statusInfo: Map<String, Any>?
        get() = tronKitWrapper?.tronKit?.statusInfo()

    suspend fun getTronKitWrapper(account: Account): TronKitWrapper = lifecycleMutex.withLock {
        if (this.tronKitWrapper != null && currentAccount != account) {
            stop()
        }

        if (this.tronKitWrapper == null) {
            val accountType = account.type
            this.tronKitWrapper = when (accountType) {
                is AccountType.Mnemonic -> {
                    createKitInstance(accountType, account)
                }

                is AccountType.TronAddress -> {
                    createKitInstance(accountType, account)
                }

                is AccountType.HardwareCard ->
                    createKitInstance(account)

                else -> throw UnsupportedAccountException()
            }
            start()
            useCount = 0
            currentAccount = account
        }

        useCount++
        requireNotNull(this.tronKitWrapper)
    }

    private fun createKitInstance(
        accountType: AccountType.Mnemonic,
        account: Account
    ): TronKitWrapper {
        val seed = accountType.seed
        val signer = Signer.getInstance(seed, network)

        val kit = TronKit.getInstance(
            application = App.instance,
            walletId = account.id,
            seed = seed,
            network = network,
            tronGridApiKeys = AppConfigProvider.trongridApiKeys
        )

        return TronKitWrapper(kit, signer)
    }

    private fun createKitInstance(
        accountType: AccountType.TronAddress,
        account: Account
    ): TronKitWrapper {
        val address = accountType.address

        val kit = TronKit.getInstance(
            application = App.instance,
            address = Address.fromBase58(address),
            network = network,
            walletId = account.id,
            tronGridApiKeys = AppConfigProvider.trongridApiKeys
        )

        return TronKitWrapper(kit, null)
    }

    private fun createKitInstance(
        account: Account
    ): TronKitWrapper {
        val hardwarePublicKey = runBlocking {
            hardwarePublicKeyStorage.getKeyByBlockchain(account.id, BlockchainType.Tron)
        } ?: throw UnsupportedException("Hardware card does not have a public key for Tron")

        val addressAndPublicKey = TronAddressParser.parseXpubToTronAddress(hardwarePublicKey.key.value)
        val signer = HardwareWalletTronSigner(
            hardwarePublicKey = hardwarePublicKey,
            expectedPublicKeyBytes = addressAndPublicKey.publicKey
        )

        val kit = TronKit.getInstance(
            application = App.instance,
            address = addressAndPublicKey.address,
            network = network,
            walletId = account.id,
            tronGridApiKeys = AppConfigProvider.trongridApiKeys
        )

        return TronKitWrapper(kit, signer)
    }

    suspend fun unlink(account: Account) = lifecycleMutex.withLock {
        if (account == currentAccount) {
            useCount -= 1

            if (useCount < 1) {
                stop()
            }
        }
    }

    suspend fun startForPolling() = lifecycleMutex.withLock {
        pollingSessionCount.onPollingStarted {
            tronKitWrapper?.tronKit?.let { kit ->
                kit.resume()
                kit.refresh()
            }
        }
    }

    suspend fun stopForPolling() = lifecycleMutex.withLock {
        pollingSessionCount.onPollingStopped(backgroundManager) {
            if (!backgroundKeepAliveManager.isKeepAlive(BlockchainType.Tron)) {
                tronKitWrapper?.tronKit?.pause()
            }
        }
    }

    private fun stop() {
        tronKitWrapper?.tronKit?.stop()
        job?.cancel()
        tronKitWrapper = null
        currentAccount = null
    }

    private fun start() {
        tronKitWrapper?.tronKit?.start()
        tronKitWrapper?.tronKit?.refresh()
        job = scope.launch {
            backgroundManager.stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterForeground) {
                    tronKitWrapper?.tronKit?.let { kit ->
                        kit.resume()
                        Handler(Looper.getMainLooper()).postDelayed({
                            kit.refresh()
                        }, 1000)
                    }
                } else if (state == BackgroundManagerState.EnterBackground) {
                    if (pollingSessionCount.get() == 0 && !backgroundKeepAliveManager.isKeepAlive(BlockchainType.Tron)) {
                        tronKitWrapper?.tronKit?.pause()
                    } else {
                        Timber.tag("TxPoller").d("TronKit staying alive")
                    }
                }
            }
        }
    }
}

class TronKitWrapper(val tronKit: TronKit, val signer: Signer?)
