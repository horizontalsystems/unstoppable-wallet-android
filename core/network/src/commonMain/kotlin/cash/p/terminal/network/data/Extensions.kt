package cash.p.terminal.network.data

fun <T> T?.requireNotNull(field: String): T {
    checkNotNull(this) { "$field is NULL" }
    return this
}