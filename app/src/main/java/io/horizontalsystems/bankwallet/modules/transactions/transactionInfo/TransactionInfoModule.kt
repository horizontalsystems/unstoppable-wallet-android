package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper

object TransactionInfoModule {
    interface View {
        fun showTransactionItem(transactionViewItem: TransactionViewItem)
        fun showCopied()
    }

    interface ViewDelegate {
        fun onCopyFromAddress()
        fun onCopyToAddress()
        fun onCopyId()
        fun getTransaction(transactionHash: String)
        fun showFullInfo()
    }

    interface Interactor {
        fun getTransaction(transactionHash: String)
        fun onCopy(value: String)
    }

    interface InteractorDelegate {
        fun didGetTransaction(txRecord: TransactionRecord)
    }

    interface Router {
        fun showFullInfo(transactionHash: String, coinCode: CoinCode)
    }

    fun init(view: TransactionInfoViewModel, router: Router) {
        val interactor = TransactionInfoInteractor(App.transactionStorage, TextHelper)
        val presenter = TransactionInfoPresenter(interactor, router, TransactionViewItemFactory(App.walletManager, App.currencyManager, App.rateManager))

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
