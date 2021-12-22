package io.horizontalsystems.bankwallet.modules.manageaccount

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.databinding.FragmentManageAccountBinding
import io.horizontalsystems.bankwallet.databinding.ViewHolderAccountSettingViewBinding
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.backupkey.BackupKeyModule
import io.horizontalsystems.bankwallet.modules.manageaccount.ManageAccountModule.ACCOUNT_ID_KEY
import io.horizontalsystems.bankwallet.modules.manageaccount.ManageAccountViewModel.KeyActionState
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.UnlinkConfirmationDialog
import io.horizontalsystems.bankwallet.modules.networksettings.NetworkSettingsModule
import io.horizontalsystems.bankwallet.modules.showkey.ShowKeyModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.ListPosition

class ManageAccountFragment : BaseFragment(), UnlinkConfirmationDialog.Listener {
    private val viewModel by viewModels<ManageAccountViewModel> {
        ManageAccountModule.Factory(
            arguments?.getString(ACCOUNT_ID_KEY)!!
        )
    }
    private var saveMenuItem: MenuItem? = null

    private var _binding: FragmentManageAccountBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageAccountBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        saveMenuItem = null
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        saveMenuItem = binding.toolbar.menu.findItem(R.id.menuSave)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuSave -> {
                    viewModel.onSave()
                    true
                }
                else -> false
            }
        }

        binding.toolbar.title = viewModel.account.name
        binding.name.setText(viewModel.account.name)

        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if (s.isNotEmpty()) {
                    viewModel.onChange(s.toString())
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        binding.name.addTextChangedListener(textWatcher)

        binding.unlinkButton.setOnSingleClickListener {
            val confirmationList = listOf(
                getString(R.string.ManageAccount_Delete_ConfirmationRemove),
                getString(R.string.ManageAccount_Delete_ConfirmationDisable),
                getString(R.string.ManageAccount_Delete_ConfirmationLose)
            )
            UnlinkConfirmationDialog.show(
                childFragmentManager,
                viewModel.account.name,
                confirmationList
            )
        }

        binding.networkSettings.setOnClickListener {
            openNetworkSettings(viewModel.account)
        }

        childFragmentManager.addFragmentOnAttachListener { _, fragment ->
            when (fragment) {
                is UnlinkConfirmationDialog -> fragment.setListener(this)
            }
        }

        viewModel.keyActionStateLiveData.observe(viewLifecycleOwner, { keyActionState ->
            when (keyActionState) {
                KeyActionState.ShowRecoveryPhrase -> {
                    binding.actionButton.showAttention(false)
                    binding.actionButton.showTitle(getString(R.string.ManageAccount_RecoveryPhraseShow))
                    binding.actionButton.setOnClickListener {
                        ShowKeyModule.start(
                            this,
                            R.id.manageAccountFragment_to_showKeyFragment,
                            navOptions(),
                            viewModel.account
                        )
                    }
                }
                KeyActionState.BackupRecoveryPhrase -> {
                    binding.actionButton.showAttention(true)
                    binding.actionButton.showTitle(getString(R.string.ManageAccount_RecoveryPhraseBackup))
                    binding.actionButton.setOnClickListener {
                        openBackupModule(viewModel.account)
                    }
                }
            }
        })

        viewModel.additionalViewItemsLiveData.observe(viewLifecycleOwner, { additionalItems ->
            if (additionalItems.isNotEmpty()) {
                binding.additionalInfoItems.adapter = AdditionalInfoAdapter(additionalItems)
            } else {
                binding.networkSettings.setListPosition(ListPosition.Last)
            }
        })

        viewModel.saveEnabledLiveData.observe(viewLifecycleOwner, {
            saveMenuItem?.isEnabled = it
        })

        viewModel.finishLiveEvent.observe(viewLifecycleOwner, {
            findNavController().popBackStack()
        })
    }

    private fun openBackupModule(account: Account) {
        BackupKeyModule.start(
            this,
            R.id.manageAccountFragment_to_backupKeyFragment,
            navOptions(),
            account
        )
    }

    private fun openNetworkSettings(account: Account) {
        findNavController().navigate(
            R.id.manageAccountFragment_to_networkSettingsFragment,
            NetworkSettingsModule.args(account),
            navOptions()
        )
    }

    override fun onUnlinkConfirm() {
        viewModel.onUnlink()
        HudHelper.showSuccessMessage(requireView(), getString(R.string.Hud_Text_Done))
    }

}

class AdditionalInfoAdapter(
    private val items: List<ManageAccountViewModel.AdditionalViewItem> = listOf()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return AdditionalInfoViewHolder(
            ViewHolderAccountSettingViewBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listPosition =
            if (position == items.size - 1) ListPosition.Last else ListPosition.Middle
        (holder as? AdditionalInfoViewHolder)?.bind(items[position], listPosition)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

class AdditionalInfoViewHolder(val binding: ViewHolderAccountSettingViewBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(additionViewItem: ManageAccountViewModel.AdditionalViewItem, position: ListPosition) {
        val platformCoin = additionViewItem.platformCoin
        binding.icon.setRemoteImage(
            platformCoin.coin.iconUrl,
            platformCoin.coinType.iconPlaceholder
        )
        binding.title.text = additionViewItem.title
        binding.wrapper.setBackgroundResource(position.getBackground())

        binding.decoratedTextCompose.setContent {
            ComposeAppTheme {
                ButtonSecondaryDefault(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = additionViewItem.value,
                    onClick = {
                        TextHelper.copyText(additionViewItem.value)
                        HudHelper.showSuccessMessage(binding.wrapper, R.string.Hud_Text_Copied)
                    }
                )
            }
        }
    }

}
