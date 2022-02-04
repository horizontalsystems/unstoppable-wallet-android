package io.horizontalsystems.bankwallet.modules.swap.confirmation.oneinch

import android.os.Parcelable
import io.horizontalsystems.bankwallet.core.EvmError
import io.horizontalsystems.bankwallet.core.ICustomRangedFeeProvider
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.*
import io.horizontalsystems.bankwallet.modules.swap.oneinch.OneInchKitHelper
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.horizontalsystems.oneinchkit.Swap
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.TimeUnit

@Parcelize
data class OneInchSwapParameters(
    val coinFrom: PlatformCoin,
    val coinTo: PlatformCoin,
    val amountFrom: BigDecimal,
    val amountTo: BigDecimal,
    val slippage: BigDecimal,
    val recipient: Address? = null
) : Parcelable

class OneInchFeeService(
    private val oneInchKitHelper: OneInchKitHelper,
    parameters: OneInchSwapParameters,
    private val feeRateProvider: ICustomRangedFeeProvider
) : IEvmFeeService {

    override val gasPriceService: IEvmGasPriceService
        get() = TODO("Not yet implemented")

    private val gasLimitSurchargePercent: Int = 25

    override var transactionStatus: DataState<Transaction> = DataState.Error(GasDataError.NoTransactionData)
        private set(value) {
            field = value
            transactionStatusSubject.onNext(value)
        }

    private val transactionStatusSubject = PublishSubject.create<DataState<Transaction>>()
    override val transactionStatusObservable: Observable<DataState<Transaction>> = transactionStatusSubject

    var parameters: OneInchSwapParameters = parameters
        private set

    private var disposable: Disposable? = null

    private var retryDelayTimeInSeconds = 3L
    private var retryDisposable: Disposable? = null

//    override fun setTransactionData(transactionData: TransactionData) {
//        TODO("not implemented")
//    }

    private fun sync() {
        disposable?.dispose()
        retryDisposable?.dispose()

        transactionStatus = DataState.Loading

//        getGasPriceAsync(gasPriceType)
//            .flatMap { gasPrice ->
//                oneInchKitHelper.getSwapAsync(
//                    fromCoin = parameters.coinFrom,
//                    toCoin = parameters.coinTo,
//                    fromAmount = parameters.amountFrom,
//                    recipient = parameters.recipient?.hex,
//                    slippagePercentage = parameters.slippage.toFloat(),
//                    gasPrice = gasPrice.orElse(null)?.toLong()
//                )
//            }
//            .subscribeIO({ swap ->
//                sync(swap)
//
//            }, {
//                onError(it)
//            })
//            .let { disposable = it }
    }

    private fun sync(swap: Swap) {
        val swapTx = swap.transaction
        val gasData = GasData(
            gasLimit = getSurchargedGasLimit(swapTx.gasLimit),
            gasPrice = GasPrice.Legacy(swapTx.gasPrice),

            )
//        if (gasPriceType == GasPriceType.Recommended) {
//            recommendedGasPrice = swapTx.gasPrice.toBigInteger()
//        }

        parameters = parameters.copy(
            amountTo = swap.toTokenAmount.toBigDecimal().movePointLeft(swap.toToken.decimals).stripTrailingZeros()
        )

        val transactionData = TransactionData(swapTx.to, swapTx.value, swapTx.data)
        transactionStatus = DataState.Success(Transaction(transactionData, gasData))
    }

    private fun onError(error: Throwable) {
        parameters = parameters.copy(amountTo = BigDecimal.ZERO)
        transactionStatus = DataState.Error(error)

        if (error is EvmError.CannotEstimateSwap) {
            retryDisposable = Single.timer(retryDelayTimeInSeconds, TimeUnit.SECONDS)
                .subscribeIO({
                    sync()
                }, {

                })
        }
    }

    private var recommendedGasPrice: BigInteger? = null

//    private fun getGasPriceAsync(gasPriceType: GasPriceType): Single<Optional<BigInteger>> {
//        var recommendedGasPriceSingle = feeRateProvider.feeRate(FeeRatePriority.RECOMMENDED)
//            .doOnSuccess { gasPrice ->
//                recommendedGasPrice = gasPrice
//            }
//
//        return when (gasPriceType) {
//            is GasPriceType.Recommended -> { // return null for 1inch API to use "fast gasPrice from network"
////                warningOfStuckSubject.onNext(false)
//                Single.just(Optional.ofNullable(null))
//            }
//            is GasPriceType.Custom -> {
//                recommendedGasPrice?.let {
//                    recommendedGasPriceSingle = Single.just(it)
//                }
//                recommendedGasPriceSingle.map { recommended ->
//                    val customGasPrice = gasPriceType.gasPrice.value.toBigInteger()
////                    warningOfStuckSubject.onNext(customGasPrice < recommended)
//                    Optional.ofNullable(customGasPrice)
//                }
//            }
//        }
//    }

    private fun getSurchargedGasLimit(estimatedGasLimit: Long): Long {
        return (estimatedGasLimit + estimatedGasLimit / 100.0 * gasLimitSurchargePercent).toLong()
    }

}
