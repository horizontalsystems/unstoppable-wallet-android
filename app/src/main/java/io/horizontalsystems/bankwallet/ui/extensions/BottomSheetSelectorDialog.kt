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
import kotlinx.android.synthetic.main.fragment_bottom_selector.*

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
        return ItemViewHolder(SettingItemWithCheckmark(parent.context)) { position ->
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
            holder.bind(items[position].title, items[position].subtitle, position == selected, position == itemCount - 1)
        } else {
            holder.bindSelected(position == selected)
        }
    }

}

class ItemViewHolder(private val settingItemWithCheckmark: SettingItemWithCheckmark, val onItemClick: (Int) -> Unit) : RecyclerView.ViewHolder(settingItemWithCheckmark) {

    init {
        settingItemWithCheckmark.setOnClickListener {
            onItemClick(bindingAdapterPosition)
        }
    }

    fun bind(title: String, subtitle: String, selected: Boolean, showBottomBorder: Boolean) {
        settingItemWithCheckmark.setTitle(title)
        settingItemWithCheckmark.setSubtitle(subtitle)
        settingItemWithCheckmark.setChecked(selected)
        settingItemWithCheckmark.toggleBottomBorder(showBottomBorder)
    }

    fun bindSelected(selected: Boolean) {
        settingItemWithCheckmark.setChecked(selected)
    }
}

data class BottomSheetSelectorViewItem (val title: String, val subtitle: String)