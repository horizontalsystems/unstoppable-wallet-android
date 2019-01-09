package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import android.support.v4.app.FragmentActivity
import io.horizontalsystems.bankwallet.entities.FullTransactionItem
import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.horizontalsystems.bankwallet.entities.FullTransactionSection
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper
import io.reactivex.Flowable

object FullTransactionInfoModule {
    interface View {
        fun show()
        fun reload()
        fun showLoading()
        fun hideLoading()
        fun hideError()
        fun showError()
        fun showCopied()
        fun openUrl(url: String)
        fun share(url: String)
    }

    interface ViewDelegate {
        fun viewDidLoad()
        fun onRetryLoad()

        val providerName: String
        val sectionCount: Int
        fun getSection(row: Int): FullTransactionSection?
        fun onTapItem(item: FullTransactionItem)
        fun onTapResource()
        fun onShare()
    }

    interface Interactor {
        fun retrieveTransactionInfo(transactionHash: String)
        fun retryLoadInfo()
        fun onTapItem(item: FullTransactionItem)
    }

    interface InteractorDelegate {
        fun onReceiveTransactionInfo(transactionRecord: FullTransactionRecord)
        fun onError()
        fun retryLoadInfo()
        fun onCopied()
        fun onOpenUrl(url: String)
    }

    interface Router

    interface Provider {
        val url: String
        val name: String
        fun retrieveTransactionInfo(transactionHash: String): Flowable<FullTransactionRecord>
    }

    interface Adapter {
        fun convert(response: FullTransactionResponse): FullTransactionRecord?
    }

    interface ProviderFactory {
        fun providerFor(coinCode: CoinCode): Provider
    }

    interface ProviderDelegate {
        fun onReceiveTransactionInfo(transactionRecord: FullTransactionRecord)
    }

    interface State {
        val url: String
        val providerName: String
        val transactionHash: String
        var transactionRecord: FullTransactionRecord?
    }

    fun init(view: FullTransactionInfoViewModel, router: Router, provider: Provider, transactionHash: String) {
        val interactor = FullTransactionInfoInteractor(provider, TextHelper)
        val dataSource = FullTransactionInfoState(transactionHash, provider.url + transactionHash, provider.name)
        val presenter = FullTransactionInfoPresenter(interactor, router, dataSource)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(activity: FragmentActivity, transactionHash: String, coinCode: CoinCode) {
        FullTransactionInfoActivity.start(activity, transactionHash, coinCode)
    }
}
