package io.horizontalsystems.bankwallet.modules.guideview

import android.text.Spanned

sealed class GuideBlock {
    var quoted = false
    var quotedFirst = false
    var quotedLast = false

    var listItem = false
    var listItemMarker: String? = null
    var listTightTop = false
    var listTightBottom = false

    data class Heading1(val text: Spanned) : GuideBlock()
    data class Heading2(val text: Spanned) : GuideBlock()
    data class Heading3(val text: Spanned) : GuideBlock()
    data class Paragraph(val text: Spanned) : GuideBlock() {
        constructor(text: Spanned, quoted: Boolean) : this(text) {
            this.quoted = quoted
        }
    }
    data class Image(val destination: String, val title: String?, val mainImage: Boolean) : GuideBlock()
    class Footer: GuideBlock()
}
