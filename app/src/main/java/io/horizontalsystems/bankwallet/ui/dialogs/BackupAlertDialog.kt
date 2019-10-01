package io.horizontalsystems.bankwallet.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.viewHelpers.bottomDialog

class BackupAlertDialog(private val listener: Listener) : DialogFragment() {
    private var walletName = ""
    private var coinName = ""

    interface Listener {
        fun onBackupButtonClick() {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.getString(walletNameKey)?.let { walletName = it }
        arguments?.getString(coinNameKey)?.let { coinName = it }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val rootView = View.inflate(context, R.layout.fragment_bottom_backup_alert, null) as ViewGroup

        rootView.findViewById<Button>(R.id.backupButton)?.apply {
            setOnClickListener {
                listener.onBackupButtonClick()
                dismiss()
            }
        }

        rootView.findViewById<TextView>(R.id.dialogText)?.let {
            it.text = getString(R.string.Receive_Alert_NotBackedUp_Description, walletName, coinName)
        }

        return bottomDialog(activity, rootView)
    }

    companion object {
        private const val walletNameKey = "walletNameKey"
        private const val coinNameKey = "coinNameKey"

        fun show(activity: FragmentActivity, walletName: String, coinName: String, listener: Listener) {
            val fragment = BackupAlertDialog(listener)

            val args = Bundle()
            args.putString(walletNameKey, walletName)
            args.putString(coinNameKey, coinName)

            fragment.arguments = args

            val transaction = activity.supportFragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_backup_alert")
            transaction.commitAllowingStateLoss()
        }
    }
}
