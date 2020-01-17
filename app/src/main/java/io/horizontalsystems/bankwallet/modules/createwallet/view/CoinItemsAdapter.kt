package io.horizontalsystems.bankwallet.modules.createwallet.view

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.viewHelpers.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_manage_item.*

class CoinItemsAdapter(private val listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun enable(coin: Coin)
        fun disable(coin: Coin)
        fun select(coin: Coin)
    }

    private val coinWithSwitch = 0
    private val coinWithArrow = 1
    private val dividerView = 2
    var viewItems = listOf<CoinManageViewItem>()

    override fun getItemCount(): Int {
        return viewItems.size
    }

    override fun getItemViewType(position: Int): Int =
            when (viewItems[position].type) {
                is CoinManageViewType.CoinWithArrow -> coinWithArrow
                is CoinManageViewType.CoinWithSwitch -> coinWithSwitch
                is CoinManageViewType.Divider -> dividerView
            }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            coinWithArrow -> CoinItemWithArrowViewHolder(
                    inflate(parent, R.layout.view_holder_coin_manage_item, false),
                    onClick = { index ->
                        viewItems[index].coinViewItem?.coin?.let {
                            listener.select(it)
                        }
                    })
            coinWithSwitch -> CoinItemWithSwitchViewHolder(
                    inflate(parent, R.layout.view_holder_coin_manage_item, false),
                    onSwitch = { isChecked, index ->
                        onSwitchToggle(isChecked, index)
                    })
            dividerView -> ViewHolderDivider(inflate(parent, R.layout.view_holder_coin_manager_divider, false))
            else -> throw Exception("No such view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CoinItemWithArrowViewHolder -> holder.bind(viewItems[position])
            is CoinItemWithSwitchViewHolder -> holder.bind(viewItems[position])
        }
    }

    private fun onSwitchToggle(isChecked: Boolean, index: Int) {
        viewItems[index].let { viewItem ->
            viewItem.coinViewItem?.coin?.let {
                when {
                    isChecked -> listener.enable(it)
                    else -> listener.disable(it)
                }
            }

            //update state in adapter item list: coins
            (viewItem.type as? CoinManageViewType.CoinWithSwitch)?.enabled = isChecked
        }
    }

}

class CoinItemWithSwitchViewHolder(
        override val containerView: View,
        private val onSwitch: (isChecked: Boolean, position: Int) -> Unit
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        containerView.setOnClickListener {
            toggleSwitch.isChecked = !toggleSwitch.isChecked
            onSwitch.invoke(toggleSwitch.isChecked, adapterPosition)
        }
    }

    fun bind(item: CoinManageViewItem) {
        val viewItem = item.coinViewItem ?: return
        val coin = viewItem.coin

        coinIcon.bind(coin)
        coinTitle.text = coin.title
        coinSubtitle.text = coin.code
        bottomShade.visibility = if (viewItem.showBottomShade) View.VISIBLE else View.GONE

        coinTypeLabel.text = coin.type.typeLabel()
        coinTypeLabel.visibility = if (coin.type.typeLabel() != null) View.VISIBLE else View.GONE

        val checked = (item.type as? CoinManageViewType.CoinWithSwitch)?.enabled ?: false
        toggleSwitch.isChecked = checked
        toggleSwitch.visibility = View.VISIBLE
    }
}

class CoinItemWithArrowViewHolder(
        override val containerView: View,
        private val onClick: (position: Int) -> Unit
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        containerView.setOnClickListener {
            onClick.invoke(adapterPosition)
        }
    }

    fun bind(item: CoinManageViewItem) {
        val viewItem = item.coinViewItem ?: return
        val coin = viewItem.coin

        rightArrow.visibility = View.VISIBLE
        coinIcon.bind(coin)
        coinTitle.text = coin.title
        coinSubtitle.text = coin.code
        coinTypeLabel.text = coin.type.typeLabel()
        coinTypeLabel.visibility = if (coin.type.typeLabel() != null) View.VISIBLE else View.GONE
        bottomShade.visibility = if (viewItem.showBottomShade) View.VISIBLE else View.GONE
    }
}

class ViewHolderDivider(val containerView: View) : RecyclerView.ViewHolder(containerView)

data class CoinManageViewItem(val type: CoinManageViewType, val coinViewItem: CoinViewItem? = null)
data class CoinViewItem(val coin: Coin, var showBottomShade: Boolean = false)

sealed class CoinManageViewType{
    object Divider: CoinManageViewType()
    object CoinWithArrow: CoinManageViewType()
    class CoinWithSwitch(var enabled: Boolean): CoinManageViewType()
}
