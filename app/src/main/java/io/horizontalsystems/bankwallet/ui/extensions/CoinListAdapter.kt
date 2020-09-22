package io.horizontalsystems.bankwallet.ui.extensions

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_manage_item.*

class CoinListAdapter(private val listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun enable(coin: Coin)
        fun disable(coin: Coin)
        fun select(coin: Coin)
    }

    private val coinWithSwitch = 0
    private val coinWithoutSwitch = 1
    private val dividerView = 2
    var viewItems = listOf<CoinViewItem>()

    override fun getItemCount(): Int {
        return viewItems.size
    }

    override fun getItemViewType(position: Int): Int =
            when (viewItems[position]) {
                CoinViewItem.Divider -> dividerView
                is CoinViewItem.ToggleHidden -> coinWithoutSwitch
                is CoinViewItem.ToggleVisible -> coinWithSwitch
            }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            coinWithoutSwitch -> CoinWithPlusViewHolder(
                    inflate(parent, R.layout.view_holder_coin_manage_item, false),
                    onClick = { index ->
                        (viewItems[index] as? CoinViewItem.ToggleHidden)?.coin?.let {
                            listener.select(it)
                        }
                    })
            coinWithSwitch -> CoinWithSwitchViewHolder(
                    inflate(parent, R.layout.view_holder_coin_manage_item, false),
                    onSwitch = { isChecked, index ->
                        (viewItems[index] as? CoinViewItem.ToggleVisible)?.coin?.let {
                            onSwitchToggle(isChecked, it)
                        }
                    })
            dividerView -> ViewHolderDivider(inflate(parent, R.layout.view_holder_coin_manager_divider, false))
            else -> throw Exception("No such view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder){
            is CoinWithSwitchViewHolder -> {
                (viewItems[position] as? CoinViewItem.ToggleVisible)?.let {
                    holder.bind(it.coin, it.enabled, it.last)
                }
            }
            is CoinWithPlusViewHolder -> {
                (viewItems[position] as? CoinViewItem.ToggleHidden)?.let {
                    holder.bind(it.coin, it.last)
                }
            }
        }
    }

    private fun onSwitchToggle(isChecked: Boolean, coin: Coin) {
        if (isChecked) {
            listener.enable(coin)
        } else {
            listener.disable(coin)
        }
    }

}

class CoinWithSwitchViewHolder(
        override val containerView: View,
        private val onSwitch: (isChecked: Boolean, position: Int) -> Unit
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        containerView.setOnClickListener {
            toggleSwitch.isChecked = !toggleSwitch.isChecked
        }
    }

    fun bind(coin: Coin, enabled: Boolean, last: Boolean) {
        coinIcon.setCoinImage(coin.code, coin.type)
        coinTitle.text = coin.title
        coinSubtitle.text = coin.code
        bottomShade.isVisible = last

        coinTypeLabel.text = coin.type.typeLabel()
        coinTypeLabel.isVisible = coin.type.typeLabel() != null

        // Disable listener when setting default values
        toggleSwitch.setOnCheckedChangeListener(null)

        toggleSwitch.isChecked = enabled
        toggleSwitch.isVisible = true

        // Enable listener
        toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            onSwitch.invoke(isChecked, bindingAdapterPosition)
        }
    }
}

class CoinWithPlusViewHolder(
        override val containerView: View,
        private val onClick: (position: Int) -> Unit
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        containerView.setOnClickListener {
            onClick.invoke(bindingAdapterPosition)
        }
    }

    fun bind(coin: Coin, last: Boolean) {
        rightArrow.isVisible = true
        coinIcon.setCoinImage(coin.code, coin.type)
        coinTitle.text = coin.title
        coinSubtitle.text = coin.code
        coinTypeLabel.text = coin.type.typeLabel()
        coinTypeLabel.isVisible = coin.type.typeLabel() != null
        bottomShade.isVisible = last
    }
}

class ViewHolderDivider(val containerView: View) : RecyclerView.ViewHolder(containerView)

sealed class CoinViewItem {
    object Divider : CoinViewItem()
    class ToggleHidden(val coin: Coin, val last: Boolean) : CoinViewItem()
    class ToggleVisible(val coin: Coin, val enabled: Boolean, val last: Boolean) : CoinViewItem()
}