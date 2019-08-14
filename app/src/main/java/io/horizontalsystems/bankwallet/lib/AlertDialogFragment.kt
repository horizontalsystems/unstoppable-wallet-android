package io.horizontalsystems.bankwallet.lib

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import io.horizontalsystems.bankwallet.R

class AlertDialogFragment(
        private var title: Int,
        private var description: Int,
        private var buttonText: Int,
        private var listener: Listener? = null
) : DialogFragment() {

    interface Listener {
        fun onButtonClick()
    }

    override fun onCreateDialog(bundle: Bundle?): Dialog {

        val builder = activity?.let { AlertDialog.Builder(it, R.style.AlertDialog) }
        val rootView = View.inflate(context, R.layout.fragment_alert_dialog, null) as ViewGroup

        builder?.setView(rootView)
        rootView.findViewById<TextView>(R.id.txtTitle)?.setText(title)
        rootView.findViewById<TextView>(R.id.txtDescription)?.setText(description)
        rootView.findViewById<TextView>(R.id.positiveButtonText)?.let { btn ->
            btn.setText(buttonText)
            btn.setOnClickListener {
                listener?.onButtonClick()
                dismiss()
            }
        }

        val mDialog = builder?.create()
        mDialog?.setCanceledOnTouchOutside(false)
        isCancelable = false

        return mDialog as Dialog
    }

    companion object {
        fun newInstance(title: Int, description: Int, buttonText: Int, listener: Listener? = null): AlertDialogFragment {
            return AlertDialogFragment(title, description, buttonText, listener)
        }
    }
}
