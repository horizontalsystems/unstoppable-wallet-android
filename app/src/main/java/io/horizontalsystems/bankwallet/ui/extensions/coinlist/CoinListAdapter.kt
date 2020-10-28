package io.horizontalsystems.bankwallet.ui.extensions.coinlist

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.ui.extensions.setCoinImage
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_manage_item.*

class CoinListAdapter(private val listener: Listener) : ListAdapter<CoinViewItem, CoinWithSwitchViewHolder>(diffCallback) {

    interface Listener {
        fun enable(coin: Coin)
        fun disable(coin: Coin)
        fun select(coin: Coin)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinWithSwitchViewHolder {
        return CoinWithSwitchViewHolder(
                inflate(parent, R.layout.view_holder_coin_manage_item, false),
                onSwitch = { isChecked, index ->
                    (getItem(index) as? CoinViewItem.ToggleVisible)?.let {
                        onSwitchToggle(isChecked, it.coin)
                        //update state in adapter item list: coins
                        it.enabled = isChecked
                    }
                },
                onClick = { index ->
                    (getItem(index) as? CoinViewItem.ToggleHidden)?.coin?.let {
                        listener.select(it)
                    }
                })
    }

    override fun onBindViewHolder(holder: CoinWithSwitchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private fun onSwitchToggle(isChecked: Boolean, coin: Coin) {
        if (isChecked) {
            listener.enable(coin)
        } else {
            listener.disable(coin)
        }
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<CoinViewItem>() {
            override fun areItemsTheSame(oldItem: CoinViewItem, newItem: CoinViewItem): Boolean {
                if ((oldItem as? CoinViewItem.ToggleHidden)?.coin?.coinId == (newItem as? CoinViewItem.ToggleHidden)?.coin?.coinId
                        || (oldItem as? CoinViewItem.ToggleVisible)?.coin?.coinId == (newItem as? CoinViewItem.ToggleVisible)?.coin?.coinId) {
                    return true
                }
                return false
            }

            override fun areContentsTheSame(oldItem: CoinViewItem, newItem: CoinViewItem): Boolean {
                if (oldItem is CoinViewItem.ToggleHidden && newItem is CoinViewItem.ToggleHidden) {
                    return oldItem.coin.coinId == newItem.coin.coinId && oldItem.last == newItem.last
                } else if (oldItem is CoinViewItem.ToggleVisible && newItem is CoinViewItem.ToggleVisible) {
                    return oldItem.coin.coinId == newItem.coin.coinId && oldItem.enabled == newItem.enabled && oldItem.last == newItem.last
                } else return false
            }
        }
    }

}

class CoinWithSwitchViewHolder(
        override val containerView: View,
        private val onSwitch: (isChecked: Boolean, position: Int) -> Unit,
        private val onClick: (position: Int) -> Unit
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        containerView.setOnClickListener {
            if (toggleSwitch.isVisible) {
                toggleSwitch.isChecked = !toggleSwitch.isChecked
            } else {
                onClick.invoke(bindingAdapterPosition)
            }
        }
    }

    fun bind(viewItem: CoinViewItem) {
        when (viewItem) {
            is CoinViewItem.ToggleHidden -> {
                set(viewItem.coin, viewItem.last)

                rightArrow.isVisible = true
                toggleSwitch.isVisible = false
            }
            is CoinViewItem.ToggleVisible -> {
                set(viewItem.coin, viewItem.last)

                rightArrow.isVisible = false
                toggleSwitch.isVisible = true

                // set switch value without triggering onChangeListener
                toggleSwitch.setOnCheckedChangeListener(null)
                toggleSwitch.isChecked = viewItem.enabled
                toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
                    onSwitch.invoke(isChecked, bindingAdapterPosition)
                }
            }
        }

    }

    private fun set(coin: Coin, last: Boolean) {
        coinIcon.setCoinImage(coin.code, coin.type)
        coinTitle.text = coin.title
        coinSubtitle.text = coin.code
        coinTypeLabel.text = coin.type.label
        coinTypeLabel.isVisible = coin.type.label != null
        bottomShade.isVisible = last
        dividerView.isVisible = last
    }
}

sealed class CoinViewItem {
    class ToggleHidden(val coin: Coin, val last: Boolean) : CoinViewItem()
    class ToggleVisible(val coin: Coin, var enabled: Boolean, val last: Boolean) : CoinViewItem()
}

data class CoinViewState(val featuredViewItems: List<CoinViewItem>, val viewItems: List<CoinViewItem>)
