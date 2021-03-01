package io.horizontalsystems.bankwallet.modules.market.favorites

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R

class EmptyListAdapter(
        showEmptyListText: LiveData<Boolean>,
        viewLifecycleOwner: LifecycleOwner,
        private val viewHolderFactoryMethod: (parent: ViewGroup, viewType: Int) -> RecyclerView.ViewHolder
) : ListAdapter<Boolean, RecyclerView.ViewHolder>(diffCallback) {

    init {
        showEmptyListText.observe(viewLifecycleOwner, { show ->
            submitList(if (show) listOf(true) else listOf())
        })
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return viewHolderFactoryMethod(parent, viewType)
    }

    companion object{
        private val diffCallback = object : DiffUtil.ItemCallback<Boolean>() {
            override fun areItemsTheSame(oldItem: Boolean, newItem: Boolean): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Boolean, newItem: Boolean): Boolean {
                return oldItem == newItem
            }
        }
    }

}
