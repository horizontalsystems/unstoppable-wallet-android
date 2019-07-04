package io.horizontalsystems.bankwallet.modules.backup

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IRandomProvider
import io.reactivex.disposables.CompositeDisposable

class BackupInteractor(private val accountManager: IAccountManager, private val indexesProvider: IRandomProvider)
    : BackupModule.IInteractor {

    var delegate: BackupModule.IInteractorDelegate? = null

    private var disposables: CompositeDisposable = CompositeDisposable()

    override fun getAccount(id: String) {
        accountManager.accountsFlowable
                .subscribe({ list ->
                    val account = list.find { it.id == id }
                    if (account == null) {
                        delegate?.onGetAccountFailed()
                    } else {
                        delegate?.onGetAccount(account)
                    }
                }, {
                    delegate?.onGetAccountFailed()
                })
                .let {
                    disposables.add(it)
                }
    }

    override fun setBackedUp(accountId: String) {
        accountManager.setIsBackedUp(accountId)
    }

    override fun fetchConfirmationIndexes(): List<Int> {
        return indexesProvider.getRandomIndexes(2)
    }

}
