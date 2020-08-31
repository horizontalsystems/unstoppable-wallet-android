package io.horizontalsystems.bankwallet.modules.swap.service

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.swap.DataState
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.uniswapkit.UniswapKit
import io.horizontalsystems.uniswapkit.models.TradeData
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable

class UniswapFeeService(
        private val uniswapKit: UniswapKit,
        private val walletManager: IWalletManager,
        private val adapterManager: IAdapterManager,
        private val baseCurrency: Currency,
        private val rateManager: IRateManager,
        private val feeRateProvider: IFeeRateProvider
) {

    val feeRatePriority = FeeRatePriority.HIGH

    fun swapFee(coinSending: Coin, coinFee: Coin, tradeData: TradeData): Flowable<DataState<Pair<CoinValue, CurrencyValue?>>> {
        return Flowable.create({ emitter ->
            emitter.onNext(DataState.Loading)

            try {
                val feeRateInfoList = feeRateProvider.feeRates().blockingGet()
                val feeRateInfo = feeRateInfoList.first { it.priority == feeRatePriority }
                val gasPrice = feeRateInfo.feeRate

                val gasLimit = uniswapKit.estimateSwap(tradeData, gasPrice).blockingGet()

                val wallet = walletManager.wallet(coinFee)
                if (wallet == null) {
                    emitter.onNext(DataState.Error(IllegalStateException("No wallet for coinFee: $coinFee")))
                    return@create
                }

                val adapter = adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter
                if (adapter == null) {
                    emitter.onNext(DataState.Error(IllegalStateException("No adapter for coinFee: $coinFee")))
                    return@create
                }

                val fee = adapter.fee(gasPrice, gasLimit)
                val coinValue = CoinValue(coinSending, fee)

                val xRate = rateManager.getLatestRate(coinFee.code, baseCurrency.code)
                val currencyValue = xRate?.times(fee)?.let { CurrencyValue(baseCurrency, it) }

                emitter.onNext(DataState.Success(Pair(coinValue, currencyValue)))

            } catch (throwable: Throwable) {
                emitter.onNext(DataState.Error(throwable))
            }
            emitter.onComplete()
        }, BackpressureStrategy.BUFFER)
    }

}
