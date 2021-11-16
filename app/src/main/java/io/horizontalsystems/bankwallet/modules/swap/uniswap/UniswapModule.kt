package io.horizontalsystems.bankwallet.modules.swap.uniswap

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapViewItemHelper
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceViewModel
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.providers.UniswapProvider
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.uniswapkit.UniswapKit
import kotlinx.android.parcel.Parcelize

object UniswapModule {

    data class GuaranteedAmountViewItem(val title: String, val value: String)

    @Parcelize
    data class PriceImpactViewItem(val level: UniswapTradeService.PriceImpactLevel, val value: String) : Parcelable

    class AllowanceViewModelFactory(
            private val service: UniswapService
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SwapAllowanceViewModel::class.java -> {
                    SwapAllowanceViewModel(service, service.allowanceService, service.pendingAllowanceService, SwapViewItemHelper(App.numberFormatter)) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    class Factory(
            dex: SwapMainModule.Dex
    ) : ViewModelProvider.Factory {

        private val evmKit: EthereumKit by lazy { dex.blockchain.evmKit!! }
        private val uniswapKit by lazy { UniswapKit.getInstance(evmKit) }
        private val uniswapProvider by lazy { UniswapProvider(uniswapKit) }
        private val allowanceService by lazy { SwapAllowanceService(uniswapProvider.routerAddress, App.adapterManager, evmKit) }
        private val pendingAllowanceService by lazy { SwapPendingAllowanceService(App.adapterManager, allowanceService) }
        private val service by lazy {
            UniswapService(
                    dex,
                    tradeService,
                    allowanceService,
                    pendingAllowanceService,
                    App.adapterManager
            )
        }
        private val tradeService by lazy {
            UniswapTradeService(evmKit, uniswapProvider)
        }
        private val formatter by lazy {
            SwapViewItemHelper(App.numberFormatter)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {

            return when (modelClass) {
                UniswapViewModel::class.java -> {
                    UniswapViewModel(service, tradeService, pendingAllowanceService, formatter) as T
                }
                SwapAllowanceViewModel::class.java -> {
                    SwapAllowanceViewModel(service, allowanceService, pendingAllowanceService, formatter) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

}
