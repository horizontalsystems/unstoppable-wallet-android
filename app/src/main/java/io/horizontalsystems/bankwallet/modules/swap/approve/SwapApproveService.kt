package io.horizontalsystems.bankwallet.modules.swap.approve

import io.horizontalsystems.bankwallet.core.FeeRatePriority
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

class SwapApproveService(
        override val coin: Coin,
        override val amount: BigDecimal,
        private val spenderAddress: String,
        private val feeCoin: Coin,
        private val baseCurrency: Currency,
        private val erc20Adapter: Erc20Adapter,
        private val feeRateProvider: IFeeRateProvider,
        private val rateManager: IRateManager
) : ISwapApproveService {

    private var gasPrice: Long = 0
    private var gasLimit: Long = 0

    override val feeValues = BehaviorSubject.create<DataState<Pair<CoinValue, CurrencyValue?>>>()
    override val approveState = BehaviorSubject.create<SwapApproveState>()

    private val disposables = CompositeDisposable()

    init {
        feeValues.onNext(DataState.Loading())
        approveState.onNext(SwapApproveState.ApproveNotAllowed)

        feeRateProvider.feeRates()
                .flatMap {
                    val feeRateInfo = it.first { it.priority == FeeRatePriority.HIGH }

                    gasPrice = feeRateInfo.feeRate

                    erc20Adapter.estimateApprove(spenderAddress, amount, gasPrice)
                }
                .subscribeOn(Schedulers.io())
                .subscribe({
                    gasLimit = it

                    val fee = erc20Adapter.fee(gasPrice, gasLimit)

                    val coinValue = CoinValue(feeCoin, fee)

                    val currencyValue = rateManager.getLatestRate(feeCoin.code, baseCurrency.code)?.let {
                        CurrencyValue(baseCurrency, it * fee)
                    }

                    feeValues.onNext(DataState.Success(Pair(coinValue, currencyValue)))

                    approveState.onNext(SwapApproveState.ApproveAllowed)
                }, {
                    feeValues.onNext(DataState.Error(it))

                    approveState.onNext(SwapApproveState.Error(it))
                })
                .let {
                    disposables.add(it)
                }

    }

    override fun approve() {
        approveState.onNext(SwapApproveState.Loading)

        erc20Adapter.approve(spenderAddress, amount, gasPrice, gasLimit)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    approveState.onNext(SwapApproveState.Success)
                }, {
                    approveState.onNext(SwapApproveState.Error(it))
                })
                .let {
                    disposables.add(it)
                }
    }

}
