package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import io.horizontalsystems.bankwallet.entities.FullTransactionItem
import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.horizontalsystems.bankwallet.entities.FullTransactionSection

class FullTransactionInfoPresenter(val interactor: FullTransactionInfoInteractor, val router: FullTransactionInfoModule.Router, private val state: FullTransactionInfoState)
    : FullTransactionInfoModule.ViewDelegate, FullTransactionInfoModule.InteractorDelegate {

    var view: FullTransactionInfoModule.View? = null

    //
    // State
    //
    override val canShowTransactionInProviderSite: Boolean
        get() = interactor.url(state.transactionHash) != null

    override val providerName: String?
        get() = state.transactionRecord?.providerName

    override val sectionCount: Int
        get() = state.transactionRecord?.sections?.size ?: 0

    override fun getSection(row: Int): FullTransactionSection? {
        return state.transactionRecord?.sections?.get(row)
    }

    //
    // ViewDelegate
    //
    override fun viewDidLoad() {
        interactor.didLoad()
        interactor.updateProvider(state.wallet)
        view?.setShareButtonVisibility(canShowTransactionInProviderSite)
        retryLoadInfo()
    }

    override fun onRetryLoad() {
        if (state.transactionRecord == null) {
            tryLoadInfo()
        }
    }

    override fun onTapId() {
        interactor.copyToClipboard(state.transactionHash)
        view?.showCopied()
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

    override fun onTapProvider() {
        view?.openProviderSettings(state.wallet.coin, state.transactionHash)
    }


    override fun onTapChangeProvider() {
        view?.openProviderSettings(state.wallet.coin, state.transactionHash)
    }

    override fun onTapResource() {
        interactor.url(state.transactionHash)?.let {
            view?.openUrl(it)
        }
    }

    override fun onShare() {
        interactor.url(state.transactionHash)?.let {
            view?.share(it)
        }
    }

    override fun onClear() {
        interactor.clear()
    }

    //
    // InteractorDelegate
    //
    override fun onProviderChange() {
        state.transactionRecord = null
        interactor.updateProvider(state.wallet)

        view?.reload()
        view?.setShareButtonVisibility(canShowTransactionInProviderSite)

        retryLoadInfo()
    }

    override fun onReceiveTransactionInfo(transactionRecord: FullTransactionRecord) {
        state.transactionRecord = transactionRecord
        view?.reload()
        view?.showTransactionInfo()
    }

    override fun onProviderOffline(providerName: String) {
        view?.showErrorProviderOffline(providerName)
    }

    override fun onTransactionNotFound(providerName: String) {
        view?.showErrorTransactionNotFound(providerName)
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
        view?.showLoading()
        interactor.retrieveTransactionInfo(state.transactionHash)
    }
}
