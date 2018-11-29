package io.horizontalsystems.bankwallet.modules.transactionInfo

import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionRecordViewItem
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem

class TransactionInfoPresenter(
        private val transactionHash: String,
        private val interactor: TransactionInfoModule.IInteractor,
        private val router: TransactionInfoModule.IRouter,
        private val transactionFactory: TransactionViewItemFactory
) : TransactionInfoModule.IViewDelegate, TransactionInfoModule.IInteractorDelegate {

    var view: TransactionInfoModule.IView? = null
    var transactionViewItem: TransactionViewItem? = null

    // IViewDelegate methods

    override fun viewDidLoad() {
        interactor.getTransaction(transactionHash)
    }

    override fun onCopyAddress() {
        val address = transactionViewItem?.let { if (it.incoming) it.from else it.to }
        address?.let {
            interactor.onCopy(it)
            view?.showCopied()
        }
    }

    override fun onCopyId() {
        interactor.onCopy(transactionHash)
        view?.showCopied()
    }

    override fun onStatusClick() {
        interactor.showFullInfo()
    }

    override fun onCloseClick() {
        view?.close()
    }

    override fun showFullInfo(transactionRecordViewItem: TransactionRecordViewItem) {
        router.showFullInfo(transactionRecordViewItem)
    }

    // IInteractorDelegate methods

    override fun didGetTransaction(txRecord: TransactionRecord) {
        val viewItem = transactionFactory.item(txRecord)
        transactionViewItem = viewItem
        view?.showTransactionItem(viewItem)
    }

}
