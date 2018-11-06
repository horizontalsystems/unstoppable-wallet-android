package bitcoin.wallet.modules.transactionInfo

import bitcoin.wallet.modules.transactions.TransactionRecordViewItem

class TransactionInfoPresenter(private val interactor: TransactionInfoModule.IInteractor, private val router: TransactionInfoModule.IRouter) : TransactionInfoModule.IViewDelegate, TransactionInfoModule.IInteractorDelegate {
    var view: TransactionInfoModule.IView? = null

    // IViewDelegate methods

    override fun viewDidLoad() {
        interactor.getTransactionInfo()
    }

    override fun onCopyAddress() {
        interactor.onCopyAddress()
    }

    override fun onCopyId() {
        interactor.onCopyId()
    }

    override fun onStatusClick() {
        interactor.showFullInfo()
    }

    override fun onCloseClick() {
        view?.close()
    }

    override fun didCopyToClipboard() {
        view?.showCopied()
    }

    override fun showFullInfo(transactionRecordViewItem: TransactionRecordViewItem) {
        router.showFullInfo(transactionRecordViewItem)
    }

    // IInteractorDelegate methods

    override fun didGetTransactionInfo(txRecordViewItem: TransactionRecordViewItem) {
        view?.showTransactionItem(txRecordViewItem)
    }

}
