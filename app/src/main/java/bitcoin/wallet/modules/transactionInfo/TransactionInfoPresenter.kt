package bitcoin.wallet.modules.transactionInfo

import bitcoin.wallet.modules.transactions.TransactionRecordViewItem

class TransactionInfoPresenter(private val interactor: TransactionInfoModule.IInteractor, private val router: TransactionInfoModule.IRouter, private val coinCode: String, private val txHash: String) : TransactionInfoModule.IViewDelegate, TransactionInfoModule.IInteractorDelegate {
    var view: TransactionInfoModule.IView? = null

    private var expanded = false

    // IViewDelegate methods

    override fun viewDidLoad() {
        interactor.getTransactionInfo(coinCode, txHash)
    }

    override fun onLessMoreClick() {
        if (expanded) {
            view?.lessen()
        } else {
            view?.expand()
        }
        expanded = !expanded
    }

    // IInteractorDelegate methods

    override fun didGetTransactionInfo(txRecordViewItem: TransactionRecordViewItem) {
        view?.showTransactionItem(txRecordViewItem)
    }

}
