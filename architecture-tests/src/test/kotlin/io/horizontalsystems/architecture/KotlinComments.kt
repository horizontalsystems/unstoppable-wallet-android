package io.horizontalsystems.architecture

/**
 * Kotlin source with its comments replaced by a space, so that a rule reading
 * the text does not trip over what a comment merely mentions.
 *
 * A regular expression cannot do this: a string template holds code, which
 * may hold another string, so `"${a ?: "https://b"}"` has to be followed
 * through its nesting to know that `//` is not the start of a comment.
 */
internal fun String.withoutComments(): String =
    CommentStripper(this, emptyLiterals = false).strip()

/**
 * As [withoutComments], and with every string literal emptied: its quotes
 * stay, the code of its `${…}` templates stays, its text goes. What is left
 * is only what executes, so a rule does not trip over what a string says.
 */
internal fun String.withoutCommentsAndLiteralText(): String =
    CommentStripper(this, emptyLiterals = true).strip()

private class CommentStripper(
    private val source: String,
    private val emptyLiterals: Boolean,
) {
    private val out = StringBuilder(source.length)
    private var index = 0

    // What the scanner is inside of, innermost last. Empty at the top level.
    private val nesting = ArrayDeque<Nesting>()

    private sealed interface Nesting
    private data object InString : Nesting
    private data object InRawString : Nesting

    // The code of a `${…}` template; openBraces counts the `{` it has open.
    private class InTemplate(var openBraces: Int = 0) : Nesting

    fun strip(): String {
        while (index < source.length) {
            when (nesting.lastOrNull()) {
                InString -> scanString()
                InRawString -> scanRawString()
                else -> scanCode()
            }
        }
        return out.toString()
    }

    private fun scanCode() {
        when {
            at("//") -> skipLineComment()
            at("/*") -> skipBlockComment()
            else -> copyCode()
        }
    }

    private fun copyCode() {
        when {
            at(RAW_QUOTE) -> copy(RAW_QUOTE.length).also { nesting.addLast(InRawString) }
            at("\"") -> copy(1).also { nesting.addLast(InString) }
            at("'") -> copyQuoted('\'')
            at("`") -> copyQuoted('`')
            at("{") -> copy(1).also { openBrace() }
            at("}") -> copy(1).also { closeBrace() }
            else -> copy(1)
        }
    }

    private fun scanString() {
        when {
            at("\\") -> copyLiteralText(2)
            at("\${") -> copy(2).also { nesting.addLast(InTemplate()) }
            at("\"") -> copy(1).also { nesting.removeLast() }
            else -> copyLiteralText(1)
        }
    }

    private fun scanRawString() {
        when {
            at("\${") -> copy(2).also { nesting.addLast(InTemplate()) }
            at(RAW_QUOTE) -> closeRawString()
            else -> copyLiteralText(1)
        }
    }

    // Quotes before the closing three belong to the string.
    private fun closeRawString() {
        copyLiteralText(quoteRun() - RAW_QUOTE.length)
        copy(RAW_QUOTE.length)
        nesting.removeLast()
    }

    private fun template() = nesting.lastOrNull() as? InTemplate

    private fun openBrace() {
        template()?.let { it.openBraces++ }
    }

    private fun closeBrace() {
        val template = template() ?: return
        if (template.openBraces == 0) nesting.removeLast() else template.openBraces--
    }

    private fun skipLineComment() {
        while (index < source.length && source[index] != '\n') index++
        out.append(' ')
    }

    // Block comments nest in Kotlin.
    private fun skipBlockComment() {
        var depth = 0
        do {
            when {
                at("/*") -> { depth++; index += 2 }
                at("*/") -> { depth--; index += 2 }
                else -> index++
            }
        } while (depth > 0 && index < source.length)
        out.append(' ')
    }

    // A character literal or a backticked name: copied whole, so that a quote
    // or `//` inside it is not read as a string or a comment.
    private fun copyQuoted(quote: Char) {
        copy(1)
        while (index < source.length && source[index] != quote && source[index] != '\n') {
            copy(if (source[index] == '\\') 2 else 1)
        }
        copy(1)
    }

    private fun quoteRun(): Int {
        var end = index
        while (end < source.length && source[end] == '"') end++
        return end - index
    }

    private fun at(token: String) = source.startsWith(token, index)

    private fun copy(count: Int) {
        val end = minOf(index + count, source.length)
        out.append(source, index, end)
        index = end
    }

    private fun copyLiteralText(count: Int) {
        if (emptyLiterals) index = minOf(index + count, source.length) else copy(count)
    }

    private companion object {
        const val RAW_QUOTE = "\"\"\""
    }
}
