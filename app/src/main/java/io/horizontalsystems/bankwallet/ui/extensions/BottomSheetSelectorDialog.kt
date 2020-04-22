package io.horizontalsystems.bankwallet.ui.extensions

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.fragment_bottom_selector.*

class BottomSheetSelectorDialog(
        private val title: String,
        private val subtitle: String,
        private val icon: Int,
        private val items: List<Pair<String, String>>,
        private val selected: Int,
        private val listener: OnItemSelectedListener
) : BaseBottomSheetDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setContentView(R.layout.fragment_bottom_selector)

        setTitle(title)
        setSubtitle(subtitle)
        setHeaderIcon(icon)

        val itemsAdapter = SelectorItemsAdapter(items, selected)

        rvItems.adapter = itemsAdapter

        btnDone.setOnClickListener {
            listener.onItemClick(itemsAdapter.selected)
            dismiss()
        }
    }

    companion object {
        fun newInstance(title: String, subtitle: String, icon: Int, items: List<Pair<String, String>>, selected: Int, listener: OnItemSelectedListener): BottomSheetSelectorDialog {
            return BottomSheetSelectorDialog(title, subtitle, icon, items, selected, listener)
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
    fun onItemClick(position: Int)
}
