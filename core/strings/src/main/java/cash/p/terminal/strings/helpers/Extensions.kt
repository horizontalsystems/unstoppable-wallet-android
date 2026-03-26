package cash.p.terminal.strings.helpers


fun String.shorten(): String {
    val prefixes = listOf("0x", "bc", "bnb", "ltc", "bitcoincash:", "ecash:")

    var prefix = ""
    for (p in prefixes) {
        if (this.startsWith(p)) {
            prefix = p
            break
        }
    }

    val withoutPrefix = this.removePrefix(prefix)

    val head = 10
    val middle = 4
    val tail = 10
    val minLength = head + middle + tail

    return if (withoutPrefix.length > minLength)
        prefix + withoutPrefix.take(head) + "..." + withoutPrefix.substring(
            withoutPrefix.length / 2 - middle / 2,
            withoutPrefix.length / 2 + middle / 2
        ) + "..." + withoutPrefix.takeLast(tail)
    else
        this
}

fun String.toMasked(): String {
    return when {
        isEmpty() -> ""
        length == 1 -> "*"
        length < 10 -> "**"
        else -> "${first()}***${last()}"
    }
}

fun List<String>.toMasked(): String {
    return when (size) {
        0 -> ""
        1 -> this[0].toMasked()
        else -> "${first().first()}****${last().last()}"
    }
}