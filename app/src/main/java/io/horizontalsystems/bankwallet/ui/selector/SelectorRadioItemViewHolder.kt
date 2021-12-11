package io.horizontalsystems.bankwallet.ui.selector

import android.view.View
import io.horizontalsystems.bankwallet.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_manage_account_item.*

class SelectorRadioItemViewHolder<ItemClass>(override val containerView: View) : ItemViewHolder<ViewItemWrapper<ItemClass>>(containerView), LayoutContainer {
    override fun bind(selected: Boolean) {
        title.text = item?.title
        subtitle.text = item?.subtitle
        radioImage.setImageResource(if (selected) R.drawable.ic_radion else R.drawable.ic_radioff)

        item?.color?.let {
            title.setTextColor(containerView.context.getColor(it))
        }
    }
}
