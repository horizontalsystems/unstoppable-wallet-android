package io.horizontalsystems.bankwallet.modules.settings.guides

import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.databinding.ViewHolderGuidePreviewBinding
import io.horizontalsystems.bankwallet.entities.Guide
import io.horizontalsystems.core.helpers.DateHelper

class ViewHolderGuide(
    private val binding: ViewHolderGuidePreviewBinding,
    private val listener: ClickListener
) : RecyclerView.ViewHolder(binding.root) {

    interface ClickListener {
        fun onClick(guide: Guide)
    }

    private var guide: Guide? = null

    init {
        binding.wrapper.setOnSingleClickListener {
            guide?.let {
                listener.onClick(it)
            }
        }
    }

    fun bind(guide: Guide) {
        this.guide = guide

        binding.title.text = guide.title
        binding.date.text = DateHelper.shortDate(guide.updatedAt)

        binding.image.setImageDrawable(null)

        guide.imageUrl?.let {
            Picasso.get().load(it).into(binding.image)
        }

    }

}
