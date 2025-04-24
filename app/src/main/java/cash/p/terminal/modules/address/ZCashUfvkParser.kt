package cash.p.terminal.modules.address

object ZCashUfvkParser {
    fun isUfvk(key: String): Boolean {
        return key.startsWith("uview")
    }
}