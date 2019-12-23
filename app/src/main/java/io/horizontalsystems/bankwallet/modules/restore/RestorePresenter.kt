package io.horizontalsystems.bankwallet.modules.restore

import io.horizontalsystems.bankwallet.core.IPredefinedAccountTypeManager
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType

class RestorePresenter(
        private val router: RestoreModule.Router,
        private val predefinedAccountTypeManager: IPredefinedAccountTypeManager)
    : RestoreModule.ViewDelegate {

    var view: RestoreModule.View? = null

    //  View Delegate

    override var items = listOf<PredefinedAccountType>()

    override fun viewDidLoad() {
        items = predefinedAccountTypeManager.allTypes
        view?.reload(items)
    }

    override fun onSelect(predefinedAccountType: PredefinedAccountType) {
        router.startRestoreCoins(predefinedAccountType)
    }

    override fun onClickClose() {
        router.close()
    }

    override fun onRestore() {
        router.close()
    }

}
