package io.horizontalsystems.bankwallet.modules.swap.provider

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveModule
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.uniswapkit.UniswapKit
import io.horizontalsystems.uniswapkit.models.TradeData
import io.reactivex.Single

data class SwapFeeInfo(
        val gasPrice: Long,
        val gasLimit: Long,
        val coinAmount: CoinValue,
        val fiatAmount: CurrencyValue?
)

class UniswapFeeProvider(
        private val uniswapKit: UniswapKit,
        private val walletManager: IWalletManager,
        private val adapterManager: IAdapterManager,
        private val baseCurrency: Currency,
        private val rateManager: IRateManager,
        private val feeRateProvider: IFeeRateProvider
) {

    val feeRatePriority = FeeRatePriority.RECOMMENDED

    fun getSwapFeeInfo(coinSending: Coin, coinFee: Coin, tradeData: TradeData): Single<SwapFeeInfo> =
            Single.create { emitter ->
                try {
                    val feeRateInfoList = feeRateProvider.feeRates().blockingGet()
                    val feeRateInfo = feeRateInfoList.first { it.priority == feeRatePriority }
                    val gasPrice = feeRateInfo.feeRate

                    val gasLimit = uniswapKit.estimateSwap(tradeData, gasPrice).blockingGet()

                    val wallet = walletManager.wallet(coinSending)
                            ?: throw IllegalStateException("No wallet for coin: $coinSending")

                    val adapter = adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter
                            ?: throw IllegalStateException("No adapter for coin: $coinSending")

                    val fee = adapter.fee(gasPrice, gasLimit)
                    val coinValue = CoinValue(coinFee, fee)

                    if (adapter.ethereumBalance < fee) {
                        throw SwapApproveModule.InsufficientFeeBalance(coinValue)
                    }

                    val xRate = rateManager.getLatestRate(coinFee.code, baseCurrency.code)
                    val currencyValue = xRate?.times(fee)?.let { CurrencyValue(baseCurrency, it) }

                    emitter.onSuccess(SwapFeeInfo(gasPrice, gasLimit, coinValue, currencyValue))
                } catch (error: Throwable) {
                    emitter.onError(error)
                }
            }

}
