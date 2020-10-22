package io.horizontalsystems.bankwallet.core.ethereum

import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.providers.EthereumFeeRateProvider
import io.horizontalsystems.bankwallet.modules.swap.DataState
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger

class EthereumTransactionService(
        private val ethereumKit: EthereumKit,
        private val feeRateProvider: EthereumFeeRateProvider
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
                            .map { gasLimit ->
                                Transaction(transactionData, GasData(gasLimit, gasPrice))
                            }
                }
                .subscribe({
                    transactionStatus = DataState.Success(it)
                }, {
                    transactionStatus = DataState.Error(it)
                })
                .let {
                    disposable.add(it)
                }
    }

    private fun gasPriceSingle(gasPriceType: GasPriceType): Single<Long> {
        return when (gasPriceType) {
            is GasPriceType.Recommended -> {
                feeRateProvider.feeRates()
                        .map {
                            val feeRateInfo = it.first { it.priority == FeeRatePriority.MEDIUM }

                            feeRateInfo.feeRate
                        }
            }
            is GasPriceType.Custom -> {
                Single.just(gasPriceType.gasPrice)
            }
        }
    }

    private fun gasLimitSingle(gasPrice: Long, transactionData: TransactionData): Single<Long> {
        // todo: make "to" optional in EthereumKit
        return ethereumKit.estimateGas(transactionData.to!!, transactionData.value, gasPrice, transactionData.input)
    }

    // types

    data class TransactionData(
            var to: Address?,
            var value: BigInteger?,
            var input: ByteArray
    ) {
        val amount: BigInteger
            get() = value ?: BigInteger.ZERO
    }

    data class GasData(
            val gasLimit: Long,
            val gasPrice: Long
    ) {
        val fee: BigInteger
            get() = gasLimit.toBigInteger() * gasPrice.toBigInteger()
    }

    data class Transaction(
            val data: TransactionData,
            val gasData: GasData
    ) {
        val totalAmount: BigInteger
            get() = data.amount + gasData.fee
    }

    sealed class GasPriceType {
        object Recommended : GasPriceType()
        class Custom(val gasPrice: Long) : GasPriceType()
    }

    sealed class GasDataError : Error() {
        object NoTransactionData : GasDataError()
    }
}
