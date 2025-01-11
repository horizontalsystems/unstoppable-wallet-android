package cash.p.terminal.wallet.balance

data class DeemedValue<T>(val value: T, val dimmed: Boolean = false, val visible: Boolean = true)