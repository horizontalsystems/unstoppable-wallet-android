package io.horizontalsystems.bankwallet.modules.manageaccount

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.manageaccount.ManageAccountModule.ACCOUNT_ID_KEY
import io.horizontalsystems.bankwallet.modules.manageaccount.ManageAccountViewModel.KeyActionState
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.BackupRequiredDialog
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.UnlinkConfirmationDialog
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_manage_account.*
import kotlinx.android.synthetic.main.fragment_manage_accounts.toolbar

class ManageAccountFragment : BaseFragment(), BackupRequiredDialog.Listener, UnlinkConfirmationDialog.Listener {
    private val viewModel by viewModels<ManageAccountViewModel> { ManageAccountModule.Factory(arguments?.getString(ACCOUNT_ID_KEY)!!) }
    private var saveMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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

        toolbar.title = viewModel.accountName
        name.setText(viewModel.accountName)

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
            viewModel.onUnlink()
        }

        actionButton.setOnSingleClickListener {
            viewModel.onClickActionButton()
        }

        childFragmentManager.addFragmentOnAttachListener { _, fragment ->
            when (fragment) {
                is BackupRequiredDialog -> fragment.setListener(this)
                is UnlinkConfirmationDialog -> fragment.setListener(this)
            }
        }

        viewModel.confirmBackupLiveEvent.observe(viewLifecycleOwner, {
            BackupRequiredDialog.show(childFragmentManager, viewModel.accountName)
        })

        viewModel.confirmUnlinkLiveEvent.observe(viewLifecycleOwner, {
            val confirmationList = listOf(
                    getString(R.string.ManageAccount_Delete_ConfirmationRemove),
                    getString(R.string.ManageAccount_Delete_ConfirmationDisable),
                    getString(R.string.ManageAccount_Delete_ConfirmationLose)
            )

            UnlinkConfirmationDialog.show(childFragmentManager, viewModel.accountName, confirmationList)
        })

        viewModel.keyActionStateLiveData.observe(viewLifecycleOwner, { keyActionState ->
            when (keyActionState) {
                KeyActionState.ShowRecoveryPhrase -> {
                    actionButton.showAttention(false)
                    actionButton.showTitle(getString(R.string.ManageAccount_RecoveryPhraseShow))
                }
                KeyActionState.BackupRecoveryPhrase -> {
                    actionButton.showAttention(true)
                    actionButton.showTitle(getString(R.string.ManageAccount_RecoveryPhraseBackup))
                }
            }
        })

        viewModel.saveEnabledLiveData.observe(viewLifecycleOwner, {
            saveMenuItem?.isEnabled = it
        })

        viewModel.finishLiveEvent.observe(viewLifecycleOwner, {
            findNavController().popBackStack()
        })
    }

    override fun onClickBackup() {
        viewModel.onClickBackup()
    }

    override fun onUnlinkConfirm() {
        viewModel.onUnlinkConfirm()
    }
}
