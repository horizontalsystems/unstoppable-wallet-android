package io.horizontalsystems.bankwallet.modules.evmfee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItemFactory
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinService
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.eip1559.Eip1559FeeSettingsViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.eip1559.Eip1559GasPriceService
import io.horizontalsystems.bankwallet.modules.evmfee.legacy.LegacyFeeSettingsViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.legacy.LegacyGasPriceService
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.reactivex.Observable
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

object EvmFeeModule {
    class Factory(
        private val feeService: IEvmFeeService,
        private val gasPriceService: IEvmGasPriceService,
        private val evmCoinService: EvmCoinService
    ) : ViewModelProvider.Factory {

        private val cautionViewItemFactory by lazy { CautionViewItemFactory(evmCoinService) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (gasPriceService) {
                is LegacyGasPriceService ->
                    LegacyFeeSettingsViewModel(
                        gasPriceService,
                        feeService,
                        evmCoinService,
                        cautionViewItemFactory
                    ) as T
                is Eip1559GasPriceService ->
                    Eip1559FeeSettingsViewModel(
                        gasPriceService,
                        feeService,
                        evmCoinService,
                        cautionViewItemFactory
                    ) as T
                else -> throw IllegalArgumentException()
            }
        }
    }
}

interface IEvmFeeService {
    val transactionStatus: DataState<Transaction>
    val transactionStatusObservable: Observable<DataState<Transaction>>
}

interface IEvmGasPriceService {
    val state: DataState<GasPriceInfo>
    val stateObservable: Observable<DataState<GasPriceInfo>>
    val isRecommendedGasPriceSelected: Boolean
}

abstract class FeeSettingsError : Throwable() {
    object InsufficientBalance : FeeSettingsError()
    object LowMaxFee : FeeSettingsError()
    class InvalidGasPriceType(override val message: String) : FeeSettingsError()
}

abstract class FeeSettingsWarning : Warning() {
    object RiskOfGettingStuck : FeeSettingsWarning()
    object Overpricing : FeeSettingsWarning()
}

data class GasPriceInfo(
    val gasPrice: GasPrice,
    val warnings: List<Warning>,
    val errors: List<Throwable>
)

data class GasData(
    val gasLimit: Long,
    val gasPrice: GasPrice
) {
    val fee: BigInteger
        get() = gasLimit.toBigInteger() * gasPrice.max.toBigInteger()
}

data class Transaction(
    val transactionData: TransactionData,
    val gasData: GasData,
    val warnings: List<Warning> = listOf(),
    val errors: List<Throwable> = listOf()
) {
    val totalAmount: BigInteger
        get() = transactionData.value + gasData.fee
}

data class FeeRangeConfig(
    val lowerBound: Bound,
    val upperBound: Bound
) {
    sealed class Bound {
        class Fixed(val value: Long) : Bound()
        class Multiplied(val multiplier: BigDecimal) : Bound()
        class Added(val addend: Long) : Bound()

        fun calculate(selectedValue: Long) = when (this) {
            is Added -> selectedValue + addend
            is Fixed -> value
            is Multiplied -> {
                (BigDecimal(selectedValue) * multiplier)
                    .setScale(0, RoundingMode.HALF_UP)
                    .toLong()
            }
        }
    }
}

sealed class GasDataError : Error() {
    object NoTransactionData : GasDataError()
    object InsufficientBalance : GasDataError()
}

data class FeeViewItem(val fee: String, val gasLimit: String)

data class SliderViewItem(val initialValue: Long, val range: LongRange, val unit: String)
