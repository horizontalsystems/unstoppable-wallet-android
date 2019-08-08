package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.R

sealed class BalanceSortType {
    object Name : BalanceSortType()
    object Value : BalanceSortType()
//    object LastDayChange : BalanceSortType()

    fun getTitleRes(): Int = when (this) {
        Value -> R.string.Balance_Sort_Balance
        Name -> R.string.Balance_Sort_Az
//        LastDayChange -> R.string.Balance_Sort_Default
    }

    fun getAsString(): String = when (this) {
        Value -> "value"
        Name -> "name"
//        LastDayChange -> "daychange"
    }

    companion object {
        fun getTypeFromString(value: String): BalanceSortType = when (value) {
            "value" -> Value
            else -> Name
//            else -> LastDayChange
        }
    }
}
