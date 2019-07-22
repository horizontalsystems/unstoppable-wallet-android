package io.horizontalsystems.bankwallet.modules.managecoins.views

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.managecoins.ManageWalletViewItem
import io.horizontalsystems.bankwallet.modules.managecoins.ManageWalletsModule
import io.horizontalsystems.bankwallet.viewHelpers.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_manager.*

class ManageWalletsAdapter(private val viewDelegate: ManageWalletsModule.IViewDelegate) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val typePopular = 0
    private val typeAll = 1
    private val typeDivider = 2
    private val showDivider
        get() = viewDelegate.popularItemsCount > 0

    override fun getItemCount() = viewDelegate.popularItemsCount + viewDelegate.itemsCount + (if (showDivider) 1 else 0)

    override fun getItemViewType(position: Int): Int = when {
        position < viewDelegate.popularItemsCount -> typePopular
        showDivider && position == viewDelegate.popularItemsCount -> typeDivider
        else -> typeAll
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            typePopular -> ViewHolderPopularCoin(inflate(parent, R.layout.view_holder_coin_manager, false))
            typeAll -> ViewHolderCoin(inflate(parent, R.layout.view_holder_coin_manager, false))
            else -> ViewHolderDivider(inflate(parent, R.layout.view_holder_coin_manager_divider, false))
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderPopularCoin -> {
                holder.bind(item = viewDelegate.popularItem(position), showBottomShadow = (position == viewDelegate.popularItemsCount - 1)) { isChecked ->
                    if (isChecked) {
                        viewDelegate.enablePopularCoin(position)
                    } else {
                        viewDelegate.disablePopularCoin(position)
                    }
                }
            }
            is ViewHolderCoin -> {
                val index = when {
                    showDivider -> position - viewDelegate.popularItemsCount - 1
                    else -> position
                }

                holder.bind(item = viewDelegate.item(index), showBottomShadow = (position == itemCount - 1)) { isChecked ->
                    if (isChecked) {
                        viewDelegate.enableCoin(index)
                    } else {
                        viewDelegate.disableCoin(index)
                    }
                }
            }
        }
    }

    private fun LayoutContainer.bind(item: ManageWalletViewItem, showBottomShadow: Boolean, onClick: (isChecked: Boolean) -> Unit) {
        val coin = item.coin

        coinTitle.text = coin.title
        coinCode.text = coin.code
        coinIcon.bind(coin)
        bottomShade.visibility = if (showBottomShadow) View.VISIBLE else View.GONE

        toggleSwitch.setOnCheckedChangeListener(null)
        toggleSwitch.isChecked = item.enabled
        toggleSwitch.setOnCheckedChangeListener { _, isChecked -> onClick.invoke(isChecked) }
    }

    class ViewHolderPopularCoin(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
    class ViewHolderCoin(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
    class ViewHolderDivider(val containerView: View) : RecyclerView.ViewHolder(containerView)
}
