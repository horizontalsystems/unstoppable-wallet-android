package io.horizontalsystems.bankwallet.modules.swap.confirmation.oneinch

import android.os.Parcelable
import io.horizontalsystems.bankwallet.core.EvmError
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.ICustomRangedFeeProvider
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionFeeService.*
import io.horizontalsystems.bankwallet.core.ethereum.IEvmTransactionFeeService
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.swap.oneinch.OneInchKitHelper
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.horizontalsystems.oneinchkit.Swap
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
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

class OneInchTransactionFeeService(
    private val oneInchKitHelper: OneInchKitHelper,
    parameters: OneInchSwapParameters,
    private val feeRateProvider: ICustomRangedFeeProvider
) : IEvmTransactionFeeService {

    private val gasLimitSurchargePercent: Int = 25

    override val hasEstimatedFee: Boolean = gasLimitSurchargePercent != 0

    override var transactionStatus: DataState<Transaction> = DataState.Error(GasDataError.NoTransactionData)
        private set(value) {
            field = value
            transactionStatusSubject.onNext(value)
        }

    private val transactionStatusSubject = PublishSubject.create<DataState<Transaction>>()
    override val transactionStatusObservable: Observable<DataState<Transaction>> = transactionStatusSubject

    override var gasPriceType: GasPriceType = GasPriceType.Recommended
        set(value) {
            field = value
            gasPriceTypeSubject.onNext(value)
            sync()
        }

    private val gasPriceTypeSubject = PublishSubject.create<GasPriceType>()
    override val gasPriceTypeObservable: Observable<GasPriceType> = gasPriceTypeSubject

    private var warningOfStuckSubject = PublishSubject.create<Boolean>()
    override val warningOfStuckObservable: Flowable<Boolean> =
        warningOfStuckSubject.toFlowable(BackpressureStrategy.BUFFER)

    var parameters: OneInchSwapParameters = parameters
        private set

    override val customFeeRange: LongRange
        get() = feeRateProvider.customFeeRange

    private var disposable: Disposable? = null

    private var retryDelayTimeInSeconds = 3L
    private var retryDisposable: Disposable? = null

    private fun sync() {
        disposable?.dispose()
        retryDisposable?.dispose()

        transactionStatus = DataState.Loading

        getGasPriceAsync(gasPriceType)
            .flatMap { gasPrice ->
                oneInchKitHelper.getSwapAsync(
                    fromCoin = parameters.coinFrom,
                    toCoin = parameters.coinTo,
                    fromAmount = parameters.amountFrom,
                    recipient = parameters.recipient?.hex,
                    slippagePercentage = parameters.slippage.toFloat(),
                    gasPrice = gasPrice.orElse(null)?.toLong()
                )
            }
            .subscribeIO({ swap ->
                sync(swap)

            }, {
                onError(it)
            })
            .let { disposable = it }
    }

    private fun sync(swap: Swap) {
        val swapTx = swap.transaction
        val gasData = GasData(
            estimatedGasLimit = swapTx.gasLimit,
            gasLimit = getSurchargedGasLimit(swapTx.gasLimit),
            gasPrice = swapTx.gasPrice
        )
        if (gasPriceType == GasPriceType.Recommended) {
            recommendedGasPrice = swapTx.gasPrice.toBigInteger()
        }

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

    private fun getGasPriceAsync(gasPriceType: GasPriceType): Single<Optional<BigInteger>> {
        var recommendedGasPriceSingle = feeRateProvider.feeRate(FeeRatePriority.RECOMMENDED)
            .doOnSuccess { gasPrice ->
                recommendedGasPrice = gasPrice
            }

        return when (gasPriceType) {
            is GasPriceType.Recommended -> { // return null for 1inch API to use "fast gasPrice from network"
                warningOfStuckSubject.onNext(false)
                Single.just(Optional.ofNullable(null))
            }
            is GasPriceType.Custom -> {
                recommendedGasPrice?.let {
                    recommendedGasPriceSingle = Single.just(it)
                }
                recommendedGasPriceSingle.map { recommended ->
                    val customGasPrice = gasPriceType.gasPrice.toBigInteger()
                    warningOfStuckSubject.onNext(customGasPrice < recommended)
                    Optional.ofNullable(customGasPrice)
                }
            }
        }
    }

    private fun getSurchargedGasLimit(estimatedGasLimit: Long): Long {
        return (estimatedGasLimit + estimatedGasLimit / 100.0 * gasLimitSurchargePercent).toLong()
    }

}
