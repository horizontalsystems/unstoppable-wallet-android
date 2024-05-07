package io.horizontalsystems.bankwallet.modules.evmfee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.ServiceState
import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinService
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.FeePriceScale
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.evmfee.eip1559.Eip1559FeeSettingsViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.eip1559.Eip1559GasPriceService
import io.horizontalsystems.bankwallet.modules.evmfee.legacy.LegacyFeeSettingsViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.legacy.LegacyGasPriceService
import io.horizontalsystems.bankwallet.modules.fee.FeeItem
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

object EvmFeeModule {
    private const val surchargePercent = 10

    fun surcharged(gasLimit: Long) : Long {
        return (gasLimit + gasLimit / 100.0 * surchargePercent).toLong()
    }

    class Factory(
        private val feeService: IEvmFeeService,
        private val gasPriceService: IEvmGasPriceService,
        private val evmCoinService: EvmCoinService
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (gasPriceService) {
                is LegacyGasPriceService ->
                    LegacyFeeSettingsViewModel(
                        gasPriceService,
                        feeService,
                        evmCoinService
                    ) as T
                is Eip1559GasPriceService ->
                    Eip1559FeeSettingsViewModel(
                        gasPriceService,
                        feeService,
                        evmCoinService
                    ) as T
                else -> throw IllegalArgumentException()
            }
        }
    }
}

interface IEvmFeeService {
    val transactionStatusFlow: Flow<DataState<Transaction>>

    fun clear()
    fun reset()
}

abstract class IEvmGasPriceService : ServiceState<DataState<GasPriceInfo>>() {
    abstract fun setRecommended()
    abstract fun start()
}

abstract class FeeSettingsError : Throwable() {
    object InsufficientBalance : FeeSettingsError()
    object UsedNonce : FeeSettingsError()
    class InvalidGasPriceType(override val message: String) : FeeSettingsError()
}

abstract class FeeSettingsWarning : Warning() {
    object RiskOfGettingStuck : FeeSettingsWarning()
    object RiskOfGettingStuckLegacy : FeeSettingsWarning()
    object Overpricing : FeeSettingsWarning()
}

data class GasPriceInfo(
    val gasPrice: GasPrice,
    val gasPriceDefault: GasPrice,
    val default: Boolean,
    val warnings: List<Warning>,
    val errors: List<Throwable>
)

open class GasData(
    val gasLimit: Long,
    val estimatedGasLimit: Long = gasLimit,
    var gasPrice: GasPrice
) {
    open val fee: BigInteger
        get() = gasLimit.toBigInteger() * gasPrice.max.toBigInteger()

    open val estimatedFee: BigInteger
        get() = estimatedGasLimit.toBigInteger() * gasPrice.max.toBigInteger()

    val isSurcharged: Boolean
        get() = gasLimit != estimatedGasLimit
}

class RollupGasData(
    gasLimit: Long,
    estimatedGasLimit: Long = gasLimit,
    gasPrice: GasPrice,
    val l1Fee: BigInteger
) : GasData(
    gasLimit = gasLimit,
    estimatedGasLimit = estimatedGasLimit,
    gasPrice = gasPrice
) {
    override val fee: BigInteger
        get() = super.fee + l1Fee

    override val estimatedFee: BigInteger
        get() = super.estimatedFee + l1Fee
}

//TODO rename to FeeData
data class Transaction(
    val transactionData: TransactionData,
    val gasData: GasData,
    val default: Boolean,
    val warnings: List<Warning> = listOf(),
    val errors: List<Throwable> = listOf()
) {
    val totalAmount: BigInteger
        get() = transactionData.value + gasData.fee
}

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

sealed class GasDataError : Error() {
    object NoTransactionData : GasDataError()
}

data class FeeSummaryViewItem(val fee: FeeItem?, val gasLimit: String, val viewState: ViewState)

data class FeeViewItem(
    val weiValue: Long,
    val scale: FeePriceScale,
    val warnings: List<Warning>,
    val errors: List<Throwable>
) {

    fun wei(scaledValue: BigDecimal): Long {
        return (scaledValue * BigDecimal(scale.scaleValue)).toLong()
    }
}

internal val BlockchainType.l1GasFeeContractAddress: Address?
    get() =
        when (this) {
            BlockchainType.Optimism -> Address("0x420000000000000000000000000000000000000F")
            else -> null
        }
