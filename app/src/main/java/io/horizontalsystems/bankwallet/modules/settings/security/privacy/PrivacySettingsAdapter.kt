package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CommunicationMode
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.modules.settings.security.privacy.PrivacySettingsModule.IPrivacySettingsViewDelegate
import io.horizontalsystems.views.CellView
import kotlinx.android.extensions.LayoutContainer

sealed class PrivacySettingsType {
    open val selectedTitle: String = ""

    class Communication(var selected: CommunicationMode) : PrivacySettingsType() {
        override val selectedTitle: String
            get() = selected.title
    }

    class WalletRestore(var selected: SyncMode) : PrivacySettingsType() {
        override val selectedTitle: String
            get() = selected.title
    }
}

data class PrivacySettingsViewItem(
        val coin: Coin,
        val settingType: PrivacySettingsType,
        val enabled: Boolean = true

)

class PrivacySettingsAdapter(
        private val delegate: IPrivacySettingsViewDelegate
) : RecyclerView.Adapter<PrivacySettingsItemViewHolder>() {

    var items = listOf<PrivacySettingsViewItem>()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return items[position].hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrivacySettingsItemViewHolder {
        val cellView = CellView(parent.context)
        cellView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return PrivacySettingsItemViewHolder(cellView, delegate)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: PrivacySettingsItemViewHolder, position: Int) {
        holder.bind(items[position], position == itemCount - 1)
    }
}


class PrivacySettingsItemViewHolder(
        override val containerView: CellView,
        private val viewDelegate: IPrivacySettingsViewDelegate
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(viewItem: PrivacySettingsViewItem, lastElement: Boolean) {
        containerView.coinIcon = viewItem.coin.code
        containerView.title = viewItem.coin.title
        containerView.subtitle = null
        containerView.dropDownText = viewItem.settingType.selectedTitle
        containerView.dropDownArrow = viewItem.enabled
        containerView.bottomBorder = lastElement

        containerView.setOnClickListener {
            viewDelegate.didTapItem(viewItem.settingType, adapterPosition)
        }
        containerView.isEnabled = viewItem.enabled
    }

}

