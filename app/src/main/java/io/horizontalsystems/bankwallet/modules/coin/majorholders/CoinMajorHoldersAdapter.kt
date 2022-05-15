package io.horizontalsystems.bankwallet.modules.coin.majorholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.databinding.ViewHolderCoinMajorHoldersItemBinding
import io.horizontalsystems.bankwallet.databinding.ViewHolderCoinMajorHoldersSectionHeaderBinding
import io.horizontalsystems.bankwallet.modules.coin.MajorHolderItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.Ellipsis

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
            viewTypeItem -> {
                ViewHolderItem(
                    ViewHolderCoinMajorHoldersItemBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    ), listener)
            }
            else -> {
                ViewHolderSectionHeader(
                    ViewHolderCoinMajorHoldersSectionHeaderBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                )
            }
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

    class ViewHolderSectionHeader(binding: ViewHolderCoinMajorHoldersSectionHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    class ViewHolderItem(
        private val binding: ViewHolderCoinMajorHoldersItemBinding,
        private val listener: Listener
    ) : RecyclerView.ViewHolder(binding.root) {

        private var item: MajorHolderItem.Item? = null

        fun bind(item: MajorHolderItem.Item) {
            this.item = item

            binding.txtIndex.text = item.index.toString()
            binding.txtHolderRate.text = item.sharePercent

            binding.buttonsCompose.setContent {
                ComposeAppTheme {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
