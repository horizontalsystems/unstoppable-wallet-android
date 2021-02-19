package io.horizontalsystems.bankwallet.ui.selector

import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment
import kotlinx.android.synthetic.main.dialog_selector.*

class SelectorBottomSheetDialog<ItemClass> : BaseBottomSheetDialogFragment() {

    var items: List<ItemClass>? = null
    var selectedItem: ItemClass? = null
    var onSelectListener: ((ItemClass) -> Unit)? = null

    var titleText: String = ""
    var subtitleText: String = ""
    @DrawableRes
    var headerIconResourceId: Int = 0

    lateinit var itemViewHolderFactory: ItemViewHolderFactory<ItemViewHolder<ItemClass>>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setContentView(R.layout.dialog_selector)

        setTitle(titleText)
        setSubtitle(subtitleText)
        setHeaderIcon(headerIconResourceId)
        setHeaderIconTint(R.color.jacob)

        items?.let {
            val itemsAdapter = SelectorAdapter(it, selectedItem, itemViewHolderFactory, {
                onSelectListener?.invoke(it)
                dismiss()
            })

            rvItems.adapter = itemsAdapter
        }
    }
}
