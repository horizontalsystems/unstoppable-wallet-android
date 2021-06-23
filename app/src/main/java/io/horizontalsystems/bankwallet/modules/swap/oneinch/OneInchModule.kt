package io.horizontalsystems.bankwallet.modules.swap.oneinch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapViewItemHelper
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceViewModel
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceService
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.oneinchkit.OneInchKit

object OneInchModule {

    class AllowanceViewModelFactory(
            private val service: OneInchSwapService
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

    class Factory(dex: SwapMainModule.Dex) : ViewModelProvider.Factory {
        private val evmKit: EthereumKit by lazy { dex.evmKit!! }
        private val oneIncKit by lazy { OneInchKit.getInstance(evmKit) }
        private val allowanceService by lazy { SwapAllowanceService(oneIncKit.smartContractAddress, App.adapterManager, evmKit) }
        private val pendingAllowanceService by lazy { SwapPendingAllowanceService(App.adapterManager, allowanceService) }
        private val service by lazy {
            OneInchSwapService(
                    dex,
                    tradeService,
                    allowanceService,
                    pendingAllowanceService,
                    App.adapterManager
            )
        }
        private val tradeService by lazy {
            OneInchTradeService(evmKit, oneIncKit)
        }
        private val formatter by lazy {
            SwapViewItemHelper(App.numberFormatter)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                OneInchSwapViewModel::class.java -> {
                    OneInchSwapViewModel(service, tradeService, pendingAllowanceService, formatter) as T
                }
                SwapAllowanceViewModel::class.java -> {
                    SwapAllowanceViewModel(service, allowanceService, pendingAllowanceService, formatter) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

}
