package io.horizontalsystems.bankwallet.modules.coin.majorholders

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.MajorHolderItem
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_major_holders_item.*

class CoinMajorHoldersAdapter(
    private val items: List<MajorHolderItem>,
    private val listener: Listener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onItemClick(address: String)
    }

    private val viewTypeHeader = 0
    private val viewTypeItem = 1
    private val viewTypeDescription = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            viewTypeItem -> ViewHolderItem(inflate(parent, R.layout.view_holder_coin_major_holders_item))
            viewTypeDescription -> ViewHolderDescription(inflate(parent, R.layout.view_holder_coin_major_holders_description))
            else -> ViewHolderSectionHeader(inflate(parent, R.layout.view_holder_coin_major_holders_section_header))
        }
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = when (items[position]) {
        is MajorHolderItem.Item -> viewTypeItem
        is MajorHolderItem.Header -> viewTypeHeader
        is MajorHolderItem.Description -> viewTypeDescription
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is ViewHolderItem -> holder.bind(item as MajorHolderItem.Item) {
                listener.onItemClick(item.address)
            }
        }
    }

    class ViewHolderSectionHeader(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer

    class ViewHolderDescription(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer

    class ViewHolderItem(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(item: MajorHolderItem.Item, onClick: () -> Unit) {
            txtHolderAddress.text = item.address
            txtHolderRate.text = item.sharePercent
            viewBackground.setBackgroundResource(item.position.getBackground())
            containerView.setOnClickListener {
                onClick.invoke()
            }
        }
    }

}
