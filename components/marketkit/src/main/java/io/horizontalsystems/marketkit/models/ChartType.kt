package io.horizontalsystems.marketkit.models

enum class ChartType(val interval: Long, val points: Int, val resource: String) {
    TODAY(30, 48, "histominute"),   // minutes
    DAILY(30, 48, "histominute"),   // minutes
    WEEKLY(4, 48, "histohour"),     // hourly
    WEEKLY2(8, 44, "histohour"),     // hourly
    MONTHLY(12, 60, "histohour"),   // hourly
    MONTHLY_BY_DAY(1, 30, "histoday"),   // daily
    MONTHLY3(2, 45, "histoday"),    // daily
    MONTHLY6(3, 60, "histoday"),    // daily
    MONTHLY12(7, 52, "histoday"),   // daily
    MONTHLY24(14, 52, "histoday");  // daily

    val expirationInterval: Long
        get() {
            val multiplier = when (resource) {
                "histominute" -> 60
                "histohour" -> 60 * 60
                "histoday" -> 24 * 60 * 60
                else -> 60
            }

            return interval * multiplier
        }

    val rangeInterval: Long
        get() = expirationInterval * points

    val seconds: Long
        get() = when (this) {
            TODAY -> interval
            DAILY -> interval
            WEEKLY -> interval * 60
            WEEKLY2 -> interval * 60
            MONTHLY -> interval * 60
            MONTHLY_BY_DAY -> interval * 24 * 60
            MONTHLY3 -> interval * 24 * 60
            MONTHLY6 -> interval * 24 * 60
            MONTHLY12 -> interval * 24 * 60
            MONTHLY24 -> interval * 24 * 60
        } * 60

    val days: Int
        get() = when (this) {
            TODAY -> 1
            DAILY -> 1
            WEEKLY -> 7
            WEEKLY2 -> 14
            MONTHLY -> 30
            MONTHLY_BY_DAY -> 30
            MONTHLY3 -> 90
            MONTHLY6 -> 180
            MONTHLY12 -> 360
            MONTHLY24 -> 720
        }

    val coinGeckoDaysParameter: Int
        get() = when (this) {
            TODAY, DAILY, MONTHLY_BY_DAY -> days
            else -> days * 2
        }

    companion object {
        private val map = values().associateBy(ChartType::name)

        fun fromString(type: String?): ChartType? = map[type]
    }
}
