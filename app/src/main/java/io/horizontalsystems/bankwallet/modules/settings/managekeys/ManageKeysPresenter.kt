package io.horizontalsystems.bankwallet.modules.settings.managekeys

import io.horizontalsystems.bankwallet.core.Account

class ManageKeysPresenter(private val interactor: ManageKeysModule.Interactor)
    : ManageKeysModule.ViewDelegate, ManageKeysModule.InteractorDelegate {

    var view: ManageKeysModule.View? = null

    //  ViewDelegate

    override var items = listOf<Account>()

    override fun viewDidLoad() {
        interactor.loadAccounts()
    }

    override fun unlinkAccount(id: String) {
        interactor.deleteAccount(id)
    }

    override fun onClear() {
        interactor.clear()
    }

    //  InteractorDelegate

    override fun didLoad(accounts: List<Account>) {
        items = accounts
        view?.show(items)
    }
}
