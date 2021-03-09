package io.horizontalsystems.bankwallet.modules.swap.confirmation

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ethereum.CoinService
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionService
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionService
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.swap.SwapService
import io.horizontalsystems.ethereumkit.models.TransactionData
import kotlinx.android.parcel.Parcelize
import java.math.BigInteger

object SwapConfirmationModule {

    const val transactionDataKey = "transactionData"

    @Parcelize
    data class TransactionDataParcelable(
            val toAddress: String,
            val value: BigInteger,
            val input: ByteArray
    ) : Parcelable {
        constructor(transactionData: TransactionData) : this(transactionData.to.hex, transactionData.value, transactionData.input)
    }

    class Factory(
            private val service: SwapService,
            private val transactionData: TransactionData
    ) : ViewModelProvider.Factory {

        private val evmKit by lazy { service.dex.evmKit!! }
        private val coin by lazy { service.dex.coin }
        private val transactionService by lazy {
            val feeRateProvider = FeeRateProviderFactory.provider(coin)!!
            EvmTransactionService(evmKit, feeRateProvider, 20)
        }
        private val coinService by lazy { CoinService(coin, App.currencyManager, App.xRateManager) }
        private val sendService by lazy { SendEvmTransactionService(transactionData, evmKit, transactionService) }
        private val stringProvider by lazy { StringProvider() }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendEvmTransactionViewModel::class.java -> {
                    SendEvmTransactionViewModel(sendService, coinService, stringProvider) as T
                }
                EthereumFeeViewModel::class.java -> {
                    EthereumFeeViewModel(transactionService, coinService, stringProvider) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

}