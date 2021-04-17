package io.horizontalsystems.bankwallet.modules.manageaccounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.manageaccount.ManageAccountModule
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule.AccountViewItem
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule.ActionViewItem
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.ListPosition
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_manage_accounts.*
import kotlinx.android.synthetic.main.view_holder_manage_account_action.*
import kotlinx.android.synthetic.main.view_holder_manage_account_action.backgroundView
import kotlinx.android.synthetic.main.view_holder_manage_account_action.title
import kotlinx.android.synthetic.main.view_holder_manage_account_item.*

class ManageAccountsFragment : BaseFragment(), AccountViewHolder.Listener {
    private val viewModel by viewModels<ManageAccountsViewModel> { ManageAccountsModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_manage_accounts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val accountsAdapter = AccountsAdapter(this)
        val actionsAdapter = ActionsAdapter(listOf(
                ActionViewItem(R.drawable.ic_plus, R.string.ManageAccounts_CreateNewWallet, ::onClickCreateWallet),
                ActionViewItem(R.drawable.ic_download, R.string.ManageAccounts_ImportWallet, ::onClickRestoreWallet)
        ))

        val concatAdapter = ConcatAdapter(accountsAdapter, MarginAdapter(), actionsAdapter)
        recyclerView.adapter = concatAdapter

        viewModel.viewItemsLiveData.observe(viewLifecycleOwner, { items ->
            accountsAdapter.items = items
            accountsAdapter.notifyDataSetChanged()
        })
    }

    private fun onClickCreateWallet() {
        findNavController().navigate(R.id.manageAccountsFragment_to_createAccountFragment, null, navOptions())
    }

    private fun onClickRestoreWallet() {
        findNavController().navigate(R.id.manageAccountsFragment_to_restoreMnemonicFragment, null, navOptions())
    }

    override fun onSelect(accountViewItem: AccountViewItem) {
        viewModel.onSelect(accountViewItem)
    }

    override fun onEdit(accountViewItem: AccountViewItem) {
        ManageAccountModule.start(this, R.id.manageAccountsFragment_to_manageAccount, navOptions(), accountViewItem.accountId)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        recyclerView.adapter = null
    }

}

class AccountsAdapter(private val listener: AccountViewHolder.Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var items: List<AccountViewItem> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return AccountViewHolder.create(parent, listener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as? AccountViewHolder)?.bind(items[position], ListPosition.getListPosition(items.size, position))
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

class AccountViewHolder(override val containerView: View, val listener: Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    interface Listener {
        fun onSelect(accountViewItem: AccountViewItem)
        fun onEdit(accountViewItem: AccountViewItem)
    }

    fun bind(account: AccountViewItem, position: ListPosition) {
        title.text = account.title
        subtitle.text = account.subtitle

        backgroundView.setBackgroundResource(position.getBackground())
        radioImage.setImageResource(if (account.selected) R.drawable.ic_radion else R.drawable.ic_radioff)
        attentionIcon.isVisible = account.alert
        editIcon.setOnClickListener {
            listener.onEdit(account)
        }

        backgroundView.setOnClickListener {
            listener.onSelect(account)
        }
    }

    companion object {
        fun create(parent: ViewGroup, listener: Listener): AccountViewHolder {
            return AccountViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_manage_account_item, parent, false), listener)
        }
    }
}

class MarginAdapter : RecyclerView.Adapter<MarginViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarginViewHolder {
        return MarginViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: MarginViewHolder, position: Int) {}

    override fun getItemCount(): Int {
        return 1
    }
}

class MarginViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    companion object {
        fun create(parent: ViewGroup): MarginViewHolder {
            return MarginViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_settings_item_space, parent, false))
        }
    }
}

class ActionsAdapter(
        private val items: List<ActionViewItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ActionViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as? ActionViewHolder)?.bind(items[position], ListPosition.Companion.getListPosition(items.size, position))
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

class ActionViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    fun bind(action: ActionViewItem, position: ListPosition) {
        icon.setImageResource(action.icon)
        title.setText(action.title)
        backgroundView.setBackgroundResource(position.getBackground())
        backgroundView.setOnSingleClickListener { action.callback() }
    }

    companion object {
        fun create(parent: ViewGroup): ActionViewHolder {
            return ActionViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_manage_account_action, parent, false))
        }
    }
}
