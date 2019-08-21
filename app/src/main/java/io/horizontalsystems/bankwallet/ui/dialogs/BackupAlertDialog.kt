package io.horizontalsystems.bankwallet.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.viewHelpers.bottomDialog

class BackupAlertDialog(private val listener: Listener) : DialogFragment() {

    interface Listener {
        fun onBackupButtonClick() {}
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val rootView = View.inflate(context, R.layout.fragment_bottom_backup_alert, null) as ViewGroup

        rootView.findViewById<Button>(R.id.backupButton)?.apply {
            setOnClickListener {
                listener.onBackupButtonClick()
                dismiss()
            }
        }

        return bottomDialog(activity, rootView)
    }

    companion object {
        fun show(activity: FragmentActivity, listener: Listener) {
            val fragment = BackupAlertDialog(listener)
            val transaction = activity.supportFragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_backup_alert")
            transaction.commitAllowingStateLoss()
        }
    }
}
