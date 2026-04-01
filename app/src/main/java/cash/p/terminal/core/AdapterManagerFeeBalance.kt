package cash.p.terminal.core

import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IBalanceAdapter
import cash.p.terminal.wallet.Token
import java.math.BigDecimal

fun IAdapterManager.getFeeTokenBalance(
    feeToken: Token,
    currentToken: Token,
): BigDecimal? {
    return getAdjustedBalanceDataForToken(feeToken)?.available
        ?: (getAdapterForToken<IBalanceAdapter>(currentToken) as? INativeBalanceProvider)
            ?.nativeBalanceData?.available
}
