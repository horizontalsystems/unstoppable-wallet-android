package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.modules.transactions.TransactionType
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.ethereumkit.models.FullTransaction
import java.math.BigDecimal

class SwapTransactionRecord(
        fullTransaction: FullTransaction,
        val exchangeAddress: String,
        val tokenIn: Coin,
        val tokenOut: Coin,
        val amountIn: BigDecimal, // amountIn stores amountInMax in cases when exact amountIn amount is not known
        val amountOut: BigDecimal, // amountOut stores amountOutMin in cases when exact amountOut amount is not known
        val foreignRecipient: Boolean
): EvmTransactionRecord(fullTransaction) {
    
    override fun getType(lastBlockInfo: LastBlockInfo?): TransactionType {
        val inCoinValue = CoinValue(tokenIn, amountIn)
        val outCoinValue = CoinValue(tokenOut, amountOut)
        return TransactionType.Swap(exchangeAddress, inCoinValue, outCoinValue)
    }
}
