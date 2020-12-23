package io.horizontalsystems.bankwallet.modules.market.fee

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_market_fee.*
import java.util.*

class MarketFeeDataAdapter : ListAdapter<Optional<FeeData>, FeeViewHolder>(diff) {

    init {
        submitList(listOf(Optional.empty()))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeeViewHolder {
        return FeeViewHolder(inflate(parent, R.layout.view_holder_market_fee, false))
    }

    override fun onBindViewHolder(holder: FeeViewHolder, position: Int) {
        val item = getItem(position)

        holder.bind(if (item.isPresent) item.get() else null)
    }

    companion object {
        private val diff = object : DiffUtil.ItemCallback<Optional<FeeData>>() {
            override fun areItemsTheSame(oldItem: Optional<FeeData>, newItem: Optional<FeeData>) = true
            override fun areContentsTheSame(oldItem: Optional<FeeData>, newItem: Optional<FeeData>) = false
        }
    }
}

class FeeData {

}

class FeeViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: FeeData?) {
        text.text = item?.let {
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
        }

        text.isSelected = true
    }

}
