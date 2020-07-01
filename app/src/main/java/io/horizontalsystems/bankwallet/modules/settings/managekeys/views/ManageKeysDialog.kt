package io.horizontalsystems.bankwallet.modules.settings.managekeys.views

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment

class ManageKeysDialog(
        private val listener: Listener,
        private val title: String,
        private val subtitle: String,
        private val content: String,
        private val action: ManageAction)
    : BaseBottomSheetDialogFragment() {

    interface Listener {
        fun onClickBackupKey() {}
    }

    private lateinit var alertText: TextView
    private lateinit var primaryActionButton: Button
    private lateinit var secondaryActionButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setContentView(R.layout.fragment_bottom_manage_keys)

        setTitle(title)
        setSubtitle(subtitle)
        val icon = if (action == ManageAction.CREATE) R.drawable.ic_manage_keys else R.drawable.ic_attention_red
        setHeaderIcon(icon)

        alertText = view.findViewById(R.id.alertText)
        primaryActionButton = view.findViewById(R.id.primaryActionBtn)
        secondaryActionButton = view.findViewById(R.id.secondaryActionBtn)

        alertText.text = content

        bindActions()
    }

    private fun bindActions() {
        secondaryActionButton.isVisible = false
        primaryActionButton.isVisible = true

        if (action == ManageAction.BACKUP) {
            primaryActionButton.text = getString(R.string.ManageKeys_Backup)
            primaryActionButton.setOnClickListener {
                listener.onClickBackupKey()
                dismiss()
            }
        }
    }

    enum class ManageAction {
        CREATE,
        BACKUP
    }

    companion object {
        fun show(title: String, subtitle: String, content: String, activity: FragmentActivity, listener: Listener, action: ManageAction) {
            val fragment = ManageKeysDialog(listener, title, subtitle, content, action)
            val transaction = activity.supportFragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_create_key_dialog")
            transaction.commitAllowingStateLoss()
        }
    }
}
