package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import io.horizontalsystems.bankwallet.entities.FullTransactionItem
import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.horizontalsystems.bankwallet.entities.FullTransactionSection

class FullTransactionInfoPresenter(val interactor: FullTransactionInfoInteractor, val router: FullTransactionInfoModule.Router, private val state: FullTransactionInfoState)
    : FullTransactionInfoModule.ViewDelegate, FullTransactionInfoModule.InteractorDelegate {

    var view: FullTransactionInfoModule.View? = null

    //
    // ViewDelegate
    //
    override fun viewDidLoad() {
        retryLoadInfo()
    }

    override fun onRetryLoad() {
        interactor.retryLoadInfo()
    }

    override fun onTapItem(item: FullTransactionItem) {
        interactor.onTapItem(item)
    }

    override fun onTapResource() {
        view?.openUrl(state.url)
    }

    override fun onShare() {
        view?.share(state.url)
    }

    //
    // State
    //
    override val providerName: String
        get() = state.providerName

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

    override fun onError() {
        view?.hideLoading()
        view?.showError()
    }

    override fun retryLoadInfo() {
        if (state.transactionRecord == null) {
            tryLoadInfo()
        }
    }

    override fun onCopied() {
        view?.showCopied()
    }

    override fun onOpenUrl(url: String) {
        view?.openUrl(url)
    }

    //
    // Private
    //
    private fun tryLoadInfo() {
        view?.hideError()
        view?.showLoading()

        interactor.retrieveTransactionInfo(state.transactionHash)
    }
}
