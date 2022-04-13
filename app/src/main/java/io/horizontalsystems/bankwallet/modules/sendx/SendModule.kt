package io.horizontalsystems.bankwallet.modules.sendx

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString

object SendModule {

    @Suppress("UNCHECKED_CAST")
    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val adapter = App.adapterManager.getAdapterForWallet(wallet)


            when (adapter) {
                is ISendBitcoinAdapter -> {
                }
                is ISendDashAdapter -> {
                    TODO()
                }
                is ISendBinanceAdapter -> {
                    TODO()
                }
                is ISendZcashAdapter -> {
                    TODO()
                }
                else -> {
                    throw Exception("No adapter found!")
                }
            }


            val provider = FeeRateProviderFactory.provider(wallet.coinType)!!

            val feeService = SendBitcoinFeeService(adapter)
            val feeRateService = SendBitcoinFeeRateService(provider)
            val amountService = SendBitcoinAmountService(adapter, wallet.coin.code)
            val addressService = SendBitcoinAddressService(adapter)
            val pluginService = SendBitcoinPluginService(App.localStorage, wallet.coinType)
            return SendBitcoinViewModel(adapter, wallet, feeRateService, feeService, amountService, addressService, pluginService)  as T
        }
    }

}

sealed class SendResult {
    object Sending : SendResult()
    object Sent : SendResult()
    class Failed(val caution: HSCaution) : SendResult()
}

object SendErrorFetchFeeRateFailed : HSCaution(
    TranslatableString.ResString(R.string.Send_Error_FetchFeeRateFailed),
    Type.Error
)

object SendWarningLowFee : HSCaution(
    TranslatableString.ResString(R.string.Send_Warning_LowFee),
    Type.Warning,
    TranslatableString.ResString(R.string.Send_Warning_LowFee_Description)
)

class SendErrorInsufficientBalance(coinCode: Any) : HSCaution(
    TranslatableString.ResString(R.string.Swap_ErrorInsufficientBalance),
    Type.Error,
    TranslatableString.ResString(
        R.string.EthereumTransaction_Error_InsufficientBalanceForFee,
        coinCode
    )
)

class SendErrorMinimumSendAmount(amount: Any) : HSCaution(
    TranslatableString.ResString(R.string.Send_Error_MinimumAmount, amount)
)

class SendErrorMaximumSendAmount(amount: Any) : HSCaution(
    TranslatableString.ResString(R.string.Send_Error_MaximumAmount, amount)
)
