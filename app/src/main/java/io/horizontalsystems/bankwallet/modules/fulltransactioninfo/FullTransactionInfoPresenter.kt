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
        if (state.transactionRecord == null) {
            onRetryLoad()
        }
    }

    override fun onTapItem(item: FullTransactionItem) {
        if (item.clickable) {
            if (item.url != null) {
                view?.openUrl(item.url)
            } else if (item.value != null) {
                interactor.copyToClipboard(item.value)
                view?.showCopied()
            }
        }
    }

    override fun onTapResource() {
        view?.openUrl(interactor.url(state.transactionHash))
    }

    override fun onShare() {
        view?.share(interactor.url(state.transactionHash))
    }

    //
    // State
    //
    override val providerName: String?
        get() = state.transactionRecord?.providerName

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

    override fun onError(providerName: String) {
        view?.hideLoading()
        view?.showError(providerName)
    }

    override fun retryLoadInfo() {
        if (state.transactionRecord == null) {
            tryLoadInfo()
        }
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
