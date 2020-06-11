package io.horizontalsystems.bankwallet.modules.guides

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.core.helpers.DateHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_guide_preview.*

class ViewHolderGuide(override val containerView: View, private val listener: ClickListener) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    interface ClickListener {
        fun onClick(position: Int)
    }

    init {
        containerView.setOnSingleClickListener { listener.onClick(bindingAdapterPosition) }
    }

    fun bind(item: GuideViewItem) {
        title.text = item.title
        date.text = DateHelper.shortDate(item.date)

        Picasso.get().load(item.imageUrl).into(image)
    }

}
