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
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_manage_account.*
import kotlinx.android.synthetic.main.view_holder_account_setting_view.*

class ManageAccountFragment : BaseFragment(), UnlinkConfirmationDialog.Listener {
    private val viewModel by viewModels<ManageAccountViewModel> {
        ManageAccountModule.Factory(
            arguments?.getString(ACCOUNT_ID_KEY)!!
        )
    }
    private var saveMenuItem: MenuItem? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_manage_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        saveMenuItem = toolbar.menu.findItem(R.id.menuSave)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuSave -> {
                    viewModel.onSave()
                    true
                }
                else -> false
            }
        }

        toolbar.title = viewModel.account.name
        name.setText(viewModel.account.name)

        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if (s.isNotEmpty()) {
                    viewModel.onChange(s.toString())
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        name.addTextChangedListener(textWatcher)

        unlinkButton.setOnSingleClickListener {
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

        networkSettings.setOnClickListener {
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
                    actionButton.showAttention(false)
                    actionButton.showTitle(getString(R.string.ManageAccount_RecoveryPhraseShow))
                    actionButton.setOnClickListener {
                        ShowKeyModule.start(
                            this,
                            R.id.manageAccountFragment_to_showKeyFragment,
                            navOptions(),
                            viewModel.account
                        )
                    }
                }
                KeyActionState.BackupRecoveryPhrase -> {
                    actionButton.showAttention(true)
                    actionButton.showTitle(getString(R.string.ManageAccount_RecoveryPhraseBackup))
                    actionButton.setOnClickListener {
                        openBackupModule(viewModel.account)
                    }
                }
            }
        })

        viewModel.additionalViewItemsLiveData.observe(viewLifecycleOwner, { additionalItems ->
            if (additionalItems.isNotEmpty()) {
                additionalInfoItems.adapter = AdditionalInfoAdapter(additionalItems)
            } else {
                networkSettings.setListPosition(ListPosition.Last)
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

    override fun onDestroyView() {
        super.onDestroyView()

        saveMenuItem = null
    }

}

class AdditionalInfoAdapter(
    private val items: List<ManageAccountViewModel.AdditionalViewItem> = listOf()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return AdditionalInfoViewHolder.create(parent)
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

class AdditionalInfoViewHolder(override val containerView: View) :
    RecyclerView.ViewHolder(containerView), LayoutContainer {
    fun bind(additionViewItem: ManageAccountViewModel.AdditionalViewItem, position: ListPosition) {
        val platformCoin = additionViewItem.platformCoin
        icon.setRemoteImage(platformCoin.coin.iconUrl, platformCoin.coinType.iconPlaceholder)
        title.text = additionViewItem.title
        containerView.setBackgroundResource(position.getBackground())

        decoratedTextCompose.setContent {
            ComposeAppTheme {
                ButtonSecondaryDefault(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = additionViewItem.value,
                    onClick = {
                        TextHelper.copyText(additionViewItem.value)
                        HudHelper.showSuccessMessage(containerView, R.string.Hud_Text_Copied)
                    }
                )
            }
        }
    }

    companion object {
        fun create(parent: ViewGroup): AdditionalInfoViewHolder {
            return AdditionalInfoViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_holder_account_setting_view, parent, false)
            )
        }
    }
}

