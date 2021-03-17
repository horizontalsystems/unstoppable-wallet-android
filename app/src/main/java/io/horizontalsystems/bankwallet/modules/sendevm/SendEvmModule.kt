package io.horizontalsystems.bankwallet.modules.sendevm

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinService
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchServiceNew
import io.horizontalsystems.bankwallet.core.fiat.FiatServiceNew
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.AddressResolutionService
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.RecipientAddressViewModel
import io.horizontalsystems.ethereumkit.models.TransactionData
import kotlinx.android.parcel.Parcelize
import java.math.BigInteger


data class SendEvmData(
        val transactionData: TransactionData,
        val additionalItems: List<AdditionalItem> = listOf()
) {
    sealed class AdditionalItem : Parcelable {
        @Parcelize
        class Domain(val value: String) : AdditionalItem()
    }
}

object SendEvmModule {

    const val walletKey = "walletKey"
    const val transactionDataKey = "transactionData"
    const val additionalItemsKey = "additionalItems"

    @Parcelize
    data class TransactionDataParcelable(
            val toAddress: String,
            val value: BigInteger,
            val input: ByteArray
    ) : Parcelable {
        constructor(transactionData: TransactionData) : this(transactionData.to.hex, transactionData.value, transactionData.input)
    }


    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        private val adapter by lazy { App.adapterManager.getAdapterForWallet(wallet) as ISendEthereumAdapter }
        private val service by lazy { SendEvmService(wallet.coin, adapter) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendEvmViewModel::class.java -> {
                    SendEvmViewModel(service) as T
                }
                AmountInputViewModel::class.java -> {
                    val switchService = AmountTypeSwitchServiceNew()
                    val fiatService = FiatServiceNew(switchService, App.currencyManager, App.xRateManager)
                    switchService.add(fiatService.toggleAvailableObservable)

                    AmountInputViewModel(service, fiatService, switchService) as T
                }
                SendAvailableBalanceViewModel::class.java -> {
                    val coinService = EvmCoinService(wallet.coin, App.currencyManager, App.xRateManager)
                    SendAvailableBalanceViewModel(service, coinService) as T
                }
                RecipientAddressViewModel::class.java -> {
                    val addressParser = App.addressParserFactory.parser(wallet.coin)
                    val resolutionService = AddressResolutionService(wallet.coin.code, true)
                    val stringProvider = StringProvider()
                    val placeholder = stringProvider.string(R.string.SwapSettings_RecipientPlaceholder)
                    RecipientAddressViewModel(service, resolutionService, addressParser, placeholder) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

}
