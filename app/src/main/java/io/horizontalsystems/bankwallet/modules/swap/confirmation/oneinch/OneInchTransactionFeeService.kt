package io.horizontalsystems.bankwallet.modules.swap.confirmation.oneinch

import android.os.Parcelable
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionService.*
import io.horizontalsystems.bankwallet.core.ethereum.IEvmTransactionFeeService
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.swap.oneinch.OneInchKitHelper
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
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

@Parcelize
data class OneInchSwapParameters(
        val coinFrom: Coin,
        val coinTo: Coin,
        val amountFrom: BigDecimal,
        val amountTo: BigDecimal,
        val slippage: BigDecimal,
        val recipient: String? = null
) : Parcelable

class OneInchTransactionFeeService(
        private val oneInchKitHelper: OneInchKitHelper,
        parameters: OneInchSwapParameters,
        private val feeRateProvider: IFeeRateProvider
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
    override val warningOfStuckObservable: Flowable<Boolean> = warningOfStuckSubject.toFlowable(BackpressureStrategy.BUFFER)

    var parameters: OneInchSwapParameters = parameters
        private set

    private var disposable: Disposable? = null

    private fun sync() {
        disposable?.dispose()

        transactionStatus = DataState.Loading

        getGasPriceAsync(gasPriceType)
                .flatMap { gasPrice ->
                    oneInchKitHelper.getSwapAsync(
                            fromCoin = parameters.coinFrom,
                            toCoin = parameters.coinTo,
                            fromAmount = parameters.amountFrom,
                            recipient = parameters.recipient?.let { Address(it) },
                            slippagePercentage = parameters.slippage.toFloat(),
                            gasPrice = gasPrice.orElse(null)?.toLong()
                    )
                }
                .subscribeIO({ swap ->
                    val swapTx = swap.transaction
                    val gasData = GasData(
                            estimatedGasLimit = swapTx.gasLimit,
                            gasLimit = getSurchargedGasLimit(swapTx.gasLimit),
                            gasPrice = swapTx.gasPrice
                    )
                    recommendedGasPrice = swapTx.gasPrice.toBigInteger()

                    parameters = parameters.copy(amountTo = swap.toTokenAmount.toBigDecimal().movePointLeft(swap.toToken.decimals).stripTrailingZeros())

                    val transactionData = TransactionData(swapTx.to, swapTx.value, swapTx.data)
                    transactionStatus = DataState.Success(Transaction(transactionData, gasData))

                }, {
                    parameters = parameters.copy(amountTo = BigDecimal.ZERO)

                    transactionStatus = DataState.Error(it)
                })
                .let { disposable = it }
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