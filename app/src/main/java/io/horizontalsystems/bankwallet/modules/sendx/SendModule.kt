package io.horizontalsystems.bankwallet.modules.sendx

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString

object SendModule {

    @Suppress("UNCHECKED_CAST")
    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val adapter = App.adapterManager.getAdapterForWallet(wallet) as ISendBitcoinAdapter
            val provider = FeeRateProviderFactory.provider(wallet.coinType)!!

            val feeService = FeeServiceBitcoin(adapter)
            val feeRateService = FeeRateServiceBitcoin(provider)
            val amountService = AmountService(adapter, wallet.coin.code)
            val addressService = AddressService(adapter)
            val service = SendBitcoinService(adapter, wallet, feeRateService, feeService, amountService, addressService)

            return SendViewModel(service) as T
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
