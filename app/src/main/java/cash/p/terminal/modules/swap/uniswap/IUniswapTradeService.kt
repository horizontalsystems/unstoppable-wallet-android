package cash.p.terminal.modules.swap.uniswap

import cash.p.terminal.modules.swap.SwapMainModule
import cash.p.terminal.modules.swap.UniversalSwapTradeData
import cash.p.terminal.modules.swap.settings.uniswap.SwapTradeOptions
import io.horizontalsystems.ethereumkit.models.TransactionData

interface IUniswapTradeService : SwapMainModule.ISwapTradeService {
    var tradeOptions: SwapTradeOptions
    @Throws
    fun transactionData(tradeData: UniversalSwapTradeData): TransactionData
}