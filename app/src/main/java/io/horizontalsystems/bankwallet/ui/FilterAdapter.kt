package io.horizontalsystems.bankwallet.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R

class FilterAdapter(private var listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    ViewHolderFilter.ClickListener {

    interface Listener {
        fun onFilterItemClick(item: FilterItem?, itemPosition: Int, itemWidth: Int)
    }

    open class FilterItem(val filterId: String) {
        override fun equals(other: Any?) = when (other) {
            is FilterItem -> filterId == other.filterId
            else -> false
        }

        override fun hashCode(): Int {
            return filterId.hashCode()
        }
    }

    var filterChangeable = true

    private var selectedFilterItem: FilterItem? = null
    private var filters: List<FilterItem?> = listOf()

    fun setFilters(filters: List<FilterItem?>, selectedFieldItem: FilterItem? = null) {
        this.filters = filters
        this.selectedFilterItem = selectedFieldItem ?: filters.firstOrNull()
        notifyDataSetChanged()
    }

    override fun getItemCount() = filters.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        ViewHolderFilter(
            LayoutInflater.from(parent.context).inflate(R.layout.view_holder_filter, parent, false),
            this
        )

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderFilter -> {
                holder.bind(filters[position]?.filterId, selectedFilterItem == filters[position])
            }
        }
    }

    override fun onClickItem(position: Int, width: Int) {
        if (filterChangeable) {
            listener.onFilterItemClick(filters[position], position, width)
            selectedFilterItem = filters[position]
            notifyDataSetChanged()
        }
    }
}