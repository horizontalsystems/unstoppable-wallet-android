package io.horizontalsystems.bankwallet.modules.guideview

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.URLSpan
import org.commonmark.node.*

class GuideVisitor(private val listItemMarkerGenerator: ListItemMarkerGenerator? = null) : AbstractVisitor() {

    val blocks = mutableListOf<GuideBlock>()

    private val spannableStringBuilder = SpannableStringBuilder()
    private var quoted = false

    override fun visit(heading: Heading) {
        val guideVisitor = GuideVisitor()
        guideVisitor.visitChildren(heading)

        val block = when (heading.level) {
            1 -> GuideBlock.Heading1(guideVisitor.spannableStringBuilder)
            2 -> GuideBlock.Heading2(guideVisitor.spannableStringBuilder)
            3 -> GuideBlock.Heading3(guideVisitor.spannableStringBuilder)
            else -> null
        }

        block?.let {
            blocks.add(block)
        }
    }

    override fun visit(paragraph: Paragraph) {
        val firstChild = paragraph.firstChild
        if (firstChild is Image && firstChild.next == null) {
            return visit(firstChild)
        }

        val guideVisitor = GuideVisitor()
        guideVisitor.visitChildren(paragraph)

        blocks.add(GuideBlock.Paragraph(guideVisitor.spannableStringBuilder, quoted))
    }

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

    override fun visit(image: Image) {
        blocks.add(GuideBlock.Image(image.destination, image.title))
    }

    override fun visit(blockQuote: BlockQuote) {
        val guideVisitor = GuideVisitor()

        guideVisitor.visitChildren(blockQuote)

        guideVisitor.blocks.let { subblocks ->
            subblocks.forEach { it.quoted = true }
            subblocks.firstOrNull()?.quotedFirst = true
            subblocks.lastOrNull()?.quotedLast = true

            blocks.addAll(subblocks)
        }
    }


    override fun visit(bulletList: BulletList) {
        visitListBlock(bulletList, ListItemMarkerGenerator.Unordered)
    }

    override fun visit(orderedList: OrderedList) {
        visitListBlock(orderedList, ListItemMarkerGenerator.Ordered(orderedList.startNumber, orderedList.delimiter))
    }

    private fun visitListBlock(listBlock: ListBlock, listItemMarkerGenerator: ListItemMarkerGenerator) {
        val guideVisitor = GuideVisitor(listItemMarkerGenerator)
        guideVisitor.visitChildren(listBlock)
        guideVisitor.blocks.let { subblocks ->
            if (listBlock.isTight) {
                subblocks.forEach {
                    it.listTightTop = true
                    it.listTightBottom = true
                }
                subblocks.firstOrNull()?.listTightTop = false
                subblocks.lastOrNull()?.listTightBottom = false
            }

            blocks.addAll(subblocks)
        }
    }


    override fun visit(listItem: ListItem) {
        val guideVisitor = GuideVisitor()

        guideVisitor.visitChildren(listItem)
        guideVisitor.blocks.let { subblocks ->
            subblocks.forEach {
                it.listItem = true
            }
            subblocks.firstOrNull()?.listItemMarker = getNextListItemMarker()
            blocks.addAll(subblocks)
        }
    }

    private fun getNextListItemMarker(): String? {
        return listItemMarkerGenerator?.getNext()
    }

    private fun getWrappedContent(node: Node, span: Any): SpannableStringBuilder {
        val guideVisitor = GuideVisitor()
        guideVisitor.visitChildren(node)

        val content = guideVisitor.spannableStringBuilder
        content.setSpan(span, 0, content.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        return content
    }
}

abstract class ListItemMarkerGenerator {
    abstract fun getNext(): String

    object Unordered : ListItemMarkerGenerator() {
        override fun getNext() = "• "
    }

    class Ordered(private var startNumber: Int, private val delimiter: Char) : ListItemMarkerGenerator() {
        override fun getNext(): String {
            return "${startNumber++}$delimiter "
        }
    }

}

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
    data class Image(val destination: String, val title: String?) : GuideBlock()
}
