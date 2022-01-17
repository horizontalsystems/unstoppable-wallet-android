package io.horizontalsystems.bankwallet.modules.sendevmtransaction.feesettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItemFactory
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinService
import io.horizontalsystems.bankwallet.core.ethereum.IEvmTransactionFeeService

object FeeSettingsModule {
    class Factory(
        private val feeService: IEvmTransactionFeeService,
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

data class FeeStatusViewItem(val fee: String, val gasLimit: String)

data class SendFeeSliderViewItem(val initialValue: Long, val range: LongRange, val unit: String)