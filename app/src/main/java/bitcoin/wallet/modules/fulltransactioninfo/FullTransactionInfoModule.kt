package bitcoin.wallet.modules.fulltransactioninfo

import android.support.v4.app.FragmentActivity
import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.modules.transactions.TransactionRecordViewItem
import bitcoin.wallet.viewHelpers.TextHelper

object FullTransactionInfoModule {
    interface IView {
        fun showTransactionItem(transactionRecordViewItem: TransactionRecordViewItem)
        fun showCopied()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onShareClick()
        fun onTransactionIdClick()
        fun onFromFieldClick()
        fun onToFieldClick()
    }

    interface IInteractor {
        fun retrieveTransaction()
        fun getTransactionInfo()
        fun onCopyFromAddress()
        fun onCopyToAddress()
        fun showBlockInfo()
        fun openShareDialog()
        fun onCopyTransactionId()
    }

    interface IInteractorDelegate {
        fun didGetTransactionInfo(txRecordViewItem: TransactionRecordViewItem)
        fun didCopyToClipboard()
        fun showBlockInfo(txRecordViewItem: TransactionRecordViewItem)
        fun openShareDialog(txRecordViewItem: TransactionRecordViewItem)
    }

    interface IRouter {
        fun showBlockInfo(transaction: TransactionRecordViewItem)
        fun shareTransaction(transaction: TransactionRecordViewItem)
    }

    fun init(view: FullTransactionInfoViewModel, router: IRouter, adapterId: String, transactionId: String) {
        val adapter = AdapterManager.adapters.firstOrNull { it.id == adapterId }
        val baseCurrency = Factory.preferencesManager.getBaseCurrency()
        val interactor = FullTransactionInfoInteractor(adapter, Factory.exchangeRateManager, transactionId, TextHelper, baseCurrency)
        val presenter = FullTransactionInfoPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(activity: FragmentActivity, adapterId: String, transactionId: String) {
        FullTransactionInfoActivity.start(activity, adapterId, transactionId)
    }

}
