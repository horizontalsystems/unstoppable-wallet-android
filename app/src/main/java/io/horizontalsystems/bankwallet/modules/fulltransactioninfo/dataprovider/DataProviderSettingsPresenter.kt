package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.dataprovider

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.Request.PostRequest
import java.net.URL

class DataProviderSettingsPresenter(val coin: Coin, val transactionHash: String, private val interactor: DataProviderSettingsModule.Interactor)
    : DataProviderSettingsModule.ViewDelegate, DataProviderSettingsModule.InteractorDelegate {

    var view: DataProviderSettingsModule.View? = null
    var items: List<DataProviderSettingsItem> = listOf()

    override fun viewDidLoad() {
        val baseProvider = interactor.baseProvider(coin)
        val allProviders = interactor.providers(coin)

        allProviders.forEach { provider ->
            val request = provider.apiRequest(transactionHash)

            val url = if (request is PostRequest) {
                val uri = URL(request.url)
                "${uri.protocol}://${uri.host}"
            } else {
                request.url
            }

            interactor.pingProvider(provider.name, url)
        }

        items = allProviders.map {
            DataProviderSettingsItem(name = it.name, online = false, selected = it.name == baseProvider.name, checking = true)
        }

        view?.show(items)
    }

    override fun onPingSuccess(name: String) {
        items.filter { it.name == name }.map {
            it.online = true
            it.checking = false
        }

        view?.show(items)
    }

    override fun onPingFailure(name: String) {
        items.filter { it.name == name }.map {
            it.online = false
            it.checking = false
        }

        view?.show(items)
    }

    override fun onSelect(item: DataProviderSettingsItem) {
        if (!item.selected) {
            interactor.setBaseProvider(item.name, coin)
        }
    }

    override fun onSetDataProvider() {
        view?.close()
    }

}
