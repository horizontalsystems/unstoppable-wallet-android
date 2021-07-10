package io.horizontalsystems.bankwallet.modules.manageaccounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
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

class ManageAccountsFragment : BaseFragment(), AccountViewHolder.Listener {
    private val viewModel by viewModels<ManageAccountsViewModel> { ManageAccountsModule.Factory(arguments?.getParcelable(ManageAccountsModule.MODE)!!) }
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_manage_accounts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        recyclerView = view.findViewById(R.id.recyclerView)

        toolbar.menu.findItem(R.id.menuCancel)?.isVisible = viewModel.isCloseButtonVisible
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuCancel -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        if (viewModel.isCloseButtonVisible) {
            toolbar.navigationIcon = null
        }
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val accountsAdapter = AccountsAdapter(this)
        val actionsAdapter = ActionsAdapter(listOf(
                ActionViewItem(R.drawable.ic_plus, R.string.ManageAccounts_CreateNewWallet, ::onClickCreateWallet),
                ActionViewItem(R.drawable.ic_download, R.string.ManageAccounts_ImportWallet, ::onClickRestoreWallet)
        ))

        val concatAdapter = ConcatAdapter(accountsAdapter, MarginAdapter(), actionsAdapter, ManageAccountsHintAdapter())
        recyclerView.adapter = concatAdapter

        viewModel.viewItemsLiveData.observe(viewLifecycleOwner, { items ->
            accountsAdapter.items = items
            accountsAdapter.notifyDataSetChanged()
        })

        viewModel.finishLiveEvent.observe(viewLifecycleOwner, {
            findNavController().popBackStack()
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
        containerView.findViewById<TextView>(R.id.title).text = account.title
        containerView.findViewById<TextView>(R.id.subtitle).text = account.subtitle
        val backgroundView = containerView.findViewById<View>(R.id.backgroundView)

        backgroundView.setBackgroundResource(position.getBackground())
        containerView.findViewById<ImageView>(R.id.radioImage).setImageResource(if (account.selected) R.drawable.ic_radion else R.drawable.ic_radioff)
        containerView.findViewById<ImageView>(R.id.attentionIcon).isVisible = account.alert
        containerView.findViewById<ImageView>(R.id.editIcon).setOnClickListener {
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
        containerView.findViewById<ImageView>(R.id.icon).setImageResource(action.icon)
        containerView.findViewById<TextView>(R.id.title).setText(action.title)
        val backgroundView = containerView.findViewById<View>(R.id.backgroundView)
        backgroundView.setBackgroundResource(position.getBackground())
        backgroundView.setOnSingleClickListener { action.callback() }
    }

    companion object {
        fun create(parent: ViewGroup): ActionViewHolder {
            return ActionViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_manage_account_action, parent, false))
        }
    }
}

class ManageAccountsHintAdapter : RecyclerView.Adapter<ManageAccountHintViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManageAccountHintViewHolder {
        return ManageAccountHintViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: ManageAccountHintViewHolder, position: Int) {}

    override fun getItemCount(): Int {
        return 1
    }
}

class ManageAccountHintViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    companion object {
        fun create(parent: ViewGroup): ManageAccountHintViewHolder {
            return ManageAccountHintViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_manage_accounts_hint, parent, false))
        }
    }
}
