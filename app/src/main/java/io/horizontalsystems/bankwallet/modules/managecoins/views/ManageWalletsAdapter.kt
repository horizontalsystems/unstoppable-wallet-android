package io.horizontalsystems.bankwallet.modules.managecoins.views

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.managecoins.CoinToggleViewItem
import io.horizontalsystems.bankwallet.modules.managecoins.CoinToggleViewItemState
import io.horizontalsystems.bankwallet.viewHelpers.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_manager.*

class ManageWalletsAdapter(private val listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun enable(item: CoinToggleViewItem)
        fun disable(item: CoinToggleViewItem)
        fun select(item: CoinToggleViewItem)
    }

    var featuredViewItems = listOf<CoinToggleViewItem>()
    var coinsViewItems = listOf<CoinToggleViewItem>()

    private val typePopular = 0
    private val typeAll = 1
    private val typeDivider = 2
    private val showDivider
        get() = featuredViewItems.isNotEmpty()

    override fun getItemCount() = featuredViewItems.size + coinsViewItems.size + (if (showDivider) 1 else 0)

    override fun getItemViewType(position: Int): Int = when {
        position < featuredViewItems.size -> typePopular
        showDivider && position == featuredViewItems.size -> typeDivider
        else -> typeAll
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            typePopular, typeAll -> ViewHolderCoin(inflate(parent, R.layout.view_holder_coin_manager, false))
            else -> ViewHolderDivider(inflate(parent, R.layout.view_holder_coin_manager_divider, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder) {
            is ViewHolderCoin -> {
                when (getItemViewType(position)) {
                    typePopular -> {
                        bindView(holder, featuredViewItems[position], position == featuredViewItems.size - 1)
                    }
                    typeAll -> {
                        val index = getIndex(position)
                        bindView(holder, coinsViewItems[index], position == itemCount - 1)
                    }
                }
            }
        }
    }

    private fun bindView(holder: ViewHolderCoin, viewItem: CoinToggleViewItem, showBottomShadow: Boolean) {
        holder.bind(
                item = viewItem,
                showBottomShadow = showBottomShadow,
                onSwitchToggle = { isChecked ->
                    when {
                        isChecked -> listener.enable(viewItem)
                        else -> listener.disable(viewItem)
                    }
                },
                onClick = {
                    listener.select(viewItem)
                })
    }

    private fun getIndex(position: Int): Int {
        return when {
            showDivider -> position - featuredViewItems.size - 1
            else -> position
        }
    }

    private fun LayoutContainer.bind(
            item: CoinToggleViewItem,
            showBottomShadow: Boolean,
            onSwitchToggle: (isChecked: Boolean) -> Unit,
            onClick: () -> Unit
    ) {

        val coin = item.coin

        coinTitle.text = coin.title
        coinCode.text = coin.code
        coinTypeLabel.text = coin.type.typeLabel()
        coinTypeLabel.visibility = if (coin.type.typeLabel() != null) View.VISIBLE else View.GONE
        coinIcon.bind(coin)
        bottomShade.visibility = if (showBottomShadow) View.VISIBLE else View.GONE

        when (item.state) {
            is CoinToggleViewItemState.ToggleHidden -> {
                toggleSwitch.visibility = View.GONE
                rightArrow.visibility = View.VISIBLE

                cellView.setOnClickListener { onClick.invoke() }
            }
            is CoinToggleViewItemState.ToggleVisible -> {
                toggleSwitch.visibility = View.VISIBLE
                rightArrow.visibility = View.GONE

                toggleSwitch.setOnCheckedChangeListener(null)
                toggleSwitch.isChecked = item.state.enabled
                toggleSwitch.setOnCheckedChangeListener { _, isChecked -> onSwitchToggle.invoke(isChecked) }
            }
        }

    }

    class ViewHolderCoin(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
    class ViewHolderDivider(val containerView: View) : RecyclerView.ViewHolder(containerView)
}
