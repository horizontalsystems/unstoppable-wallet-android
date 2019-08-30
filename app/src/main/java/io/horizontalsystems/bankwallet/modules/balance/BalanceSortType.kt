package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.R

sealed class BalanceSortType {
    object Name : BalanceSortType()
    object Value : BalanceSortType()
    object PercentGrowth : BalanceSortType()

    fun getTitleRes(): Int = when (this) {
        Value -> R.string.Balance_Sort_Balance
        Name -> R.string.Balance_Sort_Az
        PercentGrowth -> R.string.Balance_Sort_24hPriceChange
    }

    fun getAsString(): String = when (this) {
        Value -> "value"
        Name -> "name"
        PercentGrowth -> "percent_growth"
    }

    companion object {
        fun getTypeFromString(value: String): BalanceSortType = when (value) {
            "value" -> Value
            "percent_growth" -> PercentGrowth
            else -> Name
        }
    }
}
