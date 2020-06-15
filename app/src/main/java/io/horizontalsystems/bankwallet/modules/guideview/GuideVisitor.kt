package io.horizontalsystems.bankwallet.modules.guideview

import android.text.SpannableStringBuilder
import android.text.Spanned
import io.noties.markwon.Markwon
import org.commonmark.node.*

class GuideVisitor(private val markwon: Markwon) : AbstractVisitor() {

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
        quoted = true
        val blocksCount = blocks.size
        super.visit(blockQuote)
        quoted = false

        val updatedBlocksCount = blocks.size
        if (updatedBlocksCount == blocksCount) return

        blocks[blocksCount].quotedFirst = true
        blocks[updatedBlocksCount - 1].quotedLast = true
    }
}

sealed class GuideBlock {
    var quoted = false
    var quotedFirst = false
    var quotedLast = false

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
