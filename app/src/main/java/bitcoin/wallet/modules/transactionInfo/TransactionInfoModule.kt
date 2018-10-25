package bitcoin.wallet.modules.transactionInfo

import android.support.v4.app.FragmentActivity
import bitcoin.wallet.modules.transactions.TransactionRecordViewItem
import bitcoin.wallet.viewHelpers.TextHelper

object TransactionInfoModule {
    interface IView {
        fun showTransactionItem(transactionRecordViewItem: TransactionRecordViewItem)
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
        fun getTransactionInfo()
        fun onCopyAddress()
        fun onCopyId()
        fun showFullInfo()
    }

    interface IInteractorDelegate {
        fun didGetTransactionInfo(txRecordViewItem: TransactionRecordViewItem)
        fun didCopyToClipboard()
        fun showFullInfo(transactionRecordViewItem: TransactionRecordViewItem)
    }

    interface IRouter {
        fun showFullInfo(transaction: TransactionRecordViewItem)
    }

    fun init(view: TransactionInfoViewModel, router: IRouter, transaction: TransactionRecordViewItem) {
        val interactor = TransactionInfoInteractor(transaction, TextHelper)
        val presenter = TransactionInfoPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(activity: FragmentActivity, transactionRecordViewItem: TransactionRecordViewItem) {
        TransactionInfoFragment.show(activity, transactionRecordViewItem)
    }

}
