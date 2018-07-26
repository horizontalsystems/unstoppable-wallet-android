package bitcoin.wallet.modules.transactionInfo

import android.support.v4.app.FragmentActivity
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.modules.transactions.TransactionRecordViewItem

object TransactionInfoModule {
    interface IView {
        fun showTransactionItem(transactionRecordViewItem: TransactionRecordViewItem)
        fun expand()
        fun lessen()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onLessMoreClick()
    }

    interface IInteractor {
        fun getTransactionInfo(coinCode: String, txHash: String)
    }

    interface IInteractorDelegate {
        fun didGetTransactionInfo(txRecordViewItem: TransactionRecordViewItem)
    }

    interface IRouter {}

    fun init(view: TransactionInfoViewModel, router: IRouter, coinCode: String, txHash: String) {
        val interactor = TransactionInfoInteractor(Factory.databaseManager, Factory.coinManager, Factory.transactionConverter)
        val presenter = TransactionInfoPresenter(interactor, router, coinCode, txHash)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(activity: FragmentActivity, coinCode: String, txHash: String) {
        TransactionInfoFragment.show(activity, coinCode, txHash)
    }

}
