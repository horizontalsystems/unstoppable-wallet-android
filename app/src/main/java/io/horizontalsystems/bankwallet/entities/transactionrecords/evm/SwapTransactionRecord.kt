package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.ethereumkit.models.FullTransaction
import java.math.BigDecimal

class SwapTransactionRecord(
    fullTransaction: FullTransaction,
    baseCoin: Coin,
    tokenIn: Coin,
    tokenOut: Coin?,
    amountIn: BigDecimal,
    amountOut: BigDecimal?,
    val exchangeAddress: String,
    val foreignRecipient: Boolean,
    source: TransactionSource
) : EvmTransactionRecord(fullTransaction, baseCoin, source) {

    // valueIn stores amountInMax in cases when exact valueIn amount is not known
    val valueIn = CoinValue(tokenIn, amountIn)

    // valueOut stores amountOutMin in cases when exact valueOut amount is not known
    val valueOut: CoinValue? = amountOut?.let { tokenOut?.let { CoinValue(tokenOut, amountOut) } }

}
