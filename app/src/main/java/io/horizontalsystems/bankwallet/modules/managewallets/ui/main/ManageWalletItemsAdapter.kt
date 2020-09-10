package io.horizontalsystems.bankwallet.modules.managewallets.ui.main

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.managewallets.ManageCoinViewItem
import io.horizontalsystems.bankwallet.ui.extensions.setCoinImage
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_manage_item.*

class ManageWalletItemsAdapter(private val listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun enable(coin: Coin)
        fun disable(coin: Coin)
        fun select(coin: Coin)
    }

    private val coinWithSwitch = 0
    private val coinWithoutSwitch = 1
    private val dividerView = 2
    var viewItems = listOf<ManageCoinViewItem>()

    override fun getItemCount(): Int {
        return viewItems.size
    }

    override fun getItemViewType(position: Int): Int =
            when (viewItems[position]) {
                ManageCoinViewItem.Divider -> dividerView
                is ManageCoinViewItem.ToggleHidden -> coinWithoutSwitch
                is ManageCoinViewItem.ToggleVisible -> coinWithSwitch
            }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            coinWithoutSwitch -> ManageCoinWithPlusViewHolder(
                    inflate(parent, R.layout.view_holder_coin_manage_item, false),
                    onClick = { index ->
                        (viewItems[index] as? ManageCoinViewItem.ToggleHidden)?.coin?.let {
                            listener.select(it)
                        }
                    })
            coinWithSwitch -> ManageCoinWithSwitchViewHolder(
                    inflate(parent, R.layout.view_holder_coin_manage_item, false),
                    onSwitch = { isChecked, index ->
                        (viewItems[index] as? ManageCoinViewItem.ToggleVisible)?.coin?.let {
                            onSwitchToggle(isChecked, it)
                        }
                    })
            dividerView -> ViewHolderDivider(inflate(parent, R.layout.view_holder_coin_manager_divider, false))
            else -> throw Exception("No such view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder){
            is ManageCoinWithSwitchViewHolder -> {
                (viewItems[position] as? ManageCoinViewItem.ToggleVisible)?.let {
                    holder.bind(it.coin, it.enabled, it.last)
                }
            }
            is ManageCoinWithPlusViewHolder -> {
                (viewItems[position] as? ManageCoinViewItem.ToggleHidden)?.let {
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

class ManageCoinWithSwitchViewHolder(
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

class ManageCoinWithPlusViewHolder(
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
