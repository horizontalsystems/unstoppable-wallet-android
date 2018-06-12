package org.grouvi.wallet.modules.transactions

import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import org.grouvi.wallet.core.subscribeAsync
import org.grouvi.wallet.entities.Transaction
import org.grouvi.wallet.lib.WalletDataManager
import java.util.*
import kotlin.math.absoluteValue

object TransactionsModule {

    interface IView {
        fun showTransactionItems(items: List<TransactionViewItem>)

        var presenter: IPresenter
    }

    interface IPresenter {
        var view: IView
        var interactor: IInteractor
        var router: IRouter

        fun start()
    }

    interface IInteractor {
        var delegate: IInteractorDelegate
        var transactionsDataProvider: ITransactionsDataProvider
        var addressesProvider: IAddressesProvider

        fun retrieveTransactionItems()
    }

    interface IRouter

    interface IInteractorDelegate {
        fun didTransactionItemsRetrieve(items: List<TransactionViewItem>)
    }

    fun initModule(view: IView, router: IRouter) {
        val presenter = TransactionsModulePresenter()
        val interactor = TransactionsModuleInteractor()

        view.presenter = presenter

        presenter.interactor = interactor
        presenter.view = view
        presenter.router = router

        interactor.delegate = presenter
        interactor.transactionsDataProvider = WalletDataManager
        interactor.addressesProvider = WalletDataManager
    }


}

interface ITransactionsDataProvider {
    fun getTransactions(): Flowable<List<Transaction>>
}

interface IAddressesProvider {
    fun getAddresses(): List<String>
}

class TransactionViewItem(var type: Type, var amount: Double, var date: Date) {

    enum class Type {
        IN, OUT
    }

}

class TransactionsModulePresenter : TransactionsModule.IPresenter, TransactionsModule.IInteractorDelegate {

    override lateinit var view: TransactionsModule.IView
    override lateinit var interactor: TransactionsModule.IInteractor
    override lateinit var router: TransactionsModule.IRouter

    override fun start() {
        interactor.retrieveTransactionItems()
    }

    override fun didTransactionItemsRetrieve(items: List<TransactionViewItem>) {
        view.showTransactionItems(items)
    }
}

class TransactionsModuleInteractor : TransactionsModule.IInteractor {

    override lateinit var delegate: TransactionsModule.IInteractorDelegate
    override lateinit var transactionsDataProvider: ITransactionsDataProvider
    override lateinit var addressesProvider: IAddressesProvider

    override fun retrieveTransactionItems() {

        transactionsDataProvider.getTransactions().subscribeAsync(CompositeDisposable(), onNext = { transactions ->
            delegate.didTransactionItemsRetrieve(transactions.map { convertTransactionToViewItem(it) })
        })

    }

    private fun convertTransactionToViewItem(transaction: Transaction): TransactionViewItem {
        val addresses = addressesProvider.getAddresses()

        val myInputsSum = transaction.inputs.filter { addresses.contains(it.address) }.map { it.value }.sum()
        val myOutputsSum = transaction.outputs.filter { addresses.contains(it.address) }.map { it.value }.sum()

        val diff = myOutputsSum - myInputsSum

        val transactionType = if (diff > 0) TransactionViewItem.Type.IN else TransactionViewItem.Type.OUT

        return TransactionViewItem(
                transactionType,
                diff.absoluteValue.div(100000000.0),
                Date(transaction.timestamp)
        )
    }

}
