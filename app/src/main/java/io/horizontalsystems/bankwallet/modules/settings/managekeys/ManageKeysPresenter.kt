package io.horizontalsystems.bankwallet.modules.settings.managekeys

import io.horizontalsystems.bankwallet.core.IPredefinedAccountType
import io.horizontalsystems.bankwallet.entities.*

class ManageKeysPresenter(private val interactor: ManageKeysModule.Interactor, private val router: ManageKeysModule.Router)
    : ManageKeysModule.ViewDelegate, ManageKeysModule.InteractorDelegate {

    var view: ManageKeysModule.View? = null

    private var currentItem: ManageAccountItem? = null

    //  IViewDelegate

    override var items = listOf<ManageAccountItem>()

    override fun viewDidLoad() {
        interactor.loadAccounts()
    }

    override fun onClickNew(accountItem: ManageAccountItem) {
        currentItem = accountItem
        view?.showCreateConfirmation(accountItem)
    }

    override fun onClickBackup(accountItem: ManageAccountItem) {
        router.startBackupModule(accountItem)
    }

    override fun onClickRestore(accountType: IPredefinedAccountType) {
        when (accountType) {
            is UnstoppableAccountType -> {
                router.startRestoreWords(12, accountType.title)
            }
            is BinanceAccountType -> {
                router.startRestoreWords(24, accountType.title)
            }
            is EosAccountType -> {
                router.startRestoreEos(accountType.title)
            }
        }
    }

    override fun onClickUnlink(accountItem: ManageAccountItem) {
        currentItem = accountItem

        if (accountItem.account?.isBackedUp == true) {
            view?.showUnlinkConfirmation(accountItem)
        } else {
            view?.showBackupConfirmation(accountItem)
        }
    }

    override fun onClickShow(accountItem: ManageAccountItem) {
        router.startBackupModule(accountItem)
    }

    override fun onConfirmCreate() {
        try {
            currentItem?.let { interactor.createAccount(it.predefinedAccountType) }
            view?.showSuccess()
        } catch (e: Exception) {
            view?.showError(e)
        }
    }

    override fun onConfirmBackup() {
        currentItem?.let { router.startBackupModule(it) }
    }

    override fun onConfirmUnlink(accountId: String) {
        interactor.deleteAccount(accountId)
    }

    override fun onConfirmRestore(accountType: AccountType, syncMode: SyncMode?) {
        interactor.restoreAccount(accountType, syncMode)
    }

    override fun onClear() {
        interactor.clear()
    }

    //  IInteractorDelegate

    override fun didLoad(accounts: List<ManageAccountItem>) {
        items = accounts
        view?.show(items)
    }
}
