package cash.p.terminal.wallet.transaction

import cash.p.terminal.wallet.Account
import io.horizontalsystems.core.entities.Blockchain

data class TransactionSource(
    val blockchain: Blockchain,
    val account: Account,
    val meta: String?
)