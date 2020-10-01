package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers.BinanceResponse
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers.BitcoinResponse
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers.EosResponse
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers.EthereumResponse
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.reactivex.Flowable

object FullTransactionInfoModule {
    interface View {
        fun reload()
        fun showLoading()
        fun showErrorProviderOffline(providerName: String)
        fun showErrorTransactionNotFound(providerName: String)
        fun showCopied()
        fun openUrl(url: String)
        fun openProviderSettings(coin: Coin, transactionHash: String)
        fun share(url: String)
        fun setShareButtonVisibility(visible: Boolean)
        fun showTransactionInfo()
    }

    interface ViewDelegate {
        fun viewDidLoad()
        fun onRetryLoad()

        val canShowTransactionInProviderSite: Boolean
        val providerName: String?
        val sectionCount: Int
        fun getSection(row: Int): FullTransactionSection?
        fun onTapId()
        fun onTapItem(item: FullTransactionItem)
        fun onTapProvider()
        fun onTapResource()
        fun onTapChangeProvider()
        fun onShare()
        fun onClear()
    }

    interface Interactor {
        fun didLoad()
        fun updateProvider(wallet: Wallet)

        fun url(hash: String): String?

        fun retrieveTransactionInfo(transactionHash: String)
        fun copyToClipboard(value: String)
        fun clear()
    }

    interface InteractorDelegate {
        fun onProviderChange()
        fun onReceiveTransactionInfo(transactionRecord: FullTransactionRecord)
        fun onProviderOffline(providerName: String)
        fun onTransactionNotFound(providerName: String)
        fun retryLoadInfo()
    }

    interface Router

    interface Provider {
        val name: String
        val pingUrl: String
        val isTrusted: Boolean

        fun url(hash: String): String?
        fun apiRequest(hash: String): Request
    }

    sealed class Request(val url: String,val isSafeCall: Boolean) {
        class GetRequest(url: String, isSafeCall: Boolean = true) : Request(url, isSafeCall)
        class PostRequest(url: String, val body: Map<String, Any>) : Request(url, true)
    }

    interface FullProvider {
        val providerName: String
        fun url(hash: String): String?

        fun retrieveTransactionInfo(transactionHash: String): Flowable<FullTransactionRecord>
    }

    interface BitcoinForksProvider : Provider {
        fun convert(json: JsonObject): BitcoinResponse
    }

    interface EthereumForksProvider : Provider {
        fun convert(json: JsonObject): EthereumResponse
    }

    interface BinanceProvider : Provider {
        fun convert(json: JsonObject): BinanceResponse
    }

    interface EosProvider : Provider {
        fun convert(json: JsonObject, eosAccount: String): EosResponse
    }

    interface Adapter {
        fun convert(json: JsonObject): FullTransactionRecord
    }

    interface ProviderFactory {
        fun providerFor(wallet: Wallet): FullProvider
    }

    interface ProviderDelegate {
        fun onReceiveTransactionInfo(transactionRecord: FullTransactionRecord)
    }

    interface State {
        val wallet: Wallet
        val transactionHash: String
        var transactionRecord: FullTransactionRecord?
    }

    fun init(view: FullTransactionInfoViewModel, router: Router, wallet: Wallet, transactionHash: String) {
        val interactor = FullTransactionInfoInteractor(App.transactionInfoFactory, App.transactionDataProviderManager, TextHelper)
        val presenter = FullTransactionInfoPresenter(interactor, router, FullTransactionInfoState(wallet, transactionHash))

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
