package io.horizontalsystems.bankwallet.modules.swap.approve

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.Eip20Adapter
import io.horizontalsystems.bankwallet.core.ethereum.CoinService
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionService
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceService
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.Observable
import java.math.BigInteger

object SwapApproveModule {

    class Factory(private val approveData: SwapAllowanceService.ApproveData) : ViewModelProvider.Factory {
        private val evmKit by lazy { approveData.dex.evmKit!! }
        private val transactionService by lazy { EvmTransactionService(evmKit, FeeRateProviderFactory.provider(approveData.dex.coin)!!, 0) }
        private val coinService by lazy { CoinService(approveData.coin, App.currencyManager, App.xRateManager) }
        private val coin by lazy { approveData.dex.coin }
        private val ethCoinService by lazy { CoinService(coin, App.currencyManager, App.xRateManager) }
        private val stringProvider by lazy { StringProvider() }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SwapApproveViewModel::class.java -> {
                    val wallet = checkNotNull(App.walletManager.wallet(approveData.coin))
                    val erc20Adapter = App.adapterManager.getAdapterForWallet(wallet) as Eip20Adapter
                    val approveAmountBigInteger = approveData.amount.movePointRight(approveData.coin.decimal).toBigInteger()
                    val allowanceAmountBigInteger = approveData.allowance.movePointRight(approveData.coin.decimal).toBigInteger()
                    val swapApproveService = SwapApproveService(transactionService, erc20Adapter.eip20Kit, evmKit, approveAmountBigInteger, Address(approveData.spenderAddress), allowanceAmountBigInteger)
                    SwapApproveViewModel(swapApproveService, coinService, ethCoinService, stringProvider) as T
                }
                EthereumFeeViewModel::class.java -> {
                    EthereumFeeViewModel(transactionService, ethCoinService, stringProvider) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }
}

interface ISwapApproveService {
    var amount: BigInteger?
    val stateObservable: Observable<SwapApproveService.State>

    fun onCleared()
    fun approve()
}
