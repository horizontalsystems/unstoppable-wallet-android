package io.horizontalsystems.bankwallet.modules.restore

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IPredefinedAccountTypeManager
import io.horizontalsystems.bankwallet.entities.*

class RestorePresenter(
        val view: RestoreModule.IView,
        val router: RestoreModule.IRouter,
        private val interactor: RestoreModule.IInteractor,
        private val predefinedAccountTypeManager: IPredefinedAccountTypeManager,
        private var predefinedAccountType: PredefinedAccountType?,
        private val restoreMode: RestoreMode)
    : ViewModel(), RestoreModule.ViewDelegate {

    private var accountType: AccountType? = null
    override var items = listOf<PredefinedAccountType>()

    override fun onLoad() {
        predefinedAccountType?.let {
            router.showKeyInput(it)
            return
        }

        items = predefinedAccountTypeManager.allTypes
        view.reload(items)
    }

    override fun onSelect(predefinedAccountType: PredefinedAccountType) {
        this.predefinedAccountType = predefinedAccountType
        router.showKeyInput(predefinedAccountType)
    }

    override fun didEnterValidAccount(accountType: AccountType) {
        this.accountType = accountType
        predefinedAccountType?.let {
            if (restoreMode == RestoreMode.InApp) {
                restoreAccount()
                closeWithSuccess()
            } else {
                router.showRestoreCoins(it)
            }
        }
    }

    override fun didReturnFromCoinSettings() {
        predefinedAccountType?.let {
            router.showRestoreCoins(it)
        }
    }

    override fun didReturnFromRestoreCoins(enabledCoins: List<Coin>?) {
        enabledCoins?.let { createWallets(it) }
    }

    override fun onClickClose() {
        router.close()
    }

    override fun onReturnWithCancel() {
        if (restoreMode == RestoreMode.FromManageKeys || restoreMode == RestoreMode.InApp) {
            router.close()
        }
    }

    private fun createWallets(enabledCoins: List<Coin>) {
        val account = restoreAccount() ?: return

        val wallets = enabledCoins.map {
            Wallet(it, account)
        }

        wallets.forEach {
            interactor.initializeSettings(it.coin.type)
        }

        interactor.saveWallets(wallets)

        closeWithSuccess()
    }

    private fun restoreAccount(): Account? {
        val accountType = accountType ?: return null
        val account = interactor.account(accountType)
        interactor.create(account)
        return account
    }

    private fun closeWithSuccess() {
        when (restoreMode) {
            RestoreMode.FromWelcome -> router.startMainModule()
            else -> router.closeWithSuccess()
        }
    }

}
