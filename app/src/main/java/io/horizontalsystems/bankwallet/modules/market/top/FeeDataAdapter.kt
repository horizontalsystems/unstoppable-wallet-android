package io.horizontalsystems.bankwallet.modules.market.top

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer

class FeeDataAdapter : ListAdapter<FeeData, FeeViewHolder>(diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeeViewHolder {
        return FeeViewHolder(inflate(parent, R.layout.view_holder_market_fee, false))
    }

    override fun onBindViewHolder(holder: FeeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val diff = object : DiffUtil.ItemCallback<FeeData>() {
            override fun areItemsTheSame(oldItem: FeeData, newItem: FeeData) = true
            override fun areContentsTheSame(oldItem: FeeData, newItem: FeeData) = false
        }
    }
}

class FeeData {

}

class FeeViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    fun bind(item: FeeData) {

    }

}
