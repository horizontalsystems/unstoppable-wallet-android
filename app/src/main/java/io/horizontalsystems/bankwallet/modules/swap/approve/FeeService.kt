package io.horizontalsystems.bankwallet.modules.swap.approve

import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.adapters.Erc20Adapter
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.guides.DataState
import io.horizontalsystems.core.entities.Currency
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.math.BigDecimal

class FeeService(
        private val amount: BigDecimal,
        private val spenderAddress: String,
        private val feeCoin: Coin,
        private val baseCurrency: Currency,
        private val erc20Adapter: Erc20Adapter,
        private val feeRateProvider: IFeeRateProvider,
        private val rateManager: IRateManager,
        private val feeBalanceAdapter: IBalanceAdapter
) : IFeeService {

    override var gasPrice: Long = 0
    override var gasLimit: Long = 0
    override val feeRatePriority = FeeRatePriority.HIGH

    override val feeValues = BehaviorSubject.create<DataState<Pair<CoinValue, CurrencyValue?>>>()

    private val disposables = CompositeDisposable()

    init {
        feeValues.onNext(DataState.Loading())

        feeRateProvider.feeRates()
                .flatMap {
                    val feeRateInfo = it.first { it.priority == feeRatePriority }

                    gasPrice = feeRateInfo.feeRate

                    erc20Adapter.estimateApprove(spenderAddress, amount, gasPrice)
                }
                .subscribeOn(Schedulers.io())
                .subscribe({
                    gasLimit = it

                    val fee = erc20Adapter.fee(gasPrice, gasLimit)
                    val coinValue = CoinValue(feeCoin, fee)

                    if (feeBalanceAdapter.balance < fee) {
                        feeValues.onNext(DataState.Error(SwapApproveModule.InsufficientFeeBalance(coinValue)))
                    } else {
                        val currencyValue = rateManager.getLatestRate(feeCoin.code, baseCurrency.code)?.let {
                            CurrencyValue(baseCurrency, it * fee)
                        }
                        feeValues.onNext(DataState.Success(Pair(coinValue, currencyValue)))
                    }
                }, {
                    feeValues.onNext(DataState.Error(it))
                })
                .let {
                    disposables.add(it)
                }
    }
}