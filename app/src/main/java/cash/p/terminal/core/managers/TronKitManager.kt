package cash.p.terminal.core.managers

import android.os.Handler
import android.os.Looper
import cash.p.terminal.core.App
import cash.p.terminal.core.UnsupportedAccountException
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountType
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
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

class TronKitManager(
    private val appConfigProvider: AppConfigProvider,
    private val backgroundManager: BackgroundManager
) {
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
    var currentAccount: cash.p.terminal.wallet.Account? = null
        private set

    val statusInfo: Map<String, Any>?
        get() = tronKitWrapper?.tronKit?.statusInfo()

    @Synchronized
    fun getTronKitWrapper(account: cash.p.terminal.wallet.Account): TronKitWrapper {
        if (this.tronKitWrapper != null && currentAccount != account) {
            stop()
        }

        if (this.tronKitWrapper == null) {
            val accountType = account.type
            this.tronKitWrapper = when (accountType) {
                is cash.p.terminal.wallet.AccountType.Mnemonic -> {
                    createKitInstance(accountType, account)
                }

                is cash.p.terminal.wallet.AccountType.TronAddress -> {
                    createKitInstance(accountType, account)
                }

                else -> throw UnsupportedAccountException()
            }
            start()
            useCount = 0
            currentAccount = account
        }

        useCount++
        return this.tronKitWrapper!!
    }

    private fun createKitInstance(
        accountType: cash.p.terminal.wallet.AccountType.Mnemonic,
        account: cash.p.terminal.wallet.Account
    ): TronKitWrapper {
        val seed = accountType.seed
        val signer = Signer.getInstance(seed, network)

        val kit = TronKit.getInstance(
            application = App.instance,
            walletId = account.id,
            seed = seed,
            network = network,
            tronGridApiKeys = appConfigProvider.trongridApiKeys
        )

        return TronKitWrapper(kit, signer)
    }

    private fun createKitInstance(
        accountType: cash.p.terminal.wallet.AccountType.TronAddress,
        account: cash.p.terminal.wallet.Account
    ): TronKitWrapper {
        val address = accountType.address

        val kit = TronKit.getInstance(
            application = App.instance,
            address = Address.fromBase58(address),
            network = network,
            walletId = account.id,
            tronGridApiKeys = appConfigProvider.trongridApiKeys
        )

        return TronKitWrapper(kit, null)
    }

    @Synchronized
    fun unlink(account: cash.p.terminal.wallet.Account) {
        if (account == currentAccount) {
            useCount -= 1

            if (useCount < 1) {
                stop()
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
        job = scope.launch {
            backgroundManager.stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterForeground) {
                    tronKitWrapper?.tronKit?.let { kit ->
                        Handler(Looper.getMainLooper()).postDelayed({
                            kit.refresh()
                        }, 1000)
                    }
                }
            }
        }
    }
}

class TronKitWrapper(val tronKit: TronKit, val signer: Signer?)
