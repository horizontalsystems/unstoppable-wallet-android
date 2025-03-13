package io.horizontalsystems.bankwallet.modules.amount

enum class AmountInputType {
    COIN,
    CURRENCY,
    ;

    fun reversed(): AmountInputType = if (this == COIN) CURRENCY else COIN
}
