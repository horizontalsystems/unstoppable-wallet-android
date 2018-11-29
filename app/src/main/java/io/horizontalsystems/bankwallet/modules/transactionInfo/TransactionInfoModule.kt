package io.horizontalsystems.bankwallet.modules.transactionInfo

import android.support.v4.app.FragmentActivity
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionRecordViewItem
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper

object TransactionInfoModule {
    interface IView {
        fun showTransactionItem(transactionViewItem: TransactionViewItem)
        fun close()
        fun showCopied()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onStatusClick()
        fun onCopyAddress()
        fun onCopyId()
        fun onCloseClick()
    }

    interface IInteractor {
        fun getTransaction(transactionHash: String)
        fun onCopy(value: String)
        fun showFullInfo()
    }

    interface IInteractorDelegate {
        fun didGetTransaction(txRecord: TransactionRecord)
        fun showFullInfo(transactionRecordViewItem: TransactionRecordViewItem)
    }

    interface IRouter {
        fun showFullInfo(transaction: TransactionRecordViewItem)
    }

    fun init(view: TransactionInfoViewModel, router: IRouter, transactionHash: String) {
        val interactor = TransactionInfoInteractor(App.appDatabase, TextHelper)
        val presenter = TransactionInfoPresenter(transactionHash, interactor, router, TransactionViewItemFactory(App.walletManager, App.currencyManager))

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(activity: FragmentActivity, transactionHash: String) {
        TransactionInfoFragment.show(activity, transactionHash)
    }

}
