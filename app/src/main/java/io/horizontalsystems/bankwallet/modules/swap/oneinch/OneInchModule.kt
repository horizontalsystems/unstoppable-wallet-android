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
import io.horizontalsystems.ethereumkit.models.Address as EthereumAddress

object OneInchModule {
    class Factory(dex: SwapMainModule.Dex) : ViewModelProvider.Factory {

        private val evmKit: EthereumKit by lazy { dex.evmKit!! }
        private val allowanceService by lazy { SwapAllowanceService(EthereumAddress("0x11111112542d85b3ef69ae05771c2dccff4faa26"), App.adapterManager, evmKit) }
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
            OneInchTradeService(evmKit, OneInchKit.getInstance(evmKit))
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
