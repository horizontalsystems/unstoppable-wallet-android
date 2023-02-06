package io.horizontalsystems.marketkit.models

enum class HsPointTimePeriod(val value: String) {
    Minute30("30m"),
    Hour1("1h"),
    Hour4("4h"),
    Hour8("8h"),
    Day1("1d"),
    Week1("1w");

    val interval: Long
        get() = when (this) {
            Minute30 -> 30 * 60
            Hour1 -> 60 * 60
            Hour4 -> 4 * 60 * 60
            Hour8 -> 8 * 60 * 60
            Day1 -> 24 * 60 * 60
            Week1 -> 7 * 24 * 60 * 60
        }
}
