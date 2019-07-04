package io.horizontalsystems.bankwallet.modules.settings.managekeys

import io.horizontalsystems.bankwallet.core.Account
import io.horizontalsystems.bankwallet.core.ILockManager
import io.horizontalsystems.bankwallet.core.managers.AccountManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class ManageKeysInteractor(private val accountManager: AccountManager, private val lockManager: ILockManager) : ManageKeysModule.Interactor {

    var delegate: ManageKeysModule.InteractorDelegate? = null

    private var lockStateUpdateDisposable: Disposable? = null
    private val disposables = CompositeDisposable()

    override fun loadAccounts() {
//        delegate?.didLoad(accountManager.accounts)

        accountManager.accountsFlowable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { delegate?.didLoad(it) }
                .let { disposables.add(it) }
    }

    override fun backupAccount(account: Account) {
        delegate?.accessIsRestricted()
        lockStateUpdateDisposable?.dispose()
        lockStateUpdateDisposable = lockManager.lockStateUpdatedSignal.subscribe {
            if (!lockManager.isLocked) {
                delegate?.openBackupWallet(account)
                lockStateUpdateDisposable?.dispose()
            }
        }
    }

    override fun deleteAccount(id: String) {
        accountManager.delete(id)
    }

    override fun clear() {
        disposables.clear()
    }
}
