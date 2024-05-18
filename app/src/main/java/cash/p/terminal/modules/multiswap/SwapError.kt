package cash.p.terminal.modules.multiswap

sealed class SwapError : Throwable() {
    object InsufficientBalanceFrom : SwapError()
}
