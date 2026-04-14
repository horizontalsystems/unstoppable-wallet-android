package com.quantum.wallet.bankwallet.modules.multiswap

sealed class SwapError : Throwable() {
    object InsufficientBalanceFrom : SwapError()
}
