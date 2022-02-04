package io.horizontalsystems.bankwallet.modules.evmfee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItemFactory
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinService
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.legacy.LegacyFeeSettingsViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.legacy.LegacyGasPriceService
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.reactivex.Observable
import java.math.BigInteger

object EvmFeeModule {
    class Factory(
        private val feeService: IEvmFeeService,
        private val evmCoinService: EvmCoinService
    ) : ViewModelProvider.Factory {

        private val cautionViewItemFactory by lazy { CautionViewItemFactory(evmCoinService) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (val gasPriceService = feeService.gasPriceService) {
                is LegacyGasPriceService -> LegacyFeeSettingsViewModel(
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
    val gasPriceService: IEvmGasPriceService
    val transactionStatus: DataState<Transaction>
    val transactionStatusObservable: Observable<DataState<Transaction>>
}

interface IEvmGasPriceService {
    val state: DataState<GasPriceInfo>
    val stateObservable: Observable<DataState<GasPriceInfo>>
}

abstract class FeeSettingsError : Throwable() {
    object InsufficientBalance : FeeSettingsError()
    object LowBaseFee : FeeSettingsError()
}

abstract class FeeSettingsWarning : Warning() {
    object HighBaseFeeWarning : FeeSettingsWarning()
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
        get() = gasLimit.toBigInteger() * gasPrice.value.toBigInteger()
}

sealed class GasPrice {
    class Legacy(
        val gasPrice: Long
    ) : GasPrice()

    class Eip1559(
        val baseFee: Long,
        val maxFeePerGas: Long,
        val maxPriorityFeePerGas: Long,
    ) : GasPrice()

    val value: Long
        get() = when (this) {
            is Eip1559 -> maxFeePerGas + maxPriorityFeePerGas
            is Legacy -> gasPrice
        }
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

sealed class GasDataError : Error() {
    object NoTransactionData : GasDataError()
    object InsufficientBalance : GasDataError()
}

data class FeeStatusViewItem(val fee: String, val gasLimit: String)

data class SendFeeSliderViewItem(val initialValue: Long, val range: LongRange, val unit: String)
