package cash.p.terminal.modules.evmfee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.Warning
import cash.p.terminal.core.ethereum.CautionViewItemFactory
import cash.p.terminal.core.ethereum.EvmCoinService
import cash.p.terminal.entities.DataState
import cash.p.terminal.entities.FeePriceScale
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.evmfee.eip1559.Eip1559FeeSettingsViewModel
import cash.p.terminal.modules.evmfee.eip1559.Eip1559GasPriceService
import cash.p.terminal.modules.evmfee.legacy.LegacyFeeSettingsViewModel
import cash.p.terminal.modules.evmfee.legacy.LegacyGasPriceService
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.Observable
import kotlinx.coroutines.flow.StateFlow
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
    val recommendedGasPriceSelectedFlow: StateFlow<Boolean>

    fun setRecommended()
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

open class GasData(val gasLimit: Long, val gasPrice: GasPrice) {
    open val fee: BigInteger
        get() = gasLimit.toBigInteger() * gasPrice.max.toBigInteger()
}

class RollupGasData(gasLimit: Long, gasPrice: GasPrice, val l1Fee: BigInteger) : GasData(gasLimit, gasPrice) {
    override val fee: BigInteger
        get() = super.fee + l1Fee
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

data class FeeSummaryViewItem(val fee: String, val gasLimit: String, val viewState: ViewState)

data class FeeViewItem(
    val weiValue: Long,
    val scale: FeePriceScale
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
