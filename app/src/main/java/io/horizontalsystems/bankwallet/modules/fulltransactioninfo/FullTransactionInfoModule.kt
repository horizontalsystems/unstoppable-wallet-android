package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import android.support.v4.app.FragmentActivity
import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.core.App
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
        fun showError(providerName: String?)
        fun showCopied()
        fun openUrl(url: String)
        fun openProviderSettings(coinCode: CoinCode)
        fun share(url: String)
    }

    interface ViewDelegate {
        fun viewDidLoad()
        fun onRetryLoad()

        val providerName: String?
        val sectionCount: Int
        fun getSection(row: Int): FullTransactionSection?
        fun onTapItem(item: FullTransactionItem)
        fun onTapProvider()
        fun onTapResource()
        fun onShare()
    }

    interface Interactor {
        fun didLoad()
        fun updateProvider(coinCode: CoinCode)

        fun url(hash: String): String?

        fun retrieveTransactionInfo(transactionHash: String)
        fun copyToClipboard(value: String)
    }

    interface InteractorDelegate {
        fun onProviderChange()
        fun onReceiveTransactionInfo(transactionRecord: FullTransactionRecord)
        fun onError(providerName: String?)
        fun retryLoadInfo()
    }

    interface Router

    interface Provider {
        val name: String

        fun url(hash: String): String
        fun apiUrl(hash: String): String
    }

    interface FullProvider {
        val providerName: String
        fun url(hash: String): String

        fun retrieveTransactionInfo(transactionHash: String): Flowable<FullTransactionRecord>
    }

    interface ProvidersMap {
        fun bitcoin(name: String): BitcoinForksProvider
        fun bitcoinCash(name: String): BitcoinForksProvider
        fun ethereum(name: String): EthereumForksProvider
    }

    interface BitcoinForksProvider : Provider {
        fun convert(json: JsonObject): BitcoinResponse
    }

    interface EthereumForksProvider : Provider {
        fun convert(json: JsonObject): EthereumResponse
    }

    interface Adapter {
        fun convert(json: JsonObject): FullTransactionRecord
    }

    interface ProviderFactory {
        fun providerFor(coinCode: CoinCode): FullProvider
    }

    interface ProviderDelegate {
        fun onReceiveTransactionInfo(transactionRecord: FullTransactionRecord)
    }

    interface State {
        val coinCode: CoinCode
        val transactionHash: String
        var transactionRecord: FullTransactionRecord?
    }

    fun init(view: FullTransactionInfoViewModel, router: Router, coinCode: CoinCode, transactionHash: String) {
        val interactor = FullTransactionInfoInteractor(App.transactionInfoFactory, App.transactionDataProviderManager, TextHelper)
        val presenter = FullTransactionInfoPresenter(interactor, router, FullTransactionInfoState(coinCode, transactionHash))

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(activity: FragmentActivity, transactionHash: String, coinCode: CoinCode) {
        FullTransactionInfoActivity.start(activity, transactionHash, coinCode)
    }
}
