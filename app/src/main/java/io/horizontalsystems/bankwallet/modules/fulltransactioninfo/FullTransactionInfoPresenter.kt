package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.horizontalsystems.bankwallet.entities.FullTransactionSection

class FullTransactionInfoPresenter(val interactor: FullTransactionInfoInteractor, val router: FullTransactionInfoModule.Router, private val state: FullTransactionInfoState)
    : FullTransactionInfoModule.ViewDelegate, FullTransactionInfoModule.InteractorDelegate {

    var view: FullTransactionInfoModule.View? = null

    //
    // ViewDelegate
    //
    override fun viewDidLoad() {
        view?.showLoading()

        interactor.retrieveTransactionInfo(state.transactionHash)
    }

    //
    // State
    //
    override val resource: String
        get() = state.transactionRecord?.resource ?: ""

    override val sectionCount: Int
        get() = state.transactionRecord?.sections?.size ?: 0

    override fun getSection(row: Int): FullTransactionSection? {
        return state.transactionRecord?.sections?.get(row)
    }

    //
    // InteractorDelegate
    //
    override fun onReceiveTransactionInfo(transactionRecord: FullTransactionRecord) {
        state.transactionRecord = transactionRecord
        view?.hideLoading()
        view?.reload()
    }

}
