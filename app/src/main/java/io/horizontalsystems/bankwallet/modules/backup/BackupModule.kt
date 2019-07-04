package io.horizontalsystems.bankwallet.modules.backup

import android.content.Context
import io.horizontalsystems.bankwallet.core.Account
import io.horizontalsystems.bankwallet.core.AccountType
import io.horizontalsystems.bankwallet.core.App
import java.util.*

object BackupModule {

    interface IView {
        fun showWords(words: List<String>)
        fun showConfirmationWords(indexes: List<Int>)
        fun showConfirmationError()

        fun loadPage(page: Int)
        fun validateWords()
    }

    interface IPresenter : IInteractorDelegate, IViewDelegate {
        var view: IView?
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onNextClick()
        fun onBackClick()
        fun validateDidClick(confirmationWords: HashMap<Int, String>)
    }

    interface IInteractor {
        fun getAccount(id: String)
        fun setBackedUp(accountId: String)
        fun fetchConfirmationIndexes(): List<Int>
    }

    interface IInteractorDelegate {
        fun onGetAccount(account: Account)
        fun onGetAccountFailed()
    }

    interface IRouter {
        fun close()
    }

    //  helpers

    fun start(context: Context, account: Account) {
        if (account.type is AccountType.Mnemonic) {
            BackupActivity.start(context, account.id)
        }
    }

    fun init(view: BackupViewModel, router: IRouter, accountId: String) {
        val interactor = BackupInteractor(App.accountManager, App.randomManager)
        val presenter = BackupPresenter(interactor, router, accountId)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
