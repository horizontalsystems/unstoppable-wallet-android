package io.horizontalsystems.bankwallet.modules.sendevm

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinService
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchServiceSendEvm
import io.horizontalsystems.bankwallet.core.fiat.FiatServiceSendEvm
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.swap.settings.AddressResolutionService
import io.horizontalsystems.bankwallet.modules.swap.settings.RecipientAddressViewModel
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapModule
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.PlatformCoin
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.math.BigInteger


data class SendEvmData(
        val transactionData: TransactionData,
        val additionalInfo: AdditionalInfo? = null
) {
    sealed class AdditionalInfo : Parcelable {
        @Parcelize
        class Send(val info: SendInfo) : AdditionalInfo()

        @Parcelize
        class Uniswap(val info: UniswapInfo) : AdditionalInfo()

        @Parcelize
        class OneInchSwap(val info: OneInchSwapInfo): AdditionalInfo()

        val sendInfo: SendInfo?
            get() = (this as? Send)?.info

        val uniswapInfo: UniswapInfo?
            get() = (this as? Uniswap)?.info

        val oneInchSwapInfo: OneInchSwapInfo?
            get() = (this as? OneInchSwap)?.info
    }

    @Parcelize
    data class SendInfo(
            val domain: String?
    ) : Parcelable

    @Parcelize
    data class UniswapInfo(
        val estimatedOut: BigDecimal,
        val estimatedIn: BigDecimal,
        val slippage: String? = null,
        val deadline: String? = null,
        val recipientDomain: String? = null,
        val price: String? = null,
        val priceImpact: UniswapModule.PriceImpactViewItem? = null,
        val priceImpactWarning: Boolean,
        val gasPrice: String? = null,
    ) : Parcelable

    @Parcelize
    data class OneInchSwapInfo(
        val coinTo: PlatformCoin,
        val estimatedAmountTo: BigDecimal,
        val slippage: String? = null,
        val recipientDomain: String? = null
    ): Parcelable
}

object SendEvmModule {

    const val walletKey = "walletKey"
    const val transactionDataKey = "transactionData"
    const val additionalInfoKey = "additionalInfo"

    @Parcelize
    data class TransactionDataParcelable(
            val toAddress: String,
            val value: BigInteger,
            val input: ByteArray,
            val nonce: Long? = null
    ) : Parcelable {
        constructor(transactionData: TransactionData) : this(transactionData.to.hex, transactionData.value, transactionData.input, transactionData.nonce)
    }


    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        private val adapter by lazy { App.adapterManager.getAdapterForWallet(wallet) as ISendEthereumAdapter }
        private val service by lazy { SendEvmService(wallet.platformCoin, adapter) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendEvmViewModel::class.java -> {
                    SendEvmViewModel(service, listOf(service)) as T
                }
                AmountInputViewModel::class.java -> {
                    val switchService = AmountTypeSwitchServiceSendEvm()
                    val fiatService = FiatServiceSendEvm(switchService, App.currencyManager, App.marketKit)
                    switchService.add(fiatService.toggleAvailableObservable)

                    AmountInputViewModel(service, fiatService, switchService, clearables = listOf(service, fiatService, switchService)) as T
                }
                SendAvailableBalanceViewModel::class.java -> {
                    val coinService = EvmCoinService(wallet.platformCoin, App.currencyManager, App.marketKit)
                    SendAvailableBalanceViewModel(service, coinService, listOf(service, coinService)) as T
                }
                RecipientAddressViewModel::class.java -> {
                    val addressParser = App.addressParserFactory.parser(wallet.coinType)
                    val resolutionService = AddressResolutionService(wallet.coin.code, true)
                    val placeholder = Translator.getString(R.string.SwapSettings_RecipientPlaceholder)
                    RecipientAddressViewModel(service, resolutionService, addressParser, placeholder, listOf(service, resolutionService)) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

}
