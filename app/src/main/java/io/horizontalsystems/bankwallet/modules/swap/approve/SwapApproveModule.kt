package io.horizontalsystems.bankwallet.modules.swap.approve

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.adapters.Erc20Adapter
import io.horizontalsystems.bankwallet.core.ethereum.CoinService
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.core.ethereum.EthereumTransactionService
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.core.providers.EthereumFeeRateProvider
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.guides.DataState
import io.horizontalsystems.bankwallet.modules.swap.SwapModule
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.Observable
import java.math.BigInteger

object SwapApproveModule {

    class Factory(private val approveData: SwapModule.ApproveData) : ViewModelProvider.Factory {
        private val ethereumKit by lazy { App.ethereumKitManager.ethereumKit!! }
        private val transactionService by lazy {
            val feeRateProvider = FeeRateProviderFactory.provider(App.appConfigProvider.ethereumCoin) as EthereumFeeRateProvider
            EthereumTransactionService(ethereumKit, feeRateProvider)
        }
        private val coinService by lazy { CoinService(approveData.coin, App.currencyManager, App.xRateManager) }
        private val ethCoinService by lazy { CoinService(App.appConfigProvider.ethereumCoin, App.currencyManager, App.xRateManager) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SwapApproveViewModel::class.java -> {
                    val wallet = checkNotNull(App.walletManager.wallet(approveData.coin))
                    val erc20Adapter = App.adapterManager.getAdapterForWallet(wallet) as Erc20Adapter
                    val swapApproveService = SwapApproveService(transactionService, erc20Adapter.erc20Kit, ethereumKit, approveData.amount, Address(approveData.spenderAddress), approveData.allowance)
                    SwapApproveViewModel(swapApproveService, coinService, ethCoinService, listOf(swapApproveService)) as T
                }
                EthereumFeeViewModel::class.java -> {
                    EthereumFeeViewModel(transactionService, ethCoinService) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    class InsufficientFeeBalance(val coinValue: CoinValue) : Exception()

}

interface ISwapApproveService {
    var amount: BigInteger?
    val stateObservable: Observable<SwapApproveService.State>

    fun approve()
}
