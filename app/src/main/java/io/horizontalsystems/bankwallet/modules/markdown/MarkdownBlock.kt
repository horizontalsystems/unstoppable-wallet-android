package io.horizontalsystems.bankwallet.modules.markdown

import android.text.SpannableStringBuilder

sealed class MarkdownBlock {
    var quoted = false
    var quotedFirst = false
    var quotedLast = false

    var listItem = false
    var listItemMarker: String? = null
    var listTightTop = false
    var listTightBottom = false

    data class Heading1(val text: SpannableStringBuilder) : MarkdownBlock()
    data class Heading2(val text: SpannableStringBuilder) : MarkdownBlock()
    data class Heading3(val text: SpannableStringBuilder) : MarkdownBlock()
    data class Paragraph(val text: SpannableStringBuilder) : MarkdownBlock() {
        constructor(text: SpannableStringBuilder, quoted: Boolean) : this(text) {
            this.quoted = quoted
        }
    }
    data class Image(val destination: String, val title: String?, val mainImage: Boolean) : MarkdownBlock()
    class Footer: MarkdownBlock()
}
