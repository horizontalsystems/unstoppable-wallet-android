package io.horizontalsystems.bankwallet.modules.coin.adapters

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.RoiViewItem
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_roi.*

class CoinRoiAdapter(
        rateDiffsLiveData: MutableLiveData<List<RoiViewItem>>,
        viewLifecycleOwner: LifecycleOwner
) : ListAdapter<RoiViewItem, CoinRoiAdapter.ViewHolder>(diff) {

    init {
        rateDiffsLiveData.observe(viewLifecycleOwner) {
            submitList(it)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(inflate(parent, R.layout.view_holder_coin_roi, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val diff = object : DiffUtil.ItemCallback<RoiViewItem>() {
            override fun areItemsTheSame(oldItem: RoiViewItem, newItem: RoiViewItem): Boolean = true

            override fun areContentsTheSame(oldItem: RoiViewItem, newItem: RoiViewItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    class ViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(roi: RoiViewItem) {
            coinRoiLine.bind(roi)
        }
    }
}
