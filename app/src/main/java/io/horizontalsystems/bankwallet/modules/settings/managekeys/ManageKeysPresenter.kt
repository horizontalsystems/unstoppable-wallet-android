package io.horizontalsystems.bankwallet.modules.settings.managekeys

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType

class ManageKeysPresenter(
         val view: ManageKeysModule.IView,
         val router: ManageKeysModule.IRouter,
        private val interactor: ManageKeysModule.Interactor)
    : ViewModel(), ManageKeysModule.ViewDelegate, ManageKeysModule.InteractorDelegate {

    private var currentItemForUnlink: ManageAccountItem? = null

    private var predefinedAccountType: PredefinedAccountType? = null

    var items = listOf<ManageAccountItem>()

    override fun onLoad() {
        interactor.loadAccounts()
    }

    override fun onClickCreate(accountItem: ManageAccountItem) {
        router.showCreateWallet(accountItem.predefinedAccountType)
    }

    override fun onClickBackup(accountItem: ManageAccountItem) {
        val account = accountItem.account ?: return
        router.showBackup(account, accountItem.predefinedAccountType)
    }

    override fun onClickRestore(accountItem: ManageAccountItem) {
        predefinedAccountType = accountItem.predefinedAccountType
        router.showRestore(accountItem.predefinedAccountType)
    }

    override fun onClickUnlink(accountItem: ManageAccountItem) {
        currentItemForUnlink = accountItem

        if (accountItem.account?.isBackedUp == true) {
            view.showUnlinkConfirmation(accountItem)
        } else {
            view.showBackupConfirmation(accountItem)
        }
    }

    override fun onClickAddressFormat(item: ManageAccountItem) {
        router.showAddressFormat()
    }

    override fun onConfirmBackup() {
        currentItemForUnlink?.let {
            val account = it.account ?: return
            router.showBackup(account, it.predefinedAccountType)
        }
    }

    override fun onConfirmUnlink(account: Account) {
        interactor.deleteAccount(account)
    }

    override fun onCleared() {
        interactor.clear()
    }

    //  IInteractorDelegate

    override fun didLoad(accounts: List<ManageAccountItem>) {
        items = accounts
        view.show(items)
    }

}
