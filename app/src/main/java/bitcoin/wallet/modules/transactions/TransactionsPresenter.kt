package bitcoin.wallet.modules.transactions

class TransactionsPresenter(private val interactor: TransactionsModule.IInteractor, private val router: TransactionsModule.IRouter) : TransactionsModule.IViewDelegate, TransactionsModule.IInteractorDelegate {

    var view: TransactionsModule.IView? = null

    override fun viewDidLoad() {
        interactor.retrieveTransactionRecords()
    }

    override fun didRetrieveTransactionRecords(items: List<TransactionRecordViewItem>) {
        view?.showTransactionItems(items)
    }

}
