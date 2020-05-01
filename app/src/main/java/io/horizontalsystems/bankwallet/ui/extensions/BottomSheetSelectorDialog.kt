package io.horizontalsystems.bankwallet.ui.extensions

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.fragment_bottom_selector.*

class BottomSheetSelectorDialog(
        private val title: String,
        private val subtitle: String,
        private val icon: Int,
        private val items: List<Pair<String, String>>,
        private val selected: Int,
        private val listener: OnItemSelectedListener,
        private val onCancelled: (() -> Unit)?,
        private val warning: String?,
        private val notifyUnchanged: Boolean
) : BaseBottomSheetDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setContentView(R.layout.fragment_bottom_selector)

        setTitle(title)
        setSubtitle(subtitle)
        setHeaderIcon(icon)

        val itemsAdapter = SelectorItemsAdapter(items, selected)

        textWarning.text = warning
        textWarning.visibility = if (warning == null) View.GONE else View.VISIBLE

        rvItems.adapter = itemsAdapter

        btnDone.setOnClickListener {
            if (notifyUnchanged || itemsAdapter.selected != selected) {
                listener.onItemSelected(itemsAdapter.selected)
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
        fun show(fragmentManager: FragmentManager, title: String, subtitle: String, icon: Int, items: List<Pair<String, String>>, selected: Int, listener: OnItemSelectedListener, onCancelled: (() -> Unit)? = null, warning: String? = null, notifyUnchanged: Boolean = false) {
            BottomSheetSelectorDialog(title, subtitle, icon, items, selected, listener, onCancelled, warning, notifyUnchanged)
                    .show(fragmentManager, "selector_dialog")
        }
    }
}

class SelectorItemsAdapter(private val items: List<Pair<String, String>>, var selected: Int) : RecyclerView.Adapter<ItemViewHolder>(), OnItemClickListener {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(SettingItemWithCheckmark(parent.context), this)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) = Unit

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            holder.bind(items[position].first, items[position].second, position == selected, position == itemCount - 1)
        } else {
            holder.bindSelected(position == selected)
        }
    }

    override fun onItemClick(position: Int) {
        if (selected == position) return

        notifyItemChanged(selected, true)
        notifyItemChanged(position, true)

        selected = position
    }
}

class ItemViewHolder(private val settingItemWithCheckmark: SettingItemWithCheckmark, onClickListener: OnItemClickListener) : RecyclerView.ViewHolder(settingItemWithCheckmark) {

    init {
        settingItemWithCheckmark.setOnClickListener {
            onClickListener.onItemClick(adapterPosition)
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

interface OnItemClickListener {
    fun onItemClick(position: Int)
}

interface OnItemSelectedListener {
    fun onItemSelected(position: Int)
}
