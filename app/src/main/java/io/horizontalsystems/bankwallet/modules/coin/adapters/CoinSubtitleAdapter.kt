package io.horizontalsystems.bankwallet.modules.coin.adapters

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_subtitle.*
import java.math.BigDecimal

class CoinSubtitleAdapter(
        viewItemLiveData: MutableLiveData<ViewItemWrapper>,
        viewLifecycleOwner: LifecycleOwner
) : ListAdapter<CoinSubtitleAdapter.ViewItemWrapper, CoinSubtitleAdapter.ViewHolder>(diff) {

    init {
        viewItemLiveData.observe(viewLifecycleOwner) {
            submitList(listOf(it))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(inflate(parent, R.layout.view_holder_coin_subtitle, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {}

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        val item = getItem(position)
        val prev = payloads.lastOrNull() as? ViewItemWrapper

        if (prev == null) {
            holder.bind(item)
        } else {
            holder.bindUpdate(item, prev)
        }
    }

    companion object {
        private val diff = object : DiffUtil.ItemCallback<ViewItemWrapper>() {
            override fun areItemsTheSame(oldItem: ViewItemWrapper, newItem: ViewItemWrapper): Boolean = true

            override fun areContentsTheSame(oldItem: ViewItemWrapper, newItem: ViewItemWrapper): Boolean {
                return oldItem.rate == newItem.rate && oldItem.rateDiff == newItem.rateDiff
            }

            override fun getChangePayload(oldItem: ViewItemWrapper, newItem: ViewItemWrapper): Any? {
                return oldItem
            }
        }
    }

    data class ViewItemWrapper(
        val rate: String?,
        val rateDiff: BigDecimal?
    )

    class ViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(item: ViewItemWrapper) {
            coinRateLast.text = item.rate
            coinRateDiff.setDiff(item.rateDiff)
        }

        fun bindUpdate(current: ViewItemWrapper, prev: ViewItemWrapper) {
            current.apply {
                if (rate != prev.rate) {
                    coinRateLast.text = rate
                }
                if (rateDiff != prev.rateDiff) {
                    coinRateDiff.setDiff(rateDiff)
                }
            }
        }

    }
}
