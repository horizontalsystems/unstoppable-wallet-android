package io.horizontalsystems.bankwallet.modules.settings.managekeys.views

import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.settings.managekeys.ManageAccountItem
import io.horizontalsystems.uikit.AccountButtonItemType
import io.horizontalsystems.uikit.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_account.*

class ManageKeysAdapter(private val listener: Listener) : RecyclerView.Adapter<ManageKeysAdapter.KeysViewHolder>() {

    interface Listener {
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

        fun bind(item: ManageAccountItem) {
            val predefinedAccount = item.predefinedAccountType
            val accountTypeTitle = containerView.resources.getString(predefinedAccount.title)

            titleText.text = containerView.resources.getString(R.string.Wallet, accountTypeTitle)
            subtitleText.text = containerView.resources.getString(predefinedAccount.coinCodes)

            createButton.visibility = View.GONE
            restoreButton.visibility = View.GONE
            backupButton.visibility = View.GONE
            unlinkButton.visibility = View.GONE

            if (item.account == null) {
                if (predefinedAccount.isCreationSupported()) {
                    createButton.visibility = View.VISIBLE
                    createButton.bind(containerView.resources.getString(R.string.ManageKeys_Create), AccountButtonItemType.SimpleButton, false) {
                        listener.onClickCreate(item)
                    }
                }

                restoreButton.visibility = View.VISIBLE
                restoreButton.bind(containerView.resources.getString(R.string.ManageKeys_Restore), AccountButtonItemType.SimpleButton, false) {
                    listener.onClickRestore(item)
                }

                headerIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(containerView.context, R.color.grey))

                return
            }

            headerIcon.imageTintList = null

            val account = item.account

            backupButton.visibility = View.VISIBLE
            unlinkButton.visibility = View.VISIBLE

            backupButton.bind(
                    title = containerView.resources.getString(R.string.ManageKeys_Backup),
                    type = AccountButtonItemType.SimpleButton,
                    showAttentionIcon = !account.isBackedUp,
                    onClick = {
                        listener.onClickBackup(item)
                    })

            unlinkButton.bind(
                    title = containerView.resources.getString(R.string.ManageKeys_Unlink),
                    type = AccountButtonItemType.RedButton,
                    onClick = {
                        listener.onClickUnlink(item)
                    })
        }

    }

}
