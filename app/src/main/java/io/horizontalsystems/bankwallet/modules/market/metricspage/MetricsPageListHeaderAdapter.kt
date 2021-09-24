package io.horizontalsystems.bankwallet.modules.market.metricspage

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_market_metrics_list_header.*

class MetricsPageListHeaderAdapter(
    viewItemLiveData: MutableLiveData<ViewItemWrapper>,
    viewLifecycleOwner: LifecycleOwner,
    private val listener: Listener
) : ListAdapter<MetricsPageListHeaderAdapter.ViewItemWrapper,
        MetricsPageListHeaderAdapter.ViewHolder>(diff) {

    interface Listener {
        fun onSortingClick()
        fun onToggleButtonClick()
    }

    init {
        viewItemLiveData.observe(viewLifecycleOwner) {
            submitList(listOf(it))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            inflate(parent, R.layout.view_holder_market_metrics_list_header, false),
            listener
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {}

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        val item = getItem(position)
        holder.bind(item)
    }

    companion object {
        private val diff = object : DiffUtil.ItemCallback<ViewItemWrapper>() {
            override fun areItemsTheSame(
                oldItem: ViewItemWrapper,
                newItem: ViewItemWrapper
            ): Boolean = true

            override fun areContentsTheSame(
                oldItem: ViewItemWrapper,
                newItem: ViewItemWrapper
            ): Boolean {
                return oldItem.sortMenu == newItem.sortMenu && oldItem.toggleButton == newItem.toggleButton
            }

            override fun getChangePayload(
                oldItem: ViewItemWrapper,
                newItem: ViewItemWrapper
            ): Any {
                return oldItem
            }
        }
    }

    data class ViewItemWrapper(
        val sortMenu: MarketListHeaderView.SortMenu,
        val toggleButton: MarketListHeaderView.ToggleButton
    )

    class ViewHolder(
        override val containerView: View,
        private val listener: Listener
    ) : RecyclerView.ViewHolder(containerView), LayoutContainer, MarketListHeaderView.Listener {
        init {
            listHeader.listener = this
        }

        fun bind(item: ViewItemWrapper) {
            listHeader.setMenu(item.sortMenu, item.toggleButton)
        }

        override fun onSortingClick() {
            listener.onSortingClick()
        }

        override fun onToggleButtonClick() {
            listener.onToggleButtonClick()
        }
    }
}
