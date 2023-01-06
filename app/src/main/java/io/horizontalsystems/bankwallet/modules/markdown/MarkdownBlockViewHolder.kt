package io.horizontalsystems.bankwallet.modules.markdown

import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.TOP
import androidx.core.text.getSpans
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.ImageRequest
import coil.request.SuccessResult
import io.horizontalsystems.bankwallet.databinding.*
import io.horizontalsystems.bankwallet.ui.helpers.LayoutHelper
import org.apache.commons.io.FilenameUtils
import java.net.URL

abstract class MarkdownBlockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: MarkdownBlock)
}

class ViewHolderFooter(binding: ViewHolderMarkdownFooterBinding) :
    MarkdownBlockViewHolder(binding.root) {
    override fun bind(item: MarkdownBlock) {}
}

class ViewHolderH1(private val binding: ViewHolderMarkdownH1Binding) :
    MarkdownBlockViewHolder(binding.root) {
    override fun bind(item: MarkdownBlock) {
        if (item !is MarkdownBlock.Heading1) return

        binding.h1.text = item.text
    }
}

class ViewHolderH2(private val binding: ViewHolderMarkdownH2Binding) :
    MarkdownBlockViewHolder(binding.root) {
    override fun bind(item: MarkdownBlock) {
        if (item !is MarkdownBlock.Heading2) return

        binding.h2.text = item.text
    }
}

class ViewHolderH3(private val binding: ViewHolderMarkdownH3Binding) :
    MarkdownBlockViewHolder(binding.root) {
    override fun bind(item: MarkdownBlock) {
        if (item !is MarkdownBlock.Heading3) return

        binding.h3.text = item.text
    }
}

class ViewHolderImage(private val binding: ViewHolderMarkdownImageBinding) :
    MarkdownBlockViewHolder(binding.root) {
    private val ratios = mapOf(
        "l" to "4:3",
        "p" to "9:16",
        "s" to "1:1"
    )

    override fun bind(item: MarkdownBlock) {
        if (item !is MarkdownBlock.Image) return

        setConstraints(item.destination, item.mainImage)

        binding.placeholder.isVisible = true
        binding.image.setImageDrawable(null)

        if (item.title == null) {
            binding.imageCaption.isVisible = false
        } else {
            binding.imageCaption.isVisible = true
            binding.imageCaption.text = item.title
        }

        binding.image.load(item.destination) {
            listener(object : ImageRequest.Listener {
                override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                    super.onSuccess(request, result)
                    binding.placeholder.isVisible = false
                }
            })
        }
    }

    private fun setConstraints(destination: String, mainImage: Boolean) {
        val baseName = FilenameUtils.getBaseName(URL(destination).path)
        val suffix = baseName.split("-").last()

        val set = ConstraintSet()
        set.clone(binding.wrapper)
        set.setDimensionRatio(binding.image.id, ratios[suffix] ?: "1:1")
        set.setMargin(
            binding.image.id,
            TOP,
            if (mainImage) 0 else LayoutHelper.dp(12f, binding.wrapper.context)
        )
        set.applyTo(binding.wrapper)
    }
}

class ViewHolderParagraph(
    private val binding: ViewHolderMarkdownParagraphBinding,
    private val listener: MarkdownContentAdapter.Listener,
    private val handleRelativeUrl: Boolean
) : MarkdownBlockViewHolder(binding.root) {
    private val blockQuoteVerticalPadding = LayoutHelper.dp(12f, binding.wrapper.context)
    private val listItemIndent = LayoutHelper.dp(24f, binding.wrapper.context)

    override fun bind(item: MarkdownBlock) {
        if (item !is MarkdownBlock.Paragraph) return

        val text = item.text

        val spans = text.getSpans<URLSpan>(0, text.length)

        if (handleRelativeUrl) {
            spans.forEach {
                handleLinkToGuideInApp(text, it)
            }
        }

        binding.paragraph.text = text
        binding.paragraph.movementMethod = LinkMovementMethod.getInstance()

        blockquote(item)
        listItem(item)
    }

    private fun handleLinkToGuideInApp(strBuilder: SpannableStringBuilder, span: URLSpan) {
        if (!span.url.endsWith("md")) return

        val start = strBuilder.getSpanStart(span)
        val end = strBuilder.getSpanEnd(span)
        val flags = strBuilder.getSpanFlags(span)
        val clickable = object : ClickableSpan() {
            override fun onClick(view: View) {
                listener.onClick(span.url)
            }
        }
        strBuilder.setSpan(clickable, start, end, flags)
        strBuilder.removeSpan(span)
    }


    private fun listItem(item: MarkdownBlock) {
        val leftPadding = if (item.listItem) listItemIndent else 0
        val topPadding = if (item.listTightTop) 0 else LayoutHelper.dp(12f, binding.wrapper.context)
        val bottomPadding =
            if (item.listTightBottom) 0 else LayoutHelper.dp(12f, binding.wrapper.context)

        binding.paragraph.setPadding(leftPadding, topPadding, 0, bottomPadding)

        binding.listItemMarker.text = item.listItemMarker
        binding.listItemMarker.isVisible = item.listItemMarker != null
    }

    private fun blockquote(item: MarkdownBlock) {
        binding.quoted.isVisible = item.quoted

        val topPadding = if (item.quotedFirst) blockQuoteVerticalPadding else 0
        val bottomPadding = if (item.quotedLast) blockQuoteVerticalPadding else 0

        binding.wrapper.setPadding(0, topPadding, 0, bottomPadding)
    }
}