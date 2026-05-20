package cash.p.terminal.modules.calculator.domain

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class CalculatorAutoLockOption(
    val raw: String,
    private val amount: Int,
    private val timeUnit: TimeUnit,
) {
    AFTER_30_SEC("30sec", 30, TimeUnit.SECONDS),
    AFTER_1_MIN("1min", 1, TimeUnit.MINUTES),
    AFTER_2_MIN("2min", 2, TimeUnit.MINUTES),
    AFTER_3_MIN("3min", 3, TimeUnit.MINUTES),
    AFTER_4_MIN("4min", 4, TimeUnit.MINUTES),
    AFTER_5_MIN("5min", 5, TimeUnit.MINUTES),
    AFTER_15_MIN("15min", 15, TimeUnit.MINUTES),
    AFTER_30_MIN("30min", 30, TimeUnit.MINUTES),
    AFTER_1_HOUR("1hour", 1, TimeUnit.HOURS);

    val refillIntervalMillis: Long
        get() = timeUnit.toMillis(amount.toLong())

    fun formatLong(locale: Locale = Locale.getDefault()): String =
        format(locale, MeasureFormat.FormatWidth.WIDE)

    fun formatShort(locale: Locale = Locale.getDefault()): String =
        format(locale, MeasureFormat.FormatWidth.SHORT)

    private fun format(locale: Locale, width: MeasureFormat.FormatWidth): String =
        MeasureFormat.getInstance(locale, width).format(Measure(amount, measureUnit()))

    private fun measureUnit(): MeasureUnit = when (timeUnit) {
        TimeUnit.SECONDS -> MeasureUnit.SECOND
        TimeUnit.MINUTES -> MeasureUnit.MINUTE
        TimeUnit.HOURS -> MeasureUnit.HOUR
        else -> error("Unsupported time unit: $timeUnit")
    }

    companion object {
        val DEFAULT = AFTER_30_SEC

        fun fromRaw(raw: String?): CalculatorAutoLockOption? =
            raw?.let { value -> entries.firstOrNull { it.raw == value } }
    }
}
