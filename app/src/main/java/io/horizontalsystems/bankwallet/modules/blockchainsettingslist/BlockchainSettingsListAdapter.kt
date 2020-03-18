package io.horizontalsystems.bankwallet.modules.blockchainsettingslist

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.BlockchainSetting
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_blockchain_settings_list_description.*
import kotlinx.android.synthetic.main.view_holder_coin_manage_item.*

class BlockchainSettingsListAdapter(private val listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun select(viewItem: BlockchainSettingListViewItem)
    }

    private val settingType = 1
    private val descriptionType = 2
    var viewItems = listOf<BlockchainSettingListViewItem>()

    override fun getItemCount(): Int {
        return viewItems.size  + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1) descriptionType else settingType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            settingType -> CoinItemWithArrowViewHolder(inflate(parent, R.layout.view_holder_blockchain_item, false))
            descriptionType -> ViewHolderDescription(inflate(parent, R.layout.view_holder_blockchain_settings_list_description))
            else -> throw Exception("No such view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CoinItemWithArrowViewHolder) {
            holder.bind(viewItems[position], onClick = { listener.select(viewItems[position]) })
        } else if (holder is ViewHolderDescription) {
            val visible = viewItems.firstOrNull { it.enabled } == null
            holder.bind(visible)
        }
    }

}

class ViewHolderDescription(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    fun bind(visible: Boolean) {
        descriptionText.visibility = if (visible) View.VISIBLE else View.GONE
    }
}

class CoinItemWithArrowViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(viewItem: BlockchainSettingListViewItem, onClick: (position: Int) -> Unit) {
        if (viewItem.enabled) {
            containerView.alpha = 1f
            containerView.isEnabled = true
            containerView.setOnClickListener {
                onClick.invoke(adapterPosition)
            }
        } else {
            containerView.alpha = 0.5f
            containerView.isEnabled = false
        }

        val coin = viewItem.coin

        coinTitle.text = coin.title
        coinSubtitle.text = viewItem.setting?.let { setting ->
            val derivationText = setting.derivation?.let {  AccountType.getDerivationLongTitle(it) } ?: ""
            val syncModeText = getSyncModeTitle(setting.syncMode)
            val middleText = if (derivationText.isNotEmpty() && syncModeText.isNotEmpty()) " | " else ""
            val text = derivationText + middleText + getSyncModeTitle(setting.syncMode)
            text
        }

        rightArrow.visibility = View.VISIBLE
        coinIcon.bind(coin.code)
        bottomShade.visibility = if (viewItem.showBottomShade) View.VISIBLE else View.GONE
    }

    private fun getSyncModeTitle(syncMode: SyncMode?): String{
        return when(syncMode){
            SyncMode.Fast, SyncMode.New -> containerView.context.getString(R.string.CoinOption_Fast)
            SyncMode.Slow -> containerView.context.getString(R.string.CoinOption_Slow)
            else -> ""
        }
    }
}


data class BlockchainSettingListViewItem(val coin: Coin, val setting: BlockchainSetting?, var enabled: Boolean = false, var showBottomShade: Boolean = false)
