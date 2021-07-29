package io.horizontalsystems.bankwallet.modules.coin.majorholders

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.coin.MajorHolderItem
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_major_holders_item.*

class CoinMajorHoldersAdapter(
    private val items: List<MajorHolderItem>,
    private val listener: Listener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onAddressClick(address: String)
        fun onDetailsClick(address: String)
    }

    private val viewTypeHeader = 0
    private val viewTypeItem = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            viewTypeItem -> ViewHolderItem(inflate(parent, R.layout.view_holder_coin_major_holders_item), listener)
            else -> ViewHolderSectionHeader(inflate(parent, R.layout.view_holder_coin_major_holders_section_header))
        }
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = when (items[position]) {
        is MajorHolderItem.Item -> viewTypeItem
        is MajorHolderItem.Header -> viewTypeHeader
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is ViewHolderItem -> holder.bind(item as MajorHolderItem.Item)
        }
    }

    class ViewHolderSectionHeader(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer

    class ViewHolderItem(
        override val containerView: View,
        private val listener: Listener
    ) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        private var item: MajorHolderItem.Item? = null

        init {
            btnHolderAddress.setOnSingleClickListener {
                item?.let {
                    listener.onAddressClick(it.address)
                }
            }
            btnDetails.setOnSingleClickListener {
                item?.let {
                    listener.onDetailsClick(it.address)
                }
            }
        }

        fun bind(item: MajorHolderItem.Item) {
            this.item = item

            txtIndex.text = item.index.toString()
            btnHolderAddress.text = item.address
            txtHolderRate.text = item.sharePercent
        }
    }
}
