package io.horizontalsystems.bankwallet.modules.settings.managekeys.views

import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.settings.managekeys.ManageAccountItem
import io.horizontalsystems.views.helpers.LayoutHelper
import io.horizontalsystems.views.inflate
import io.horizontalsystems.views.showIf
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_account.*

class ManageKeysAdapter(private val listener: Listener) : RecyclerView.Adapter<ManageKeysAdapter.KeysViewHolder>() {

    interface Listener {
        fun onClickAdvancedSettings(item: ManageAccountItem)
        fun onClickCreate(item: ManageAccountItem)
        fun onClickRestore(item: ManageAccountItem)
        fun onClickBackup(item: ManageAccountItem)
        fun onClickUnlink(item: ManageAccountItem)
    }

    var items = listOf<ManageAccountItem>()

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeysViewHolder {
        return KeysViewHolder(inflate(parent, R.layout.view_holder_account))
    }

    override fun onBindViewHolder(holder: KeysViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class KeysViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        private lateinit var item: ManageAccountItem

        init {
            createButton.setOnClickListener {
                listener.onClickCreate(item)
            }

            restoreButton.setOnClickListener {
                listener.onClickRestore(item)
            }

            backupButton.setOnClickListener {
                listener.onClickBackup(item)
            }

            showKeyButton.setOnClickListener {
                listener.onClickBackup(item)
            }

            unlinkButton.setOnClickListener {
                listener.onClickUnlink(item)
            }

            advancedSettingsButton.setOnClickListener {
                listener.onClickAdvancedSettings(item)
            }
        }

        fun bind(item: ManageAccountItem) {
            this.item = item

            val predefinedAccount = item.predefinedAccountType
            val hasAccount = item.account != null
            val isBackedUp = item.account?.isBackedUp == true

            val accountTypeTitle = containerView.resources.getString(predefinedAccount.title)
            titleText.text = containerView.resources.getString(R.string.Wallet, accountTypeTitle)
            subtitleText.text = containerView.resources.getString(predefinedAccount.coinCodes)

            containerView.isActivated = hasAccount

            advancedSettingsButton.showIf(hasAccount && predefinedAccount == PredefinedAccountType.Standard && item.hasDerivationSetting)
            backupButton.showIf(hasAccount && !isBackedUp)
            showKeyButton.showIf(hasAccount && isBackedUp)
            unlinkButton.showIf(hasAccount)
            createButton.showIf(!hasAccount && predefinedAccount.isCreationSupported())
            restoreButton.showIf(!hasAccount)


            val padding = if (hasAccount) LayoutHelper.dp(1f, containerView.context) else 0
            containerView.setPadding(padding, 0, padding, padding)
            headerIcon.imageTintList = if (hasAccount) null else ColorStateList.valueOf(ContextCompat.getColor(containerView.context, R.color.grey))
        }
    }

}
