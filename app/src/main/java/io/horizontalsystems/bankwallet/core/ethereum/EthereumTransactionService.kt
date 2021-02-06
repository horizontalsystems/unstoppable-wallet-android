package io.horizontalsystems.bankwallet.core.ethereum

import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.providers.EthereumFeeRateProvider
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger

class EthereumTransactionService(
        private val ethereumKit: EthereumKit,
        private val feeRateProvider: EthereumFeeRateProvider,
        private val gasLimitSurchargePercent: Int
) {

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

    fun resync() {
        sync()
    }

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

        gasPriceSingle(gasPriceType)
                .flatMap { gasPrice ->
                    gasLimitSingle(gasPrice, transactionData)
                            .map { estimatedGasLimit ->
                                val gasLimit = estimatedGasLimit + (estimatedGasLimit * gasLimitSurchargePercent / 100.0).toLong()
                                Transaction(transactionData, GasData(estimatedGasLimit, gasLimit, gasPrice.toLong()))
                            }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({
                    transactionStatus = DataState.Success(it)
                }, {
                    transactionStatus = DataState.Error(it)
                })
                .let {
                    disposable.add(it)
                }
    }

    private fun gasPriceSingle(gasPriceType: GasPriceType): Single<BigInteger> {
        return when (gasPriceType) {
            is GasPriceType.Recommended -> {
                feeRateProvider.feeRate(FeeRatePriority.RECOMMENDED)
            }
            is GasPriceType.Custom -> {
                Single.just(gasPriceType.gasPrice.toBigInteger())
            }
        }
    }

    private fun gasLimitSingle(gasPrice: BigInteger, transactionData: TransactionData): Single<Long> {
        return ethereumKit.estimateGas(transactionData, gasPrice.toLong())
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
    }
}
