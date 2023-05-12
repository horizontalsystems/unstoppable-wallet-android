package io.horizontalsystems.bankwallet.modules.swap.uniswap

import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.UniversalSwapTradeData
import io.horizontalsystems.bankwallet.modules.swap.settings.uniswap.SwapTradeOptions
import io.horizontalsystems.ethereumkit.models.TransactionData

interface IUniswapTradeService : SwapMainModule.ISwapTradeService {
    var tradeOptions: SwapTradeOptions
    @Throws
    fun transactionData(tradeData: UniversalSwapTradeData): TransactionData
}