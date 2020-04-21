package io.horizontalsystems.pin

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class BiometricScannerDisabledDialogFragment : DialogFragment() {

    override fun onCreateDialog(bundle: Bundle?): Dialog {
        val rootView = View.inflate(context, R.layout.biometric_scanner_disabled_dialog, null) as ViewGroup

        rootView.findViewById<TextView>(R.id.actionButtonTextView)?.let { btn ->
            btn.setOnClickListener {
                dismiss()
            }
        }

        val builder = activity?.let { AlertDialog.Builder(it, R.style.AlertDialog) }
        builder?.setView(rootView)
        val mDialog = builder?.create()
        mDialog?.setCanceledOnTouchOutside(true)

        return mDialog as Dialog
    }

    companion object {
        fun newInstance(): BiometricScannerDisabledDialogFragment {
            return BiometricScannerDisabledDialogFragment()
        }
    }
}
