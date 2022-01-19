package io.horizontalsystems.bankwallet.ui.selector

import android.view.View
import android.widget.TextView
import io.horizontalsystems.bankwallet.R
import kotlinx.android.extensions.LayoutContainer

class SelectorOptionTextViewHolder<ItemClass>(override val containerView: View) : ItemViewHolder<ViewItemWrapper<ItemClass>>(containerView), LayoutContainer {

    override fun bind(selected: Boolean) {
        containerView.findViewById<TextView>(R.id.itemTitle)?.apply {
            text = item?.title
            isSelected = selected
        }
    }
}
