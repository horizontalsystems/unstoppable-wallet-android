package io.horizontalsystems.bankwallet.modules.guideview

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_guide_h1.*
import kotlinx.android.synthetic.main.view_holder_guide_h2.*
import kotlinx.android.synthetic.main.view_holder_guide_h3.*
import kotlinx.android.synthetic.main.view_holder_guide_image.*
import kotlinx.android.synthetic.main.view_holder_guide_paragraph.*

class GuideContentAdapter : ListAdapter<GuideBlock, GuideContentAdapter.ViewHolder>(diffCallback) {

    override fun getItemViewType(position: Int): Int {
        return when(getItem(position)) {
            is GuideBlock.Heading1 -> R.layout.view_holder_guide_h1
            is GuideBlock.Heading2 -> R.layout.view_holder_guide_h2
            is GuideBlock.Heading3 -> R.layout.view_holder_guide_h3
            is GuideBlock.Paragraph -> R.layout.view_holder_guide_paragraph
            is GuideBlock.Image -> R.layout.view_holder_guide_image
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when(viewType) {
            R.layout.view_holder_guide_h1 -> ViewHolderH1(inflate(parent, viewType))
            R.layout.view_holder_guide_h2 -> ViewHolderH2(inflate(parent, viewType))
            R.layout.view_holder_guide_h3 -> ViewHolderH3(inflate(parent, viewType))
            R.layout.view_holder_guide_paragraph -> ViewHolderParagraph(inflate(parent, viewType))
            R.layout.view_holder_guide_image -> ViewHolderImage(inflate(parent, viewType))
            else -> throw Exception("Undefined viewType: $viewType")
        }

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<GuideBlock>() {
            override fun areItemsTheSame(oldItem: GuideBlock, newItem: GuideBlock): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: GuideBlock, newItem: GuideBlock): Boolean {
                return oldItem == newItem
            }

        }
    }

    abstract class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        abstract fun bind(item: GuideBlock)
    }

    class ViewHolderH1(override val containerView: View) : ViewHolder(containerView), LayoutContainer {
        override fun bind(item: GuideBlock) {
            if (item !is GuideBlock.Heading1) return

            h1.text = item.text
        }
    }

    class ViewHolderH2(override val containerView: View) : ViewHolder(containerView), LayoutContainer {
        override fun bind(item: GuideBlock) {
            if (item !is GuideBlock.Heading2) return

            h2.text = item.text
        }
    }
    class ViewHolderH3(override val containerView: View) : ViewHolder(containerView), LayoutContainer {
        override fun bind(item: GuideBlock) {
            if (item !is GuideBlock.Heading3) return

            h3.text = item.text
        }
    }

    class ViewHolderImage(override val containerView: View) : ViewHolder(containerView), LayoutContainer {
        override fun bind(item: GuideBlock) {
            if (item !is GuideBlock.Image) return

            if (item.title == null) {
                imageCaption.isVisible = false
            } else {
                imageCaption.isVisible = true
                imageCaption.text = item.title
            }

            Picasso.get().load(item.destination).into(image)
        }
    }

    class ViewHolderParagraph(override val containerView: View) : ViewHolder(containerView), LayoutContainer {
        override fun bind(item: GuideBlock) {
            if (item !is GuideBlock.Paragraph) return

            paragraph.isVisible = true
            paragraph.text = item.text
        }
    }


}
