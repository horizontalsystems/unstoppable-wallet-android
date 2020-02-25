package io.horizontalsystems.bankwallet.modules.restore

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IPredefinedAccountTypeManager
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType

class RestorePresenter(
        val view: RestoreModule.IView,
        val router: RestoreModule.IRouter,
        private val predefinedAccountTypeManager: IPredefinedAccountTypeManager)
    : ViewModel(), RestoreModule.ViewDelegate {

    private var accountType: AccountType? = null
    private var predefinedAccountType: PredefinedAccountType? = null

    override var items = listOf<PredefinedAccountType>()

    override fun onLoad() {
        items = predefinedAccountTypeManager.allTypes
        view.reload(items)
    }

    override fun onSelect(predefinedAccountType: PredefinedAccountType) {
        this.predefinedAccountType = predefinedAccountType
        router.showKeyInput(predefinedAccountType)
    }

    override fun didEnterValidAccount(accountType: AccountType) {
        this.accountType = accountType
        if (predefinedAccountType == PredefinedAccountType.Standard) {
            router.showCoinSettings()
        } else {
            predefinedAccountType?.let { router.showRestoreCoins(it, accountType) }
        }
    }

    override fun didReturnFromCoinSettings() {
        predefinedAccountType?.let {
            accountType?.let { accountType ->
                router.showRestoreCoins(it, accountType)
            }
        }
    }

    override fun onClickClose() {
        router.close()
    }

}
