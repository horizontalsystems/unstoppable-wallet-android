package io.horizontalsystems.bankwallet.modules.swap_new.viewmodels

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.swap.provider.StringProvider
import io.horizontalsystems.bankwallet.modules.swap.view.SwapItemFormatter
import io.horizontalsystems.bankwallet.modules.swap_new.SwapModule.CoinBalanceItem
import io.horizontalsystems.bankwallet.modules.swap_new.SwapService
import io.horizontalsystems.bankwallet.modules.swap_new.SwapService.SwapError
import io.horizontalsystems.bankwallet.modules.swap_new.SwapTradeService
import io.horizontalsystems.bankwallet.modules.swap_new.providers.SwapCoinProvider
import io.horizontalsystems.uniswapkit.models.TradeType
import io.reactivex.schedulers.Schedulers

class SwapFromCoinCardViewModel(
        service: SwapService,
        tradeService: SwapTradeService,
        private val coinProvider: SwapCoinProvider,
        formatter: SwapItemFormatter,
        stringProvider: StringProvider
) : SwapCoinCardViewModel(service, tradeService, formatter, stringProvider) {

    override val title = stringProvider.string(R.string.Swap_FromAmountTitle)

    override val tokensForSelection: List<CoinBalanceItem>
        get() = coinProvider.coins(enabledCoins = true)

    override val tradeType = TradeType.ExactIn

    override val coin: Coin?
        get() = tradeService.coinFrom

    init {
        subscribeToServices()
    }

    override fun handleErrors(errors: List<Throwable>) {
        val insufficientBalanceFrom = errors.firstOrNull { it is SwapError.InsufficientBalanceFrom }
        balanceErrorLiveData.postValue(insufficientBalanceFrom != null)
    }

    override fun subscribeToServices() {
        super.subscribeToServices()

        onUpdateCoin(tradeService.coinFrom)
        onUpdateBalance(service.balanceFrom)

        tradeService.amountFromObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    update(it.orElse(null))
                }
                .let { disposables.add(it) }

        tradeService.coinFromObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    onUpdateCoin(it.orElse(null))
                }
                .let { disposables.add(it) }

        service.balanceFromObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    onUpdateBalance(it.orElse(null))
                }.let { disposables.add(it) }

    }

    override fun onSelectCoin(coinBalanceItem: CoinBalanceItem) {
        tradeService.enterCoinFrom(coinBalanceItem.coin)
    }

    override fun onChangeAmount(amount: String?) {
        tradeService.enterAmountFrom(validateAmount(amount))
    }

}
