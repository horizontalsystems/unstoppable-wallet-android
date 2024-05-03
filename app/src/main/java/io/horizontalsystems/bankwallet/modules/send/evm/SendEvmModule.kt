package io.horizontalsystems.bankwallet.modules.send.evm

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.isNative
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.ethereumkit.models.TransactionData
import kotlinx.parcelize.Parcelize
import java.math.BigInteger
import java.math.RoundingMode


data class SendEvmData(
    val transactionData: TransactionData,
    val additionalInfo: AdditionalInfo? = null
) {
    sealed class AdditionalInfo : Parcelable {
        @Parcelize
        class Send(val info: SendInfo) : AdditionalInfo()

        val sendInfo: SendInfo?
            get() = (this as? Send)?.info
    }

    @Parcelize
    data class SendInfo(
        val nftShortMeta: NftShortMeta? = null
    ) : Parcelable

    @Parcelize
    data class NftShortMeta(
        val nftName: String,
        val previewImageUrl: String?
    ) : Parcelable
}

object SendEvmModule {

    @Parcelize
    data class TransactionDataParcelable(
        val toAddress: String,
        val value: BigInteger,
        val input: ByteArray
    ) : Parcelable {
        constructor(transactionData: TransactionData) : this(
            transactionData.to.hex,
            transactionData.value,
            transactionData.input
        )
    }


    class Factory(private val wallet: Wallet, private val predefinedAddress: String?) : ViewModelProvider.Factory {
        val adapter = (App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter) ?: throw IllegalArgumentException("SendEthereumAdapter is null")

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendEvmViewModel::class.java -> {
                    val amountValidator = AmountValidator()
                    val coinMaxAllowedDecimals = wallet.token.decimals

                    val amountService = SendAmountService(
                        amountValidator,
                        wallet.token.coin.code,
                        adapter.balanceData.available.setScale(coinMaxAllowedDecimals, RoundingMode.DOWN),
                        wallet.token.type.isNative
                    )
                    val addressService = SendEvmAddressService(predefinedAddress)
                    val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)

                    SendEvmViewModel(
                        wallet,
                        wallet.token,
                        adapter,
                        xRateService,
                        amountService,
                        addressService,
                        coinMaxAllowedDecimals,
                        predefinedAddress == null,
                        App.connectivityManager,
                    ) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }
}
