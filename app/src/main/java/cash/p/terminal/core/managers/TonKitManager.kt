package cash.p.terminal.core.managers

import android.os.Handler
import android.os.Looper
import cash.p.terminal.core.App
import cash.p.terminal.core.UnsupportedAccountException
import cash.p.terminal.entities.Account
import cash.p.terminal.entities.AccountType
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.horizontalsystems.hdwalletkit.Curve
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.tonkit.core.TonKit
import io.horizontalsystems.tonkit.models.Network
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TonKitManager(
    private val backgroundManager: BackgroundManager
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null
    private val _kitStartedFlow = MutableStateFlow(false)
    val kitStartedFlow: StateFlow<Boolean> = _kitStartedFlow

    var tonKitWrapper: TonKitWrapper? = null
        private set(value) {
            field = value

            _kitStartedFlow.update { value != null }
        }

    private var useCount = 0
    var currentAccount: Account? = null
        private set

    val statusInfo: Map<String, Any>?
        get() = tonKitWrapper?.tonKit?.statusInfo()

    @Synchronized
    fun getTonKitWrapper(account: Account): TonKitWrapper {
        if (this.tonKitWrapper != null && currentAccount != account) {
            stop()
        }

        if (this.tonKitWrapper == null) {
            val accountType = account.type
            this.tonKitWrapper = when (accountType) {
                is AccountType.Mnemonic -> {
                    createKitInstance(accountType, account)
                }

                is AccountType.TonAddress -> {
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
        return this.tonKitWrapper!!
    }

    private fun createKitInstance(
        accountType: AccountType.Mnemonic,
        account: Account
    ): TonKitWrapper {
        val hdWallet = HDWallet(accountType.seed, 607, HDWallet.Purpose.BIP44, Curve.Ed25519)
        val privateKey = hdWallet.privateKey(0)
        var privateKeyBytes = privateKey.privKeyBytes
        if (privateKeyBytes.size > 32) {
            privateKeyBytes = privateKeyBytes.copyOfRange(1, privateKeyBytes.size)
        }
        val walletType = TonKit.WalletType.Seed(privateKeyBytes)

        val kit = TonKit.getInstance(walletType, Network.MainNet, App.instance, account.id)

        return TonKitWrapper(kit)
    }

    private fun createKitInstance(
        accountType: AccountType.TonAddress,
        account: Account
    ): TonKitWrapper {
        val walletType = TonKit.WalletType.Watch(accountType.address)
        val kit = TonKit.getInstance(walletType, Network.MainNet, App.instance, account.id)

        return TonKitWrapper(kit)
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
        tonKitWrapper?.tonKit?.stop()
        job?.cancel()
        tonKitWrapper = null
        currentAccount = null
    }

    private suspend fun start() {
        tonKitWrapper?.tonKit?.start()
        job = scope.launch {
            backgroundManager.stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterForeground) {
                    tonKitWrapper?.tonKit?.let { kit ->
                        Handler(Looper.getMainLooper()).postDelayed({
                            kit.refresh()
                        }, 1000)
                    }
                }
            }
        }
    }
}

class TonKitWrapper(val tonKit: TonKit)

fun TonKit.refresh() {
}

private suspend fun TonKit.start() {
    this.sync()
}

private fun TonKit.stop() {
}

val TonKit.network: Network
    get() = Network.MainNet

fun TonKit.statusInfo(): Map<String, Any> {
    return buildMap {
//        put("Balance Sync State", balanceState)
//            put("Transaction Sync State", transactionsState)
    }
}
