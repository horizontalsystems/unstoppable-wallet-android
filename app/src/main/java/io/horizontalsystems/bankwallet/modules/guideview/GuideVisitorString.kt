package io.horizontalsystems.bankwallet.modules.guideview

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.URLSpan
import org.commonmark.node.*

class GuideVisitorString : AbstractVisitor() {
    val spannableStringBuilder = SpannableStringBuilder()

    override fun visit(text: Text) {
        spannableStringBuilder.append(text.literal)
    }

    override fun visit(strongEmphasis: StrongEmphasis) {
        spannableStringBuilder.append(getWrappedContent(strongEmphasis, StyleSpan(Typeface.BOLD)))
    }

    override fun visit(link: Link) {
        spannableStringBuilder.append(getWrappedContent(link, URLSpan(link.destination)))
    }

    override fun visit(emphasis: Emphasis) {
        spannableStringBuilder.append(getWrappedContent(emphasis, StyleSpan(Typeface.ITALIC)))
    }

    private fun getWrappedContent(node: Node, span: Any): SpannableStringBuilder {
        val content = getNodeContent(node)
        content.setSpan(span, 0, content.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        return content
    }

    companion object {
        fun getNodeContent(node: Node): SpannableStringBuilder {
            val guideVisitor = GuideVisitorString()
            guideVisitor.visitChildren(node)

            return guideVisitor.spannableStringBuilder
        }
    }
}
