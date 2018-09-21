package bitcoin.wallet.modules.fulltransactioninfo

import bitcoin.wallet.modules.transactions.TransactionRecordViewItem

class FullTransactionInfoPresenter(private val interactor: FullTransactionInfoModule.IInteractor, private val router: FullTransactionInfoModule.IRouter) : FullTransactionInfoModule.IViewDelegate, FullTransactionInfoModule.IInteractorDelegate {
    var view: FullTransactionInfoModule.IView? = null

    override fun viewDidLoad() {
        interactor.retrieveTransaction()
    }

    override fun didGetTransactionInfo(txRecordViewItem: TransactionRecordViewItem) {
        view?.showTransactionItem(txRecordViewItem)
    }

    override fun didCopyToClipboard() {
        view?.showCopied()
    }

    override fun showBlockInfo(txRecordViewItem: TransactionRecordViewItem) {
        router.showBlockInfo(txRecordViewItem)
    }

    override fun openShareDialog(txRecordViewItem: TransactionRecordViewItem) {
        router.shareTransaction(txRecordViewItem)
    }

    override fun onShareClick() {
        interactor.openShareDialog()
    }

    override fun onTransactionIdClick() {
        interactor.onCopyTransactionId()
    }

    override fun onFromFieldClick() {
        interactor.onCopyFromAddress()
    }

    override fun onToFieldClick() {
        interactor.onCopyToAddress()
    }
}
