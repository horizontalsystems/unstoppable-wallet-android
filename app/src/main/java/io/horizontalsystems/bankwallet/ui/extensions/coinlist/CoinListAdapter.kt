package io.horizontalsystems.bankwallet.ui.extensions.coinlist

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.core.setRemoteImage
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_manage_item.*

class CoinListAdapter(private val listener: Listener) :
    ListAdapter<CoinViewItem, CoinWithSwitchViewHolder>(diffCallback) {

    interface Listener {
        fun enable(fullCoin: FullCoin)
        fun disable(fullCoin: FullCoin)
        fun edit(fullCoin: FullCoin) = Unit
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinWithSwitchViewHolder {
        return CoinWithSwitchViewHolder(
            inflate(parent, R.layout.view_holder_coin_manage_item, false),
            { isChecked, index ->
                val item = getItem(index)
                onSwitchToggle(isChecked, item.fullCoin)
            },
            { index ->
                val item = getItem(index)
                listener.edit(item.fullCoin)
            }
        )
    }

    override fun onBindViewHolder(holder: CoinWithSwitchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private fun onSwitchToggle(isChecked: Boolean, fullCoin: FullCoin) {
        if (isChecked) {
            listener.enable(fullCoin)
        } else {
            listener.disable(fullCoin)
        }
    }

    fun disableCoin(coin: Coin): Boolean {
        for (i in 0 until itemCount) {
            if (getItem(i).fullCoin.coin == coin) {
                notifyItemChanged(i)
                return true
            }
        }
        return false
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<CoinViewItem>() {
            override fun areItemsTheSame(oldItem: CoinViewItem, newItem: CoinViewItem): Boolean {
                return oldItem.fullCoin.coin == newItem.fullCoin.coin
            }

            override fun areContentsTheSame(oldItem: CoinViewItem, newItem: CoinViewItem): Boolean {
                return oldItem.fullCoin.coin == newItem.fullCoin.coin && oldItem.state == newItem.state && oldItem.listPosition == newItem.listPosition
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
        set(viewItem.fullCoin, viewItem.listPosition)

        when (viewItem.state) {
            CoinViewItemState.ToggleHidden -> {
                toggleSwitch.setOnCheckedChangeListener { _, _ -> }
                toggleSwitch.isVisible = false
                edit.isVisible = false
            }
            is CoinViewItemState.ToggleVisible -> {
                // set switch value without triggering onChangeListener
                toggleSwitch.setOnCheckedChangeListener(null)
                toggleSwitch.isChecked = viewItem.state.enabled
                toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
                    onSwitch(isChecked, bindingAdapterPosition)
                }
                toggleSwitch.isVisible = true

                edit.isVisible = viewItem.state.hasSettings
                edit.setOnSingleClickListener {
                    onEdit(bindingAdapterPosition)
                }
            }
        }
    }

    private fun set(fullCoin: FullCoin, listPosition: ListPosition) {
        backgroundView.setBackgroundResource(getBackground(listPosition))
        coinIcon.setRemoteImage(fullCoin.coin.iconUrl, fullCoin.iconPlaceholder)
        coinTitle.text = fullCoin.coin.name
        coinSubtitle.text = fullCoin.coin.code
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

data class CoinViewItem(val fullCoin: FullCoin, val state: CoinViewItemState, val listPosition: ListPosition)

sealed class CoinViewItemState {
    data class ToggleVisible(val enabled: Boolean, val hasSettings: Boolean) : CoinViewItemState()
    object ToggleHidden : CoinViewItemState()
}