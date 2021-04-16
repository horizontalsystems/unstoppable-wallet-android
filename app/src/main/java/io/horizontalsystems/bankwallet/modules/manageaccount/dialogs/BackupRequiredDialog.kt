package io.horizontalsystems.bankwallet.modules.manageaccount.dialogs

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_bottom_backup_required.*

class BackupRequiredDialog : BaseBottomSheetDialogFragment() {

    interface Listener {
        fun onClickBackup()
    }

    private var listener: Listener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setContentView(R.layout.fragment_bottom_backup_required)

        setTitle(getString(R.string.ManageAccount_BackupRequired_Title))
        setSubtitle(requireArguments().getString(ACCOUNT_NAME))
        setHeaderIcon(R.drawable.ic_attention_red_24)

        backupButton.setOnClickListener {
            listener?.onClickBackup()
            dismiss()
        }
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    companion object {
        private const val ACCOUNT_NAME = "account_name"

        fun show(fragmentManager: FragmentManager, accountName: String) {
            val fragment = BackupRequiredDialog().apply {
                arguments = bundleOf(ACCOUNT_NAME to accountName)
            }

            fragmentManager.beginTransaction().apply {
                add(fragment, "backup_required_dialog")
                commitAllowingStateLoss()
            }
        }
    }

}
