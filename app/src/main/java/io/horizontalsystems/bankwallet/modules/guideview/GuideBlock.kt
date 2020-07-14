package io.horizontalsystems.bankwallet.modules.guideview

import android.text.SpannableStringBuilder

sealed class GuideBlock {
    var quoted = false
    var quotedFirst = false
    var quotedLast = false

    var listItem = false
    var listItemMarker: String? = null
    var listTightTop = false
    var listTightBottom = false

    data class Heading1(val text: SpannableStringBuilder) : GuideBlock()
    data class Heading2(val text: SpannableStringBuilder) : GuideBlock()
    data class Heading3(val text: SpannableStringBuilder) : GuideBlock()
    data class Paragraph(val text: SpannableStringBuilder) : GuideBlock() {
        constructor(text: SpannableStringBuilder, quoted: Boolean) : this(text) {
            this.quoted = quoted
        }
    }
    data class Image(val destination: String, val title: String?, val mainImage: Boolean) : GuideBlock()
    class Footer: GuideBlock()
}
