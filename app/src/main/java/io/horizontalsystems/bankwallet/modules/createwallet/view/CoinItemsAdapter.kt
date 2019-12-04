package io.horizontalsystems.bankwallet.modules.createwallet.view

import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.components.CellView
import io.horizontalsystems.bankwallet.modules.managecoins.CoinToggleViewItem
import io.horizontalsystems.bankwallet.modules.managecoins.CoinToggleViewItemState
import io.horizontalsystems.bankwallet.viewHelpers.inflate
import kotlinx.android.extensions.LayoutContainer

class CoinItemsAdapter(private val listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun enable(item: CoinToggleViewItem)
        fun disable(item: CoinToggleViewItem)
        fun onSelect(item: CoinToggleViewItem)
    }

    private val typeFeatured = 0
    private val typeAll = 1
    private val typeDivider = 2
    private val typeTopDescription = 3

    private val showDivider
        get() = featuredCoins.isNotEmpty()

    var featuredCoins = listOf<CoinToggleViewItem>()
    var coins = listOf<CoinToggleViewItem>()

    override fun getItemViewType(position: Int): Int = when {
        position == 0 -> typeTopDescription
        position < featuredCoins.size + 1 -> typeFeatured
        showDivider && position == featuredCoins.size + 1 -> typeDivider
        else -> typeAll
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            typeFeatured, typeAll -> {
                val cellView = CellView(parent.context)
                cellView.layoutParams = getCellViewLayoutParams()
                CoinViewHolder(cellView)
            }
            typeTopDescription -> ViewHolderTopDescription(inflate(parent, R.layout.view_holder_top_description, false))
            else -> ViewHolderDivider(inflate(parent, R.layout.view_holder_coin_manager_divider, false))
        }
    }

    private fun getCellViewLayoutParams() =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    override fun getItemCount(): Int {
        return featuredCoins.size + coins.size + (if (showDivider) 1 else 0) + 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CoinViewHolder -> {
                val item = getItemByPosition(position)
                holder.bind(
                        item,
                        isLastItemInGroup(position),
                        onClick = { index ->
                            listener.onSelect(getItemByPosition(index))
                        },
                        onSwitch = { isChecked, index ->
                            when {
                                isChecked -> listener.enable(getItemByPosition(index))
                                else -> listener.disable(getItemByPosition(index))
                            }
                        })
            }
        }
    }

    private fun isLastItemInGroup(position: Int): Boolean {
        return if (position == featuredCoins.size) {
            true
        } else {
            val dividerCount = if (showDivider) 1 else 0
            position == itemCount - dividerCount
        }
    }

    private fun getItemByPosition(position: Int): CoinToggleViewItem {
        return if (position < featuredCoins.size + 1) {
            featuredCoins[position - 1]
        } else {
            val index = when {
                showDivider -> position - featuredCoins.size - 2
                else -> position - 1
            }
            coins[index]
        }
    }
}

class CoinViewHolder(override val containerView: CellView) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(
            coinViewItem: CoinToggleViewItem,
            lastElement: Boolean,
            onClick: (position: Int) -> Unit,
            onSwitch: (isChecked: Boolean, position: Int) -> Unit
    ) {
        containerView.coinIcon = coinViewItem.coin.code
        containerView.title = coinViewItem.coin.code
        containerView.subtitle = coinViewItem.coin.title
        containerView.subtitleLabel = coinViewItem.coin.type.typeLabel()
        containerView.bottomBorder = lastElement

        when (val state = coinViewItem.state) {
            is CoinToggleViewItemState.ToggleVisible -> {
                containerView.switchIsChecked = state.enabled
                containerView.switchOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
                    onSwitch.invoke(isChecked, adapterPosition)
                }
            }
            is CoinToggleViewItemState.ToggleHidden -> {
                containerView.setOnClickListener {
                    onClick.invoke(adapterPosition)
                }
            }
        }
    }
}

class ViewHolderDivider(val containerView: View) : RecyclerView.ViewHolder(containerView)
class ViewHolderTopDescription(val containerView: View) : RecyclerView.ViewHolder(containerView)
