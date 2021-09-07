package io.horizontalsystems.bankwallet.modules.coin.majorholders

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.MajorHolderItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.Ellipsis
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
            viewTypeItem -> ViewHolderItem(
                inflate(
                    parent,
                    R.layout.view_holder_coin_major_holders_item
                ), listener
            )
            else -> ViewHolderSectionHeader(
                inflate(
                    parent,
                    R.layout.view_holder_coin_major_holders_section_header
                )
            )
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

    class ViewHolderSectionHeader(override val containerView: View) :
        RecyclerView.ViewHolder(containerView), LayoutContainer

    class ViewHolderItem(
        override val containerView: View,
        private val listener: Listener
    ) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        private var item: MajorHolderItem.Item? = null

        fun bind(item: MajorHolderItem.Item) {
            this.item = item

            txtIndex.text = item.index.toString()
            txtHolderRate.text = item.sharePercent

            buttonsCompose.setContent {
                ComposeAppTheme {
                    Row() {
                        ButtonSecondaryDefault(
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                            title = item.address,
                            onClick = {
                                listener.onAddressClick(item.address)
                            },
                            ellipsis = Ellipsis.Middle(8)
                        )
                        ButtonSecondaryCircle(
                            modifier = Modifier.padding(end = 16.dp),
                            icon = R.drawable.ic_globe_20,
                            onClick = {
                                listener.onDetailsClick(item.address)
                            }
                        )
                    }
                }
            }
        }
    }
}
