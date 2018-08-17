package bitcoin.wallet.modules.transactionInfo

import bitcoin.wallet.modules.transactions.TransactionRecordViewItem

class TransactionInfoPresenter(private val interactor: TransactionInfoModule.IInteractor, private val router: TransactionInfoModule.IRouter, private val coinCode: String, private val txHash: String) : TransactionInfoModule.IViewDelegate, TransactionInfoModule.IInteractorDelegate {
    var view: TransactionInfoModule.IView? = null

    // IViewDelegate methods

    override fun viewDidLoad() {
        interactor.getTransactionInfo(coinCode, txHash)
    }

    override fun onDetailsClick() {
        view?.showDetails()
    }

    override fun onCloseClick() {
        view?.close()
    }

    // IInteractorDelegate methods

    override fun didGetTransactionInfo(txRecordViewItem: TransactionRecordViewItem) {
        view?.showTransactionItem(txRecordViewItem)
    }

}
