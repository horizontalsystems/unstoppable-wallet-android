package io.horizontalsystems.bankwallet.modules.ratelist

import androidx.lifecycle.ViewModel
import io.horizontalsystems.xrateskit.entities.MarketInfo

class RateListPresenter(
        val view: RateListView,
        private val interactor: RateListModule.IInteractor,
        private val factory: RateListModule.IRateListFactory
) : ViewModel(), RateListModule.IViewDelegate, RateListModule.IInteractorDelegate {

    //IViewDelegate

    override fun viewDidLoad() {
        val coins = interactor.coins
        val currency = interactor.currency

        val marketInfos = coins.map { it.code to interactor.getMarketInfo(it.code, currency.code)}.toMap()

        val item = factory.rateListViewItem(coins, currency, marketInfos)
        view.show(item)

        val coinCodes = coins.map { it.code }
        interactor.setupXRateManager(coinCodes)
        interactor.subscribeToMarketInfo(currency.code)
    }

    //IInteractorDelegate

    override fun didUpdateMarketInfo(marketInfos: Map<String, MarketInfo>) {
        val item = factory.rateListViewItem(interactor.coins, interactor.currency, marketInfos)
        view.show(item)
    }

    override fun onCleared() {
        super.onCleared()
        interactor.clear()
    }

}
