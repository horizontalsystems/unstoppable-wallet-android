package io.horizontalsystems.bankwallet.modules.markdown

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.inflate

class MarkdownContentAdapter(private val listener: Listener) : ListAdapter<MarkdownBlock, MarkdownBlockViewHolder>(diffCallback) {

    interface Listener {
        fun onClick(url: String)
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is MarkdownBlock.Heading1 -> R.layout.view_holder_markdown_h1
        is MarkdownBlock.Heading2 -> R.layout.view_holder_markdown_h2
        is MarkdownBlock.Heading3 -> R.layout.view_holder_markdown_h3
        is MarkdownBlock.Paragraph -> R.layout.view_holder_markdown_paragraph
        is MarkdownBlock.Image -> R.layout.view_holder_markdown_image
        is MarkdownBlock.Footer -> R.layout.view_holder_markdown_footer
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkdownBlockViewHolder = when (viewType) {
        R.layout.view_holder_markdown_h1 -> ViewHolderH1(inflate(parent, viewType))
        R.layout.view_holder_markdown_h2 -> ViewHolderH2(inflate(parent, viewType))
        R.layout.view_holder_markdown_h3 -> ViewHolderH3(inflate(parent, viewType))
        R.layout.view_holder_markdown_paragraph -> ViewHolderParagraph(inflate(parent, viewType), listener)
        R.layout.view_holder_markdown_image -> ViewHolderImage(inflate(parent, viewType))
        R.layout.view_holder_markdown_footer -> ViewHolderFooter(inflate(parent, viewType))
        else -> throw Exception("Undefined viewType: $viewType")
    }

    override fun onBindViewHolder(holder: MarkdownBlockViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<MarkdownBlock>() {
            override fun areItemsTheSame(oldItem: MarkdownBlock, newItem: MarkdownBlock): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: MarkdownBlock, newItem: MarkdownBlock): Boolean {
                return oldItem == newItem
            }
        }
    }

}

