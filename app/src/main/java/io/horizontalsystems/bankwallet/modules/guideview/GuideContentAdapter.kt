package io.horizontalsystems.bankwallet.modules.guideview

import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_guide_block.*

class GuideContentAdapter : ListAdapter<GuideBlock, GuideContentAdapter.ViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(inflate(parent, R.layout.view_holder_guide_block))
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

    class ViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(item: GuideBlock) {
            (containerView as? ViewGroup)?.children?.forEach {
                it.isVisible = false
            }

            when (item) {
                is GuideBlock.Heading1 -> {
                    h1.isVisible = true
                    h1.text = item.text
                    headingBottomBorder.isVisible = true
                }
                is GuideBlock.Heading2 -> {
                    h2.isVisible = true
                    h2.text = item.text
                    headingBottomBorder.isVisible = true
                }
                is GuideBlock.Paragraph -> {
                    paragraph.isVisible = true
                    paragraph.text = item.text
                }
                is GuideBlock.Image -> {
                    item.title?.let {
                        imageCaption.isVisible = true
                        imageCaption.text = it
                    }

                    image.isVisible = true
                    Picasso.get().load(item.destination).into(image)
                }
            }
        }

    }


}
