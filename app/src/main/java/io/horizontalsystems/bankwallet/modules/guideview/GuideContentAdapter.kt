package io.horizontalsystems.bankwallet.modules.guideview

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.inflate

class GuideContentAdapter(private val listener: Listener) : ListAdapter<GuideBlock, GuideBlockViewHolder>(diffCallback) {

    interface Listener {
        fun onGuideClick(url: String)
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is GuideBlock.Heading1 -> R.layout.view_holder_guide_h1
        is GuideBlock.Heading2 -> R.layout.view_holder_guide_h2
        is GuideBlock.Heading3 -> R.layout.view_holder_guide_h3
        is GuideBlock.Paragraph -> R.layout.view_holder_guide_paragraph
        is GuideBlock.Image -> R.layout.view_holder_guide_image
        is GuideBlock.Footer -> R.layout.view_holder_guide_footer
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuideBlockViewHolder = when (viewType) {
        R.layout.view_holder_guide_h1 -> ViewHolderH1(inflate(parent, viewType))
        R.layout.view_holder_guide_h2 -> ViewHolderH2(inflate(parent, viewType))
        R.layout.view_holder_guide_h3 -> ViewHolderH3(inflate(parent, viewType))
        R.layout.view_holder_guide_paragraph -> ViewHolderParagraph(inflate(parent, viewType), listener)
        R.layout.view_holder_guide_image -> ViewHolderImage(inflate(parent, viewType))
        R.layout.view_holder_guide_footer -> ViewHolderFooter(inflate(parent, viewType))
        else -> throw Exception("Undefined viewType: $viewType")
    }

    override fun onBindViewHolder(holder: GuideBlockViewHolder, position: Int) {
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

}

