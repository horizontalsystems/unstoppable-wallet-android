package io.horizontalsystems.bankwallet.core.sorting

import java.math.BigDecimal

object SortComparators {
    fun <T> booleanFirst(selector: (T) -> Boolean): Comparator<T> =
        compareByDescending(selector)

    fun <T> nullableIntAscending(selector: (T) -> Int?): Comparator<T> =
        compareBy { selector(it) ?: Int.MAX_VALUE }

    fun <T> nullableDecimalDescending(selector: (T) -> BigDecimal?): Comparator<T> =
        compareByDescending { selector(it) ?: BigDecimal.ZERO }

    fun <T> nullableDecimalDescendingNullLast(selector: (T) -> BigDecimal?): Comparator<T> =
        Comparator { a, b ->
            val va = selector(a)
            val vb = selector(b)
            when {
                va == null && vb == null -> 0
                va == null -> 1
                vb == null -> -1
                else -> vb.compareTo(va)
            }
        }

    fun <T> nullableStringAscending(selector: (T) -> String?): Comparator<T> =
        compareBy(String.CASE_INSENSITIVE_ORDER) { selector(it) ?: "" }
}
