package io.horizontalsystems.bankwallet.core.managers

import android.os.Handler
import android.os.Looper
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.tronkit.TronKit
import io.horizontalsystems.tronkit.models.Address
import io.horizontalsystems.tronkit.network.Network
import io.horizontalsystems.tronkit.transaction.Signer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class TronKitManager(
    private val appConfigProvider: AppConfigProvider,
    backgroundManager: BackgroundManager
) : BackgroundManager.Listener {
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

    init {
        backgroundManager.registerListener(this)
    }

    @Synchronized
    fun getTronKitWrapper(account: Account): TronKitWrapper {
        if (this.tronKitWrapper != null && currentAccount != account) {
            stopKit()
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

                else -> throw UnsupportedAccountException()
            }
            startKit()
            useCount = 0
            currentAccount = account
        }

        useCount++
        return this.tronKitWrapper!!
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
            words = accountType.words,
            network = network,
            tronGridApiKey = appConfigProvider.trongridApiKey
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
            tronGridApiKey = appConfigProvider.trongridApiKey
        )

        return TronKitWrapper(kit, null)
    }

    @Synchronized
    fun unlink(account: Account) {
        if (account == currentAccount) {
            useCount -= 1

            if (useCount < 1) {
                stopKit()
            }
        }
    }

    private fun stopKit() {
        tronKitWrapper?.tronKit?.stop()
        tronKitWrapper = null
        currentAccount = null
    }

    private fun startKit() {
        tronKitWrapper?.tronKit?.start()
    }

    //
    // BackgroundManager.Listener
    //

    override fun willEnterForeground() {
        this.tronKitWrapper?.tronKit?.let { kit ->
            Handler(Looper.getMainLooper()).postDelayed({
                kit.refresh()
            }, 1000)
        }
    }

    override fun didEnterBackground() = Unit
}

class TronKitWrapper(val tronKit: TronKit, val signer: Signer?)
