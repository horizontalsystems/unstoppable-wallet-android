package io.horizontalsystems.bankwallet.ui.selector

import android.view.View
import androidx.core.view.isVisible
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_selector_item.*

class SelectorItemViewHolder<ItemClass>(override val containerView: View) : ItemViewHolder<ViewItemWrapper<ItemClass>>(containerView), LayoutContainer {
    override fun bind(selected: Boolean) {
        title.text = item?.title

        item?.color?.let {
            title.setTextColor(containerView.context.getColor(it))
        }

        selectedIcon.isVisible = selected
    }
}
