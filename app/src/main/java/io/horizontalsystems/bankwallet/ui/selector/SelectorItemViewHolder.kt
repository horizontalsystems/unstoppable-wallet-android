package io.horizontalsystems.bankwallet.ui.selector

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import kotlinx.android.extensions.LayoutContainer

class SelectorItemViewHolder<ItemClass>(override val containerView: View) : ItemViewHolder<ViewItemWrapper<ItemClass>>(containerView), LayoutContainer {

    private var title: TextView = containerView.findViewById(R.id.title)
    private var selectedIcon: ImageView = containerView.findViewById(R.id.selectedIcon)

    override fun bind(selected: Boolean) {
        title.text = item?.title

        item?.color?.let {
            title.setTextColor(containerView.context.getColor(it))
        }

        selectedIcon.isVisible = selected
    }
}
