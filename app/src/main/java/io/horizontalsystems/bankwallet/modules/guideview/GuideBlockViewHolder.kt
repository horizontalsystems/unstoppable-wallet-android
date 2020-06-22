package io.horizontalsystems.bankwallet.modules.guideview

import android.text.method.LinkMovementMethod
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.TOP
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_guide_h1.*
import kotlinx.android.synthetic.main.view_holder_guide_h2.*
import kotlinx.android.synthetic.main.view_holder_guide_h3.*
import kotlinx.android.synthetic.main.view_holder_guide_image.*
import kotlinx.android.synthetic.main.view_holder_guide_paragraph.*
import org.apache.commons.io.FilenameUtils
import java.net.URL

abstract class GuideBlockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: GuideBlock)
}

class ViewHolderH1(override val containerView: View) : GuideBlockViewHolder(containerView), LayoutContainer {
    override fun bind(item: GuideBlock) {
        if (item !is GuideBlock.Heading1) return

        h1.text = item.text
    }
}

class ViewHolderH2(override val containerView: View) : GuideBlockViewHolder(containerView), LayoutContainer {
    override fun bind(item: GuideBlock) {
        if (item !is GuideBlock.Heading2) return

        h2.text = item.text
    }
}

class ViewHolderH3(override val containerView: View) : GuideBlockViewHolder(containerView), LayoutContainer {
    override fun bind(item: GuideBlock) {
        if (item !is GuideBlock.Heading3) return

        h3.text = item.text
    }
}

class ViewHolderImage(override val containerView: View) : GuideBlockViewHolder(containerView), LayoutContainer {
    private val ratios = mapOf(
            "l" to "4:3",
            "p" to "9:16",
            "s" to "1:1"
    )

    override fun bind(item: GuideBlock) {
        if (item !is GuideBlock.Image) return

        setConstraints(item.destination, item.mainImage)

        placeholder.isVisible = true
        image.setImageDrawable(null)

        if (item.title == null) {
            imageCaption.isVisible = false
        } else {
            imageCaption.isVisible = true
            imageCaption.text = item.title
        }

        Picasso.get().load(item.destination)
                .into(image, object : Callback.EmptyCallback() {
                    override fun onSuccess() {
                        placeholder.isVisible = false
                    }
                })
    }

    private fun setConstraints(destination: String, mainImage: Boolean) {
        if (containerView is ConstraintLayout) {
            val baseName = FilenameUtils.getBaseName(URL(destination).path)
            val suffix = baseName.split("-").last()

            val set = ConstraintSet()
            set.clone(containerView)
            set.setDimensionRatio(image.id, ratios[suffix] ?: "1:1")
            set.setMargin(image.id, TOP, if (mainImage) 0 else LayoutHelper.dp(12f, containerView.context))
            set.applyTo(containerView)
        }
    }
}

class ViewHolderParagraph(override val containerView: View) : GuideBlockViewHolder(containerView), LayoutContainer {
    private val blockQuoteVerticalPadding = LayoutHelper.dp(12f, containerView.context)
    private val listItemIndent = LayoutHelper.dp(24f, containerView.context)

    override fun bind(item: GuideBlock) {
        if (item !is GuideBlock.Paragraph) return

        paragraph.text = item.text
        paragraph.movementMethod = LinkMovementMethod.getInstance()

        blockquote(item)
        listItem(item)
    }

    private fun listItem(item: GuideBlock) {
        val leftPadding = if (item.listItem) listItemIndent else 0
        val topPadding = if (item.listTightTop) 0 else LayoutHelper.dp(12f, containerView.context)
        val bottomPadding = if (item.listTightBottom) 0 else LayoutHelper.dp(12f, containerView.context)

        paragraph.setPadding(leftPadding, topPadding, 0, bottomPadding)

        listItemMarker.text = item.listItemMarker
        listItemMarker.isVisible = item.listItemMarker != null
    }

    private fun blockquote(item: GuideBlock) {
        quoted.isVisible = item.quoted

        val topPadding = if (item.quotedFirst) blockQuoteVerticalPadding else 0
        val bottomPadding = if (item.quotedLast) blockQuoteVerticalPadding else 0

        containerView.setPadding(0, topPadding, 0, bottomPadding)
    }
}