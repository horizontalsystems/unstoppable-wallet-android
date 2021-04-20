package io.horizontalsystems.bankwallet.ui.extensions.coinlist

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setCoinImage
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.entities.label
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_manage_item.*

class CoinListAdapter(private val listener: Listener) : ListAdapter<CoinViewItem, CoinWithSwitchViewHolder>(diffCallback) {

    interface Listener {
        fun enable(coin: Coin)
        fun disable(coin: Coin)
        fun edit(coin: Coin) = Unit
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinWithSwitchViewHolder {
        return CoinWithSwitchViewHolder(
                inflate(parent, R.layout.view_holder_coin_manage_item, false),
                { isChecked, index ->
                    val item = getItem(index)
                    onSwitchToggle(isChecked, item.coin)
                    //update state in adapter item list: coins
                    item.enabled = isChecked
                },
                { index ->
                    val item = getItem(index)
                    listener.edit(item.coin)
                }
        )
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

    fun disableCoin(coin: Coin): Boolean {
        for (i in 0 until itemCount) {
            if (getItem(i).coin == coin) {
                getItem(i).enabled = false
                notifyItemChanged(i)
                return true
            }
        }
        return false
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<CoinViewItem>() {
            override fun areItemsTheSame(oldItem: CoinViewItem, newItem: CoinViewItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: CoinViewItem, newItem: CoinViewItem): Boolean {
                return oldItem == newItem
            }
        }
    }

}

class CoinWithSwitchViewHolder(
        override val containerView: View,
        private val onSwitch: (isChecked: Boolean, position: Int) -> Unit,
        private val onEdit: (position: Int) -> Unit
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        containerView.setOnClickListener {
            toggleSwitch.isChecked = !toggleSwitch.isChecked
        }
    }

    fun bind(viewItem: CoinViewItem) {
        set(viewItem.coin, viewItem.listPosition)

        // set switch value without triggering onChangeListener
        toggleSwitch.setOnCheckedChangeListener(null)
        toggleSwitch.isChecked = viewItem.enabled
        toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            onSwitch(isChecked, bindingAdapterPosition)
        }
        edit.isVisible = viewItem.hasSettings
        edit.setOnSingleClickListener {
            onEdit(bindingAdapterPosition)
        }
    }

    private fun set(coin: Coin, listPosition: ListPosition) {
        backgroundView.setBackgroundResource(getBackground(listPosition))
        coinIcon.setCoinImage(coin.type)
        coinTitle.text = coin.title
        coinSubtitle.text = coin.code
        coinTypeLabel.text = coin.type.label
        coinTypeLabel.isVisible = coin.type.label != null
        dividerView.isVisible = listPosition == ListPosition.Last || listPosition == ListPosition.Single
    }

    private fun getBackground(listPosition: ListPosition): Int {
        return when (listPosition) {
            ListPosition.First -> R.drawable.border_steel10_top
            ListPosition.Middle -> R.drawable.border_steel10_top
            ListPosition.Last -> R.drawable.border_steel10_top_bottom
            ListPosition.Single -> R.drawable.border_steel10_top_bottom
        }
    }

}

data class CoinViewItem(val coin: Coin, val hasSettings: Boolean, var enabled: Boolean, val listPosition: ListPosition)

data class CoinViewState(val featuredViewItems: List<CoinViewItem>, val viewItems: List<CoinViewItem>)
