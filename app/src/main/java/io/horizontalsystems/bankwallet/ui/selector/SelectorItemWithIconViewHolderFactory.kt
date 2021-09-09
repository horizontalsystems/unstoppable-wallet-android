package io.horizontalsystems.bankwallet.ui.selector

import android.view.ViewGroup
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.inflate

class SelectorItemWithIconViewHolderFactory<ItemClass> :
    ItemViewHolderFactory<ItemViewHolder<ViewItemWithIconWrapper<ItemClass>>> {
    override fun create(parent: ViewGroup, viewType: Int): ItemViewHolder<ViewItemWithIconWrapper<ItemClass>> {
        return SelectorItemWithIconViewHolder(inflate(parent, R.layout.view_holder_selector_item_with_icon))
    }
}
