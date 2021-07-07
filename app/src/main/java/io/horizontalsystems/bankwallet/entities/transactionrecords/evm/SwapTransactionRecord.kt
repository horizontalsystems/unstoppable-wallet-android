package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.modules.transactions.TransactionType
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.ethereumkit.models.FullTransaction
import java.math.BigDecimal

class SwapTransactionRecord(
    fullTransaction: FullTransaction,
    baseCoin: Coin,
    tokenIn: Coin,
    tokenOut: Coin,
    amountIn: BigDecimal,
    amountOut: BigDecimal?,
    val exchangeAddress: String
) : EvmTransactionRecord(fullTransaction, baseCoin) {

    // valueIn stores amountInMax in cases when exact valueIn amount is not known
    val valueIn = CoinValue(tokenIn, amountIn)

    // valueOut stores amountOutMin in cases when exact valueOut amount is not known
    val valueOut: CoinValue? = amountOut?.let { CoinValue(tokenOut, it) }

    override fun getType(lastBlockInfo: LastBlockInfo?): TransactionType {
        return TransactionType.Swap(exchangeAddress, valueIn, valueOut)
    }
}
