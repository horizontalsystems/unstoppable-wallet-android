package cash.p.terminal.modules.swap.uniswap

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.Warning
import cash.p.terminal.modules.swap.SwapMainModule
import cash.p.terminal.modules.swap.SwapViewItemHelper
import cash.p.terminal.modules.swap.allowance.SwapAllowanceService
import cash.p.terminal.modules.swap.allowance.SwapAllowanceViewModel
import cash.p.terminal.modules.swap.allowance.SwapPendingAllowanceService
import cash.p.terminal.modules.swap.providers.UniswapProvider
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.uniswapkit.UniswapKit
import kotlinx.parcelize.Parcelize

object UniswapModule {

    data class GuaranteedAmountViewItem(val title: String, val value: String)

    @Parcelize
    data class PriceImpactViewItem(val level: UniswapTradeService.PriceImpactLevel, val value: String) : Parcelable

    abstract class UniswapWarnings : Warning() {
        object PriceImpactWarning : UniswapWarnings()
    }

    class AllowanceViewModelFactory(
        private val service: UniswapService
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SwapAllowanceViewModel::class.java -> {
                    SwapAllowanceViewModel(
                        service,
                        service.allowanceService,
                        service.pendingAllowanceService,
                        SwapViewItemHelper(App.numberFormatter)
                    ) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    class Factory(
        dex: SwapMainModule.Dex
    ) : ViewModelProvider.Factory {

        private val evmKit: EthereumKit by lazy { App.evmBlockchainManager.getEvmKitManager(dex.blockchainType).evmKitWrapper?.evmKit!! }
        private val uniswapKit by lazy { UniswapKit.getInstance(evmKit) }
        private val uniswapProvider by lazy { UniswapProvider(uniswapKit) }
        private val allowanceService by lazy {
            SwapAllowanceService(
                uniswapProvider.routerAddress,
                App.adapterManager,
                evmKit
            )
        }
        private val pendingAllowanceService by lazy {
            SwapPendingAllowanceService(
                App.adapterManager,
                allowanceService
            )
        }
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
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

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
