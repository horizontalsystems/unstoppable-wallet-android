package io.horizontalsystems.bankwallet.ui.selector

import android.view.View
import android.widget.TextView
import io.horizontalsystems.bankwallet.R
import kotlinx.android.extensions.LayoutContainer

class SelectorOptionTextViewHolder<ItemClass>(override val containerView: View) : ItemViewHolder<ViewItemWrapper<ItemClass>>(containerView), LayoutContainer {

    private val itemTitle = containerView.findViewById<TextView>(R.id.itemTitle)

    override fun bind(selected: Boolean) {
        itemTitle.text = item?.title
        itemTitle.isSelected = selected
    }
}
