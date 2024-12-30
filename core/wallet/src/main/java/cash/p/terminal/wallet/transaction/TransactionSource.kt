package cash.p.terminal.wallet.transaction

import io.horizontalsystems.core.entities.Blockchain

data class TransactionSource(
    val blockchain: Blockchain,
    val account: cash.p.terminal.wallet.Account,
    val meta: String?
)