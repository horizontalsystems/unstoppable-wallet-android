package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.R

sealed class BalanceSortType {
    object Balance : BalanceSortType()
    object Az : BalanceSortType()
    object Default : BalanceSortType()

    fun getTitleRes(): Int = when (this) {
        Balance -> R.string.Balance_Sort_Balance
        Az -> R.string.Balance_Sort_Az
        Default -> R.string.Balance_Sort_Default
    }
}
