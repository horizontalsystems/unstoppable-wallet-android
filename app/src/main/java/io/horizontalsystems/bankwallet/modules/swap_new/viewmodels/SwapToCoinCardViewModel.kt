package io.horizontalsystems.bankwallet.modules.swap_new.viewmodels

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.swap.provider.StringProvider
import io.horizontalsystems.bankwallet.modules.swap.view.SwapItemFormatter
import io.horizontalsystems.bankwallet.modules.swap_new.SwapModule.CoinBalanceItem
import io.horizontalsystems.bankwallet.modules.swap_new.SwapService
import io.horizontalsystems.bankwallet.modules.swap_new.SwapTradeService
import io.horizontalsystems.bankwallet.modules.swap_new.providers.SwapCoinProvider
import io.horizontalsystems.uniswapkit.models.TradeType
import io.reactivex.schedulers.Schedulers

class SwapToCoinCardViewModel(
        service: SwapService,
        tradeService: SwapTradeService,
        private val coinProvider: SwapCoinProvider,
        formatter: SwapItemFormatter,
        stringProvider: StringProvider
) : SwapCoinCardViewModel(service, tradeService, formatter, stringProvider) {

    override val title = stringProvider.string(R.string.Swap_ToAmountTitle)

    override val tokensForSelection: List<CoinBalanceItem>
        get() = coinProvider.coins(enabledCoins = false)

    override val tradeType = TradeType.ExactOut

    override val coin: Coin?
        get() = tradeService.coinTo

    init {
        subscribeToServices()
    }

    override fun subscribeToServices() {
        super.subscribeToServices()

        onUpdateCoin(tradeService.coinTo)
        onUpdateBalance(service.balanceTo)

        tradeService.amountToObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    update(it.orElse(null))
                }
                .let { disposables.add(it) }

        tradeService.coinToObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    onUpdateCoin(it.orElse(null))
                }
                .let { disposables.add(it) }
        service.balanceToObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    onUpdateBalance(it.orElse(null))
                }.let { disposables.add(it) }

    }

    override fun onSelectCoin(coinBalanceItem: CoinBalanceItem) {
        tradeService.enterCoinTo(coinBalanceItem.coin)
    }

    override fun onChangeAmount(amount: String?) {
        tradeService.enterAmountTo(validateAmount(amount))
    }

}
