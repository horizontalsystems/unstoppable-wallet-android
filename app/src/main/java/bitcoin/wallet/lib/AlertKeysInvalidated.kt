package bitcoin.wallet.lib

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.util.Log
import bitcoin.wallet.R

class AlertKeysInvalidated : DialogFragment() {

    interface Listener {
        fun onDismiss()
    }

    var listener: Listener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val alertDialog = activity?.let { AlertDialog.Builder(it) }

        alertDialog?.setTitle("Android Keystore Error")
        alertDialog?.setMessage("Your encrypted data was recently invalidated because your Android lock screen was changed.")
        alertDialog?.setPositiveButton(R.string.alert_ok,{ _, _ -> dismiss()})

        return alertDialog?.create() ?: super.onCreateDialog(savedInstanceState)
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        listener?.onDismiss()
    }

    override fun show(manager: FragmentManager?, tag: String?) {
        try {
            super.show(manager, tag)
        } catch (e: IllegalStateException) {
            Log.e("AlertDialogFrag", "Exception", e)
        }
    }


}
