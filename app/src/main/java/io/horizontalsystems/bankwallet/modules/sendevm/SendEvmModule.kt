package io.horizontalsystems.bankwallet.modules.sendevm

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapModule
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.PlatformCoin
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.BigInteger


data class SendEvmData(
    val transactionData: TransactionData,
    val additionalInfo: AdditionalInfo? = null,
    val warnings: List<Warning> = listOf()
) {
    sealed class AdditionalInfo : Parcelable {
        @Parcelize
        class Send(val info: SendInfo) : AdditionalInfo()

        @Parcelize
        class Uniswap(val info: UniswapInfo) : AdditionalInfo()

        @Parcelize
        class OneInchSwap(val info: OneInchSwapInfo) : AdditionalInfo()

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
        val gasPrice: String? = null,
    ) : Parcelable

    @Parcelize
    data class OneInchSwapInfo(
        val coinFrom: PlatformCoin,
        val coinTo: PlatformCoin,
        val amountFrom: BigDecimal,
        val estimatedAmountTo: BigDecimal,
        val slippage: BigDecimal,
        val recipient: Address?
    ) : Parcelable
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
        constructor(transactionData: TransactionData) : this(
            transactionData.to.hex,
            transactionData.value,
            transactionData.input,
            transactionData.nonce
        )
    }


    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val adapter = App.adapterManager.getAdapterForWallet(wallet) as ISendEthereumAdapter
            val service = SendEvmService(wallet.platformCoin, adapter)

            return SendEvmViewModel(service, listOf(service)) as T
        }
    }

}
