package io.horizontalsystems.bankwallet.modules.settings.managekeys.views

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.EosAccountType
import io.horizontalsystems.bankwallet.entities.Words12AccountType
import io.horizontalsystems.bankwallet.entities.Words24AccountType
import io.horizontalsystems.bankwallet.modules.settings.managekeys.ManageAccountItem
import io.horizontalsystems.bankwallet.modules.settings.managekeys.ManageKeysViewModel
import io.horizontalsystems.bankwallet.viewHelpers.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_account.*

class ManageKeysAdapter(private val viewModel: ManageKeysViewModel) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = listOf<ManageAccountItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return KeysViewHolder(inflate(parent, R.layout.view_holder_account))
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is KeysViewHolder) {
            holder.bind(items[position])
        }
    }

    inner class KeysViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bind(item: ManageAccountItem) {
            hideButtons()
            changeStates(isEnabled = true)

            val predefinedAccount = item.predefinedAccountType
            accountName.text = predefinedAccount.title
            accountCoins.text = predefinedAccount.coinCodes

            if (item.account == null) {
                changeStates(isEnabled = false)

                when (predefinedAccount) {
                    is EosAccountType -> {
                        buttonImport.visibility = View.VISIBLE
                    }
                    is Words12AccountType,
                    is Words24AccountType -> {
                        buttonImport.visibility = View.VISIBLE
                        buttonNew.visibility = View.VISIBLE
                        buttonNew.setOnClickListener {
                            viewModel.delegate.onClickNew(predefinedAccount)
                        }
                    }
                }

                buttonImport.setOnClickListener { viewModel.delegate.onClickRestore(predefinedAccount) }

                return
            }

            val account = item.account
            if (account.isBackedUp) {
                buttonShow.visibility = View.VISIBLE
            } else {
                buttonBackup.visibility = View.VISIBLE
            }

            buttonUnlink.visibility = View.VISIBLE
            buttonUnlink.setOnClickListener { viewModel.confirmUnlink(account) }
            buttonBackup.setOnClickListener { viewModel.delegate.onClickBackup(account) }
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
}
