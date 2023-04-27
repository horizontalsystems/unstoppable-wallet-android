package io.horizontalsystems.bankwallet.modules.swap.approve

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.Eip20Adapter
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinService
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.ethereumkit.models.Address

object SwapApproveModule {

    const val requestKey = "approve"
    const val resultKey = "result"
    const val dataKey = "data_key"

    class Factory(private val approveData: SwapMainModule.ApproveData) :
        ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SwapApproveViewModel::class.java -> {
                    val wallet =
                        checkNotNull(App.walletManager.activeWallets.firstOrNull { it.token == approveData.token })
                    val erc20Adapter =
                        App.adapterManager.getAdapterForWallet(wallet) as Eip20Adapter
                    val approveAmountBigInteger =
                        approveData.amount.movePointRight(approveData.token.decimals).toBigInteger()
                    val allowanceAmountBigInteger =
                        approveData.allowance.movePointRight(approveData.token.decimals)
                            .toBigInteger()
                    val swapApproveService = SwapApproveService(
                        erc20Adapter.eip20Kit,
                        approveAmountBigInteger,
                        Address(approveData.spenderAddress),
                        allowanceAmountBigInteger
                    )
                    val coinService by lazy {
                        EvmCoinService(approveData.token, App.currencyManager, App.marketKit)
                    }
                    SwapApproveViewModel(approveData.dex, swapApproveService, coinService) as T
                }

                else -> throw IllegalArgumentException()
            }
        }
    }

    fun prepareParams(approveData: SwapMainModule.ApproveData) = bundleOf(dataKey to approveData)

}
