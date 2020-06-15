package io.horizontalsystems.bankwallet.modules.guideview

import android.text.SpannableStringBuilder
import android.text.Spanned
import io.noties.markwon.Markwon
import org.commonmark.node.*

class GuideVisitor(
        private val markwon: Markwon,
        private val listItemMarkerGenerator: ListItemMarkerGenerator? = null
) : AbstractVisitor() {

    val blocks = mutableListOf<GuideBlock>()

    val spannableStringBuilder = SpannableStringBuilder()
    private var quoted = false

    override fun visit(heading: Heading) {
        val guideVisitor = GuideVisitor(markwon)
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

        val guideVisitor = GuideVisitor(markwon)
        guideVisitor.visitChildren(paragraph)

        blocks.add(GuideBlock.Paragraph(guideVisitor.spannableStringBuilder, quoted))
    }

    override fun visit(text: Text) {
        spannableStringBuilder.append(markwon.render(text))
    }

    override fun visit(strongEmphasis: StrongEmphasis) {
        spannableStringBuilder.append(markwon.render(strongEmphasis))
    }

    override fun visit(link: Link) {
        spannableStringBuilder.append(markwon.render(link))
    }

    override fun visit(emphasis: Emphasis) {
        spannableStringBuilder.append(markwon.render(emphasis))
    }

    override fun visit(image: Image) {
        blocks.add(GuideBlock.Image(image.destination, image.title))
        spannableStringBuilder.append(markwon.render(image))
    }

    override fun visit(blockQuote: BlockQuote) {
        val guideVisitor = GuideVisitor(markwon)

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
        val guideVisitor = GuideVisitor(markwon, listItemMarkerGenerator)
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
        val guideVisitor = GuideVisitor(markwon)

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
