package io.horizontalsystems.bankwallet.ui.selector

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener

class SelectorAdapter<ItemClass>(
        private val items: List<ItemClass>,
        private var selectedItem: ItemClass?,
        private val itemViewHolderFactory: ItemViewHolderFactory<ItemViewHolder<ItemClass>>,
        private val onSelectListener: ((ItemClass) -> Unit))
    : RecyclerView.Adapter<ItemViewHolder<ItemClass>>() {

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder<ItemClass> {
        val viewHolder = itemViewHolderFactory.create(parent, viewType)
        viewHolder.itemView.setOnSingleClickListener {
            viewHolder.item?.let {
                selectedItem = it
                notifyDataSetChanged()

                onSelectListener(it)
            }
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: ItemViewHolder<ItemClass>, position: Int) {
        val item = items[position]
        holder.item = item
        holder.bind(selectedItem == item)
    }
}

abstract class ItemViewHolder<ItemClass>(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var item: ItemClass? = null

    abstract fun bind(selected: Boolean)
}

interface ItemViewHolderFactory<VH : RecyclerView.ViewHolder> {
    fun create(parent: ViewGroup, viewType: Int): VH
}



