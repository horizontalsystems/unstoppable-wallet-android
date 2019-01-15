package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem

class TransactionInfoPresenter(
        private val interactor: TransactionInfoModule.Interactor,
        private val router: TransactionInfoModule.Router,
        private val transactionFactory: TransactionViewItemFactory
) : TransactionInfoModule.ViewDelegate, TransactionInfoModule.InteractorDelegate {

    var view: TransactionInfoModule.View? = null
    private var transactionViewItem: TransactionViewItem? = null
    private var transactionHash = ""

    // ViewDelegate methods
    override fun getTransaction(transactionHash: String) {
        this.transactionHash = transactionHash
        interactor.getTransaction(transactionHash)
    }

    override fun onCopyFromAddress() {
        transactionViewItem?.from?.let {
            interactor.onCopy(it)
            view?.showCopied()
        }
    }

    override fun onCopyToAddress() {
        transactionViewItem?.to?.let {
            interactor.onCopy(it)
            view?.showCopied()
        }
    }

    override fun onCopyId() {
        interactor.onCopy(transactionHash)
        view?.showCopied()
    }

    override fun showFullInfo() {
        transactionViewItem?.let {
            router.showFullInfo(it.transactionHash, it.coinValue.coinCode)
        }
    }

    // IInteractorDelegate methods

    override fun didGetTransaction(txRecord: TransactionRecord) {
//        val viewItem = transactionFactory.item(txRecord)
//        transactionViewItem = viewItem
//        view?.showTransactionItem(viewItem)
    }

}
