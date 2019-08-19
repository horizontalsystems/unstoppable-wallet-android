package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.R

sealed class BalanceSortType {
    object Name : BalanceSortType()
    object Value : BalanceSortType()

    fun getTitleRes(): Int = when (this) {
        Value -> R.string.Balance_Sort_Balance
        Name -> R.string.Balance_Sort_Az
    }

    fun getAsString(): String = when (this) {
        Value -> "value"
        Name -> "name"
    }

    companion object {
        fun getTypeFromString(value: String): BalanceSortType = when (value) {
            "value" -> Value
            else -> Name
        }
    }
}
