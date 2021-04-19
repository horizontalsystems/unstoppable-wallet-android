package io.horizontalsystems.bankwallet.ui.selector

import android.view.View
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_item_selector.*

class SelectorOptionTextViewHolder<ItemClass>(override val containerView: View) : ItemViewHolder<ViewItemWrapper<ItemClass>>(containerView), LayoutContainer {

    override fun bind(selected: Boolean) {
        itemTitle.text = item?.title
        itemTitle.isSelected = selected
    }
}
