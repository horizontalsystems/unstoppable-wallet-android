package io.horizontalsystems.bankwallet.ui.extensions

import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_bottom_selector.*
import kotlinx.android.synthetic.main.view_holder_setting_with_checkmark_wrapper.*

class BottomSheetSelectorDialog(
        private val title: String,
        private val subtitle: String,
        private val icon: Drawable?,
        private val items: List<BottomSheetSelectorViewItem>,
        private val selected: Int,
        private val onItemSelected: (Int) -> Unit,
        private val onCancelled: (() -> Unit)?,
        private val warning: String?,
        private val notifyUnchanged: Boolean
) : BaseBottomSheetDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setContentView(R.layout.fragment_bottom_selector)

        setTitle(title)
        setSubtitle(subtitle)
        setHeaderIconDrawable(icon)

        val itemsAdapter = SelectorItemsAdapter(items, selected)

        textWarning.text = warning
        textWarning.isVisible = warning != null
        divider.isVisible = warning != null

        rvItems.adapter = itemsAdapter

        btnDone.setOnClickListener {
            if (notifyUnchanged || itemsAdapter.selected != selected) {
                onItemSelected(itemsAdapter.selected)
            }
            dismiss()
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onCancelled?.invoke()
    }

    override fun close() {
        super.close()
        onCancelled?.invoke()
    }

    companion object {
        fun show(fragmentManager: FragmentManager, title: String, subtitle: String, icon: Drawable?, items: List<BottomSheetSelectorViewItem>, selected: Int, onItemSelected: (Int) -> Unit, onCancelled: (() -> Unit)? = null, warning: String? = null, notifyUnchanged: Boolean = false) {
            BottomSheetSelectorDialog(title, subtitle, icon, items, selected, onItemSelected, onCancelled, warning, notifyUnchanged)
                    .show(fragmentManager, "selector_dialog")
        }
    }
}

class SelectorItemsAdapter(private val items: List<BottomSheetSelectorViewItem>, var selected: Int) : RecyclerView.Adapter<ItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(inflate(parent, R.layout.view_holder_setting_with_checkmark_wrapper, false)) { position ->
            if (selected == position) return@ItemViewHolder

            notifyItemChanged(selected, true)
            notifyItemChanged(position, true)

            selected = position
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) = Unit

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            holder.bind(items[position].title, items[position].subtitle, position == selected)
        } else {
            holder.bindSelected(position == selected)
        }
    }

}

class ItemViewHolder(override val containerView: View, val onItemClick: (Int) -> Unit)
    : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        containerView.setOnClickListener {
            onItemClick(bindingAdapterPosition)
        }
    }

    fun bind(title: String, subtitle: String, selected: Boolean) {
        itemWithCheckmark.bind(title, subtitle, selected)
    }

    fun bindSelected(selected: Boolean) {
        itemWithCheckmark.setChecked(selected)
    }
}

data class BottomSheetSelectorViewItem (val title: String, val subtitle: String)