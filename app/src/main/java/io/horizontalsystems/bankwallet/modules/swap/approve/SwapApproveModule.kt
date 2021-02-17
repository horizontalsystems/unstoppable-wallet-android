package io.horizontalsystems.bankwallet.modules.swap.approve

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.Eip20Adapter
import io.horizontalsystems.bankwallet.core.ethereum.CoinService
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionService
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.core.providers.EthereumFeeRateProvider
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceService
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.Observable
import java.math.BigInteger

object SwapApproveModule {

    class Factory(private val approveData: SwapAllowanceService.ApproveData) : ViewModelProvider.Factory {
        private val ethereumKit by lazy { App.ethereumKitManager.evmKit!! }
        private val transactionService by lazy {
            val feeRateProvider = FeeRateProviderFactory.provider(App.appConfigProvider.ethereumCoin) as EthereumFeeRateProvider
            EvmTransactionService(ethereumKit, feeRateProvider, 0)
        }
        private val coinService by lazy { CoinService(approveData.coin, App.currencyManager, App.xRateManager) }
        private val ethCoinService by lazy { CoinService(App.appConfigProvider.ethereumCoin, App.currencyManager, App.xRateManager) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SwapApproveViewModel::class.java -> {
                    val wallet = checkNotNull(App.walletManager.wallet(approveData.coin))
                    val erc20Adapter = App.adapterManager.getAdapterForWallet(wallet) as Eip20Adapter
                    val approveAmountBigInteger = approveData.amount.movePointRight(approveData.coin.decimal).toBigInteger()
                    val allowanceAmountBigInteger = approveData.allowance.movePointRight(approveData.coin.decimal).toBigInteger()
                    val swapApproveService = SwapApproveService(transactionService, erc20Adapter.eip20Kit, ethereumKit, approveAmountBigInteger, Address(approveData.spenderAddress), allowanceAmountBigInteger)
                    SwapApproveViewModel(swapApproveService, coinService, ethCoinService) as T
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

    fun onCleared()
    fun approve()
}
