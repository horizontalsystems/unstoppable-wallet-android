package io.horizontalsystems.core

import io.horizontalsystems.core.entities.Value
import java.math.BigDecimal

interface IAppNumberFormatter {
    fun format(
        value: Number,
        minimumFractionDigits: Int,
        maximumFractionDigits: Int,
        prefix: String = "",
        suffix: String = ""
    ): String

    fun formatCoinFull(
        value: BigDecimal,
        code: String?,
        coinDecimals: Int,
    ): String

    fun formatCoinShort(
        value: BigDecimal,
        code: String?,
        coinDecimals: Int
    ): String

    fun formatNumberShort(
        value: BigDecimal,
        maximumFractionDigits: Int
    ): String

    fun formatFiatFull(
        value: BigDecimal,
        symbol: String
    ): String

    fun formatFiatShort(
        value: BigDecimal,
        symbol: String,
        currencyDecimals: Int
    ): String

    fun formatValueAsDiff(value: Value): String
}