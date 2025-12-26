package io.horizontalsystems.bankwallet.modules.markdown

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.URLSpan
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Emphasis
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text

class MarkdownVisitorString(private val markdownUrl: String) : AbstractVisitor() {
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
        val content = getNodeContent(node, markdownUrl)
        content.setSpan(span, 0, content.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        return content
    }

    companion object {
        fun getNodeContent(node: Node, markdownUrl: String): SpannableStringBuilder {
            val markdownVisitor = MarkdownVisitorString(markdownUrl)
            markdownVisitor.visitChildren(node)

            return markdownVisitor.spannableStringBuilder
        }
    }
}
