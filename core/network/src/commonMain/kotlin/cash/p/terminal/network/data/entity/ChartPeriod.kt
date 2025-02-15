package cash.p.terminal.network.data.entity

enum class ChartPeriod(val value: String) {
    HOUR("hour"),
    DAY("day"),
    WEEK("week"),
    MONTH("month"),
    YEAR("year"),
    MAX("max")
}