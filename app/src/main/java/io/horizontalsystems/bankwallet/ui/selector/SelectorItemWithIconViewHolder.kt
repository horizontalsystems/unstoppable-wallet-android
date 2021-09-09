package io.horizontalsystems.bankwallet.ui.selector

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_selector_item_with_icon.*

class SelectorItemWithIconViewHolder<ItemClass>(override val containerView: View) :
    ItemViewHolder<ViewItemWithIconWrapper<ItemClass>>(containerView), LayoutContainer {
    override fun bind(selected: Boolean) {
        title.text = item?.title

        item?.iconName?.let {
            image.setImageResource(getDrawableResource(containerView.context, it))
        }
        checkmarkIcon.isVisible = selected
    }

    private fun getDrawableResource(context: Context, name: String): Int {
        return context.resources.getIdentifier(name, "drawable", context.packageName)
    }
}
