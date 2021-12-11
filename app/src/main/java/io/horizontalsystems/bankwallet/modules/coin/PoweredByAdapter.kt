package io.horizontalsystems.bankwallet.modules.coin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R

class PoweredByAdapter(
        showPoweredBy: LiveData<Boolean>,
        viewLifecycleOwner: LifecycleOwner,
        private val poweredByText: String
) : ListAdapter<Boolean, PoweredByAdapter.ViewHolder>(diffCallback) {

    init {
        showPoweredBy.observe(viewLifecycleOwner, { show ->
            submitList(if (show) listOf(true) else listOf())
        })
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int){
        holder.bind(poweredByText)
    }

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

    class ViewHolder(private val containerView: View) : RecyclerView.ViewHolder(containerView) {

        fun bind(text: String) {
            containerView.findViewById<TextView>(R.id.txtViewPoweredBy).text = text
        }

        companion object {
            fun create(parent: ViewGroup): ViewHolder {
                return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_market_powered_by, parent, false))
            }
        }
    }

}
