package io.horizontalsystems.bankwallet.modules.settings.managekeys.views

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.settings.managekeys.ManageAccountItem
import io.horizontalsystems.bankwallet.modules.settings.managekeys.ManageKeysViewModel
import io.horizontalsystems.bankwallet.viewHelpers.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_account.*

class ManageKeysAdapter(private val viewModel: ManageKeysViewModel) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = listOf<ManageAccountItem>()

    private val keys = 1
    private val keysInfo = 2

    override fun getItemCount() = items.size + 1
    override fun getItemViewType(position: Int) = if (position == 0) keysInfo else keys

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            keys -> KeysViewHolder(inflate(parent, R.layout.view_holder_account))
            else -> KeysInfoViewHolder(inflate(parent, R.layout.view_holder_account_info))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is KeysViewHolder) {
            holder.bind(items[position - 1])
        }
    }

    inner class KeysViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bind(item: ManageAccountItem) {
            hideButtons()
            changeStates(isEnabled = true)

            val predefinedAccount = item.predefinedAccountType
            val accountTypeTitle = containerView.resources.getString(predefinedAccount.title)
            accountName.text = containerView.resources.getString(R.string.Wallet, accountTypeTitle)
            accountCoins.text = containerView.resources.getString(predefinedAccount.coinCodes)

            buttonNew.isEnabled = predefinedAccount.createSupported()

            if (item.account == null) {
                changeStates(isEnabled = false)

                buttonNew.visibility = View.VISIBLE
                buttonNew.setOnClickListener {
                    viewModel.delegate.onClickCreate(item)
                }

                buttonImport.visibility = View.VISIBLE
                buttonImport.setOnClickListener { viewModel.delegate.onClickRestore(item) }

                return
            }

            val account = item.account
            if (account.isBackedUp) {
                buttonShow.visibility = View.VISIBLE
                buttonShow.setOnClickListener { viewModel.delegate.onClickBackup(item) }
            } else {
                buttonBackup.visibility = View.VISIBLE
            }

            buttonUnlink.visibility = View.VISIBLE
            buttonUnlink.setOnClickListener { viewModel.delegate.onClickUnlink(item) }
            buttonBackup.setOnClickListener { viewModel.delegate.onClickBackup(item) }
        }

        private fun hideButtons() {
            buttonNew.visibility = View.GONE
            buttonImport.visibility = View.GONE
            buttonUnlink.visibility = View.GONE
            buttonShow.visibility = View.GONE
            buttonBackup.visibility = View.GONE
        }

        private fun changeStates(isEnabled: Boolean) {
            viewHolderRoot.isEnabled = isEnabled
            accountName.isEnabled = isEnabled
            accountCoins.isEnabled = isEnabled

            keyIcon.isEnabled = isEnabled
            keyIcon.alpha = if (isEnabled) 1F else 0.25F
        }
    }

    class KeysInfoViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
}
