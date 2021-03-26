package io.horizontalsystems.bankwallet.core.ethereum

import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger

class EvmTransactionService(
        private val evmKit: EthereumKit,
        private val feeRateProvider: IFeeRateProvider,
        private val gasLimitSurchargePercent: Int
) {

    val hasEstimatedFee: Boolean = gasLimitSurchargePercent != 0

    var transactionData: TransactionData? = null
        set(value) {
            field = value
            sync()
        }

    var gasPriceType: GasPriceType = GasPriceType.Recommended
        set(value) {
            field = value
            gasPriceTypeSubject.onNext(value)
            sync()
        }

    private val gasPriceTypeSubject = PublishSubject.create<GasPriceType>()
    val gasPriceTypeObservable: Observable<GasPriceType> = gasPriceTypeSubject

    var transactionStatus: DataState<Transaction> = DataState.Error(GasDataError.NoTransactionData)
        set(value) {
            field = value
            transactionStatusSubject.onNext(value)
        }
    private val transactionStatusSubject = PublishSubject.create<DataState<Transaction>>()
    val transactionStatusObservable: Observable<DataState<Transaction>> = transactionStatusSubject

    private val disposable = CompositeDisposable()

    private val evmBalance: BigInteger
        get() = evmKit.accountState?.balance ?: BigInteger.ZERO

    fun onCleared() {
        disposable.clear()
    }

    private fun sync() {
        disposable.clear()

        val transactionData = this.transactionData
        if (transactionData == null) {
            transactionStatus = DataState.Error(GasDataError.NoTransactionData)
            return
        }

        transactionStatus = DataState.Loading

        getGasPriceAsync(gasPriceType)
                .flatMap { gasPrice ->
                    getTransactionAsync(gasPrice, transactionData)
                }
                .subscribeIO({
                    transactionStatus = DataState.Success(it)
                }, {
                    transactionStatus = DataState.Error(it)
                })
                .let {
                    disposable.add(it)
                }
    }

    private fun getTransactionAsync(gasPrice: BigInteger, transactionData: TransactionData): Single<Transaction> {
        return getAdjustedTransactionDataAsync(gasPrice, transactionData)
                .flatMap { adjustedTransactionData ->
                    getGasLimitAsync(gasPrice, adjustedTransactionData)
                            .map { estimatedGasLimit ->
                                val gasLimit = getSurchargedGasLimit(estimatedGasLimit)
                                Transaction(adjustedTransactionData, GasData(estimatedGasLimit, gasLimit, gasPrice.toLong()))
                            }
                }
    }

    private fun getAdjustedTransactionDataAsync(gasPrice: BigInteger, transactionData: TransactionData): Single<TransactionData> {
        if (transactionData.input.isEmpty() && transactionData.value == evmBalance) {
            val stubTransactionData = TransactionData(transactionData.to, BigInteger.ONE, byteArrayOf())
            return getGasLimitAsync(gasPrice, stubTransactionData)
                    .flatMap { estimatedGasLimit ->
                        val gasLimit = getSurchargedGasLimit(estimatedGasLimit)
                        val adjustedValue = transactionData.value - gasLimit.toBigInteger() * gasPrice

                        if (adjustedValue <= BigInteger.ZERO) {
                            Single.error(GasDataError.InsufficientBalance)
                        } else {
                            val adjustedTransactionData = TransactionData(transactionData.to, adjustedValue, byteArrayOf())
                            Single.just(adjustedTransactionData)
                        }
                    }
        } else {
            return Single.just(transactionData)
        }
    }

    private fun getGasPriceAsync(gasPriceType: GasPriceType): Single<BigInteger> {
        return when (gasPriceType) {
            is GasPriceType.Recommended -> {
                feeRateProvider.feeRate(FeeRatePriority.RECOMMENDED)
            }
            is GasPriceType.Custom -> {
                Single.just(gasPriceType.gasPrice.toBigInteger())
            }
        }
    }

    private fun getGasLimitAsync(gasPrice: BigInteger, transactionData: TransactionData): Single<Long> {
        return evmKit.estimateGas(transactionData, gasPrice.toLong())
    }

    private fun getSurchargedGasLimit(estimatedGasLimit: Long): Long {
        return (estimatedGasLimit + estimatedGasLimit / 100.0 * gasLimitSurchargePercent).toLong()
    }

    // types

    data class GasData(
            val estimatedGasLimit: Long,
            val gasLimit: Long,
            val gasPrice: Long
    ) {
        val estimatedFee: BigInteger
            get() = estimatedGasLimit.toBigInteger() * gasPrice.toBigInteger()

        val fee: BigInteger
            get() = gasLimit.toBigInteger() * gasPrice.toBigInteger()
    }

    data class Transaction(
            val data: TransactionData,
            val gasData: GasData
    ) {
        val totalAmount: BigInteger
            get() = data.value + gasData.fee
    }

    sealed class GasPriceType {
        object Recommended : GasPriceType()
        class Custom(val gasPrice: Long) : GasPriceType()
    }

    sealed class GasDataError : Error() {
        object NoTransactionData : GasDataError()
        object InsufficientBalance : GasDataError()
    }

}
