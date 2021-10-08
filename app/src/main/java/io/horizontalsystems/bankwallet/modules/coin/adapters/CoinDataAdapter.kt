package io.horizontalsystems.bankwallet.modules.coin.adapters

import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.CoinDataItem
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_market_info.*
import kotlinx.android.synthetic.main.view_holder_coin_page_section_header.*

class CoinDataAdapter(
        rateDiffsLiveData: MutableLiveData<List<CoinDataItem>>,
        viewLifecycleOwner: LifecycleOwner,
        @StringRes private val sectionHeader: Int? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        rateDiffsLiveData.observe(viewLifecycleOwner) { list ->
            if (list.isEmpty()){
                return@observe
            }

            val insert = items.isEmpty()
            items = list
            if (insert) {
                notifyItemRangeInserted(0, items.size + 1)
            } else {
                notifyItemRangeChanged(0, items.size + 1)
            }
        }
    }

    private var items = listOf<CoinDataItem>()
    private val viewTypeItem = 0
    private val viewTypeHeader = 1

    override fun getItemCount(): Int {
        return if (items.isNotEmpty()) items.size + 1 else 0
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> viewTypeHeader
            else -> viewTypeItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            viewTypeItem -> ViewHolder(inflate(parent, R.layout.view_holder_coin_market_info, false))
            viewTypeHeader -> ViewHolderSectionHeader(inflate(parent, R.layout.view_holder_coin_page_section_header, false))
            else -> throw  IllegalArgumentException("No such viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> holder.bind(items[position - 1])
            is ViewHolderSectionHeader -> holder.bind(sectionHeader)
        }
    }

    class ViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(item: CoinDataItem) {
            coinMarketInfoLine.bindItem(item)
        }
    }

    class ViewHolderSectionHeader(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(title: Int?) {
            border.isVisible = title != null
            sectionTitle.isVisible = title != null
            title?.let { sectionTitle.setText(it) }
        }
    }
}
