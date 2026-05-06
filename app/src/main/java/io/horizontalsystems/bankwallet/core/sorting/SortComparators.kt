package io.horizontalsystems.bankwallet.core.sorting

import java.math.BigDecimal

object SortComparators {
    fun <T> booleanFirst(selector: (T) -> Boolean): Comparator<T> =
        compareByDescending(selector)

    fun <T> nullableIntAscending(selector: (T) -> Int?): Comparator<T> =
        compareBy { selector(it) ?: Int.MAX_VALUE }

    fun <T> nullableDecimalDescending(selector: (T) -> BigDecimal?): Comparator<T> =
        compareByDescending { selector(it) ?: BigDecimal.ZERO }

    fun <T> nullableStringAscending(selector: (T) -> String?): Comparator<T> =
        compareBy(String.CASE_INSENSITIVE_ORDER) { selector(it) ?: "" }
}
