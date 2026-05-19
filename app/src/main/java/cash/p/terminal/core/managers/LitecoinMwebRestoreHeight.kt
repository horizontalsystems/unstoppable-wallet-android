package cash.p.terminal.core.managers

object LitecoinMwebRestoreHeight {
    fun parse(value: String?): Long? {
        return value
            ?.trim()
            ?.toLongOrNull()
            ?.takeIf(::isValid)
    }

    fun toBlockHeight(value: Long?): Int? {
        return value
            ?.takeIf(::isValid)
            ?.toInt()
    }

    private fun isValid(value: Long): Boolean {
        return value > 0 && value <= Int.MAX_VALUE.toLong()
    }
}
