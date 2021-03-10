package io.horizontalsystems.bankwallet.modules.swap.approve

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.Eip20Adapter
import io.horizontalsystems.bankwallet.core.ethereum.CoinService
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceService
import io.horizontalsystems.ethereumkit.models.Address

object SwapApproveModule {

    class Factory(private val approveData: SwapAllowanceService.ApproveData) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SwapApproveViewModel::class.java -> {
                    val wallet = checkNotNull(App.walletManager.wallet(approveData.coin))
                    val erc20Adapter = App.adapterManager.getAdapterForWallet(wallet) as Eip20Adapter
                    val approveAmountBigInteger = approveData.amount.movePointRight(approveData.coin.decimal).toBigInteger()
                    val allowanceAmountBigInteger = approveData.allowance.movePointRight(approveData.coin.decimal).toBigInteger()
                    val swapApproveService = SwapApproveService(erc20Adapter.eip20Kit, approveAmountBigInteger, Address(approveData.spenderAddress), allowanceAmountBigInteger)
                    val coinService by lazy { CoinService(approveData.coin, App.currencyManager, App.xRateManager) }
                    val stringProvider by lazy { StringProvider() }
                    SwapApproveViewModel(approveData.dex, swapApproveService, coinService, stringProvider) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }
}
