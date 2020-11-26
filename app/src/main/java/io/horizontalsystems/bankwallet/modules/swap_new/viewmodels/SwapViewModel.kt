package io.horizontalsystems.bankwallet.modules.swap_new.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.modules.swap.model.PriceImpact
import io.horizontalsystems.bankwallet.modules.swap.view.SwapItemFormatter
import io.horizontalsystems.bankwallet.modules.swap.view.item.TradeViewItem
import io.horizontalsystems.bankwallet.modules.swap_new.SwapService
import io.horizontalsystems.bankwallet.modules.swap_new.SwapTradeService
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class SwapViewModel(
        private val service: SwapService,
        private val tradeService: SwapTradeService,
        private val formatter: SwapItemFormatter
) : ViewModel() {

    private val disposables = CompositeDisposable()

    private val tradeViewItemLiveData = MutableLiveData<TradeViewItem?>()

    //region outputs
    fun tradeViewItemLiveData(): LiveData<TradeViewItem?> = tradeViewItemLiveData

    //endregion

    init {
        subscribeToServices()
    }

    private fun subscribeToServices() {
        service.stateObservable
                .subscribeOn(Schedulers.io())
                .subscribe { sync(it) }
                .let { disposables.add(it) }

        service.errorsObservable
                .subscribeOn(Schedulers.io())
                .subscribe { sync(it) }
                .let { disposables.add(it) }

        tradeService.stateObservable
                .subscribeOn(Schedulers.io())
                .subscribe { sync(it) }
                .let { disposables.add(it) }

        tradeService.tradeOptionsObservable
                .subscribeOn(Schedulers.io())
                .subscribe { sync(it) }
                .let { disposables.add(it) }

    }


    private fun sync(serviceState: SwapService.State) {
        //TODO("Not yet implemented")
    }

    private fun sync(errors: List<Throwable>) {
        //TODO("Not yet implemented")
    }

    private fun sync(tradeServiceState: SwapTradeService.State) {
        when (tradeServiceState) {
            is SwapTradeService.State.Ready -> {
                tradeViewItemLiveData.postValue(tradeViewItem(tradeServiceState.trade))
            }
            else -> tradeViewItemLiveData.postValue(null)
        }
    }

    private fun sync(tradeOptions: TradeOptions) {
        //TODO("Not yet implemented")
    }

    private fun tradeViewItem(trade: SwapTradeService.Trade): TradeViewItem {

        return TradeViewItem(
                formatter.price(trade.tradeData.executionPrice, tradeService.coinFrom, tradeService.coinTo),
                formatter.priceImpact(trade.tradeData.priceImpact),
                trade.priceImpactLevel,
                formatter.minMaxTitle(trade.tradeData.type),
                formatter.minMaxValue(trade.minMaxAmount, tradeService.coinFrom, tradeService.coinTo, trade.tradeData.type)
        )
    }


    //region models
    data class TradeViewItem(
            val price: String? = null,
            val priceImpact: String? = null,
            val priceImpactLevel: SwapTradeService.PriceImpactLevel,
            val minMaxTitle: String? = null,
            val minMaxAmount: String? = null
    )

    //endregion
}
