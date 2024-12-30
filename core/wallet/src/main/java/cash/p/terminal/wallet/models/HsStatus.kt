package cash.p.terminal.wallet.models

data class HsStatus(
    val coins: Long,
    val blockchains: Long,
    val tokens: Long,
    val exchanges: Long
)
