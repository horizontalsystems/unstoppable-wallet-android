package io.horizontalsystems.bankwallet.modules.market.overview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R

class PoweredByAdapter(
        showPoweredBy: LiveData<Boolean>,
        viewLifecycleOwner: LifecycleOwner
) : ListAdapter<Boolean, PoweredByAdapter.ViewHolder>(diffCallback) {

    init {
        showPoweredBy.observe(viewLifecycleOwner, { show ->
            submitList(if (show) listOf(true) else listOf())
        })
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = Unit

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.create(parent)
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

    class ViewHolder(containerView: View) : RecyclerView.ViewHolder(containerView) {
        companion object {
            fun create(parent: ViewGroup): ViewHolder {
                return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_market_powered_by, parent, false))
            }
        }
    }

}
