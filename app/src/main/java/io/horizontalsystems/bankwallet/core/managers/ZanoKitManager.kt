package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BackgroundManager
import io.horizontalsystems.bankwallet.core.BackgroundManagerState
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.zanokit.ZanoKit
import io.horizontalsystems.zanokit.ZanoWallet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.text.split

class ZanoKitManager(
    private val zanoNodeManager: ZanoNodeManager,
    private val backgroundManager: BackgroundManager,
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _kitStartedFlow = MutableStateFlow(false)
    val kitStartedFlow: StateFlow<Boolean> = _kitStartedFlow

    var zanoKitWrapper: ZanoKitWrapper? = null
        private set(value) {
            field = value
            _kitStartedFlow.update { value != null }
        }

    private var useCount = 0
    var currentAccount: Account? = null
        private set

    init {
        scope.launch {
            zanoNodeManager.currentNodeUpdatedFlow.collect {
                handleNodeUpdate()
            }
        }
        scope.launch {
            backgroundManager.stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterBackground) {
                    zanoKitWrapper?.kit?.store()
                }
            }
        }
    }

    private fun handleNodeUpdate() {
        if (zanoKitWrapper == null) return
        stop()
    }

    @Synchronized
    fun getZanoKitWrapper(account: Account, creationTimestamp: Long): ZanoKitWrapper {
        if (this.zanoKitWrapper != null && currentAccount != account) {
            stop()
        }

        if (this.zanoKitWrapper == null) {
            val accountType = account.type
            this.zanoKitWrapper = when (accountType) {
                is AccountType.Mnemonic -> createKitInstance(accountType, account, creationTimestamp)
                else -> throw UnsupportedAccountException()
            }
            this.zanoKitWrapper!!.kit.start()
            useCount = 0
            currentAccount = account
        }

        useCount++
        return this.zanoKitWrapper!!
    }

    private fun createKitInstance(accountType: AccountType.Mnemonic, account: Account, creationTimestamp: Long): ZanoKitWrapper {
        val node = zanoNodeManager.currentNode
        val wallet = ZanoWallet.Bip39(
            mnemonic = "top post mercy height badge hazard airport clump velvet category essay actor".split(" "),
            passphrase = accountType.passphrase,
            creationTimestamp = creationTimestamp,
        )
        val kit = ZanoKit.getInstance(
            context = App.instance,
            wallet = wallet,
            walletId = account.id,
            daemonAddress = node.host,
        )
        return ZanoKitWrapper(kit)
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
        val wrapper = zanoKitWrapper
        zanoKitWrapper = null
        currentAccount = null
        wrapper?.kit?.stop()
    }
}

class ZanoKitWrapper(val kit: ZanoKit)

val ZanoKitManager.statusInfo: Map<String, Any>?
    get() = zanoKitWrapper?.kit?.statusInfo()
