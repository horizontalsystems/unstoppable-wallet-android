package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.R

sealed class BalanceSortType {
    object Value : BalanceSortType()
    object Az : BalanceSortType()
    object Custom : BalanceSortType()

    fun getTitleRes(): Int = when (this) {
        Value -> R.string.Balance_Sort_Value
        Az -> R.string.Balance_Sort_Az
        Custom -> R.string.Balance_Sort_Custom
    }
}
