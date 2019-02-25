package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import io.horizontalsystems.bankwallet.entities.Coin

class TransactionInfoPresenter(
        private val interactor: TransactionInfoModule.Interactor,
        private val router: TransactionInfoModule.Router
) : TransactionInfoModule.ViewDelegate, TransactionInfoModule.InteractorDelegate {

    var view: TransactionInfoModule.View? = null

    // ViewDelegate methods

    override fun onCopy(value: String) {
        interactor.onCopy(value)
        view?.showCopied()
    }

    override fun openFullInfo(transactionHash: String, coin: Coin) {
        router.openFullInfo(transactionHash, coin)
    }

}
