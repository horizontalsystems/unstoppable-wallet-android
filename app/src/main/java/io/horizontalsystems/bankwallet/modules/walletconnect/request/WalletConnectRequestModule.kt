package io.horizontalsystems.bankwallet.modules.walletconnect.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectSendEthereumTransactionRequest
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

object WalletConnectRequestModule {

    class Factory(val request: WalletConnectSendEthereumTransactionRequest) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = WalletConnectSendEthereumTransactionRequestService(
                    App.ethereumKitManager.ethereumKit!!,
                    App.appConfigProvider,
                    App.currencyManager,
                    App.xRateManager
            )

            return WalletConnectSendEthereumTransactionRequestViewModel(service, request) as T
        }
    }

}

data class WalletConnectTransaction(
        val from: Address,
        val to: Address,
        val nonce: Long?,
        val gasPrice: Long?,
        val gasLimit: Long,
        val value: BigInteger,
        val data: ByteArray
)
