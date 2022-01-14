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
import io.horizontalsystems.bankwallet.databinding.*
import io.horizontalsystems.bankwallet.modules.manageaccount.ManageAccountModule
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule.AccountViewItem
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule.ActionViewItem
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.ListPosition

class ManageAccountsFragment : BaseFragment(), AccountViewHolder.Listener {
    private val viewModel by viewModels<ManageAccountsViewModel> {
        ManageAccountsModule.Factory(
            arguments?.getParcelable(ManageAccountsModule.MODE)!!
        )
    }

    private var _binding: FragmentManageAccountsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageAccountsBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.menu.findItem(R.id.menuCancel)?.isVisible = viewModel.isCloseButtonVisible
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuClose -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        if (viewModel.isCloseButtonVisible) {
            binding.toolbar.navigationIcon = null
        }
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val accountsAdapter = AccountsAdapter(this)
        val actionsAdapter = ActionsAdapter(
            listOf(
                ActionViewItem(
                    R.drawable.ic_plus,
                    R.string.ManageAccounts_CreateNewWallet,
                    ::onClickCreateWallet
                ),
                ActionViewItem(
                    R.drawable.ic_download,
                    R.string.ManageAccounts_ImportWallet,
                    ::onClickRestoreWallet
                ),
                ActionViewItem(
                    R.drawable.ic_eye_2_20,
                    R.string.ManageAccounts_WatchAddress,
                    ::onClickWatch
                )
            )
        )

        val concatAdapter =
            ConcatAdapter(accountsAdapter, actionsAdapter, ManageAccountsHintAdapter())
        binding.recyclerView.adapter = concatAdapter

        viewModel.viewItemsLiveData.observe(viewLifecycleOwner, { items ->
            accountsAdapter.items = items
            accountsAdapter.notifyDataSetChanged()
        })

        viewModel.finishLiveEvent.observe(viewLifecycleOwner, {
            findNavController().popBackStack()
        })
    }

    private fun onClickCreateWallet() {
        findNavController().navigate(
            R.id.manageAccountsFragment_to_createAccountFragment,
            null,
            navOptions()
        )
    }

    private fun onClickRestoreWallet() {
        findNavController().navigate(
            R.id.manageAccountsFragment_to_restoreMnemonicFragment,
            null,
            navOptions()
        )
    }

    private fun onClickWatch() {
        findNavController().navigate(
            R.id.watchAddressFragment,
            null,
            navOptions()
        )
    }

    override fun onSelect(accountViewItem: AccountViewItem) {
        viewModel.onSelect(accountViewItem)
    }

    override fun onEdit(accountViewItem: AccountViewItem) {
        ManageAccountModule.start(
            this,
            R.id.manageAccountsFragment_to_manageAccount,
            navOptions(),
            accountViewItem.accountId
        )
    }

}

class AccountsAdapter(private val listener: AccountViewHolder.Listener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var items: List<AccountViewItem> = listOf()

    private val itemView = 0
    private val bottomMarginView = 1

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            itemCount - 1 -> bottomMarginView
            else -> itemView
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            bottomMarginView -> {
                MarginViewHolder(
                    ViewSettingsItemSpaceBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                )
            }
            else -> {
                AccountViewHolder(
                    ViewHolderManageAccountItemBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    ), listener)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as? AccountViewHolder)?.bind(
            items[position],
            ListPosition.getListPosition(items.size, position)
        )
    }

    override fun getItemCount(): Int {
        return if (items.isEmpty()) 0 else items.size + 1
    }
}

class AccountViewHolder(
    private val binding: ViewHolderManageAccountItemBinding,
    val listener: Listener
) : RecyclerView.ViewHolder(binding.root) {

    interface Listener {
        fun onSelect(accountViewItem: AccountViewItem)
        fun onEdit(accountViewItem: AccountViewItem)
    }

    fun bind(account: AccountViewItem, position: ListPosition) {
        binding.title.text = account.title
        binding.subtitle.text = account.subtitle

        binding.backgroundView.setBackgroundResource(position.getBackground())
        binding.radioImage.setImageResource(if (account.selected) R.drawable.ic_radion else R.drawable.ic_radioff)
        binding.attentionIcon.isVisible = account.alert
        binding.editIcon.setOnClickListener {
            listener.onEdit(account)
        }

        binding.backgroundView.setOnClickListener {
            listener.onSelect(account)
        }
    }

}

class MarginViewHolder(binding: ViewSettingsItemSpaceBinding) :
    RecyclerView.ViewHolder(binding.root)

class ActionsAdapter(
    private val items: List<ActionViewItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ActionViewHolder(
            ViewHolderManageAccountActionBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as? ActionViewHolder)?.bind(
            items[position],
            ListPosition.Companion.getListPosition(items.size, position)
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

class ActionViewHolder(private val binding: ViewHolderManageAccountActionBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(action: ActionViewItem, position: ListPosition) {
        binding.icon.setImageResource(action.icon)
        binding.title.setText(action.title)
        binding.backgroundView.setBackgroundResource(position.getBackground())
        binding.backgroundView.setOnSingleClickListener { action.callback() }
    }
}

class ManageAccountsHintAdapter : RecyclerView.Adapter<ManageAccountHintViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManageAccountHintViewHolder {
        return ManageAccountHintViewHolder(
            ViewHolderManageAccountsHintBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ManageAccountHintViewHolder, position: Int) {}

    override fun getItemCount(): Int {
        return 1
    }
}

class ManageAccountHintViewHolder(binding: ViewHolderManageAccountsHintBinding) :
    RecyclerView.ViewHolder(binding.root)
