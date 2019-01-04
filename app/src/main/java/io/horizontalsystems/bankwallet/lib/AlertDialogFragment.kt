package io.horizontalsystems.bankwallet.lib

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.horizontalsystems.bankwallet.R

class AlertDialogFragment : DialogFragment() {

    interface Listener {
        fun onButtonClick()
    }

    private var title: Int? = null
    private var description: Int? = null
    private var buttonText: Int? = null

    private var listener: Listener? = null

    override fun onCreateDialog(bundle: Bundle?): Dialog {

        val builder = activity?.let { AlertDialog.Builder(it, R.style.AlertDialog) }

        val rootView = View.inflate(context, R.layout.fragment_alert_dialog, null) as ViewGroup
        builder?.setView(rootView)

        title?.let { rootView.findViewById<TextView>(R.id.txtTitle)?.setText(it) }
        description?.let { rootView.findViewById<TextView>(R.id.txtDescription)?.setText(it) }
        buttonText?.let { rootView.findViewById<TextView>(R.id.txtButton)?.setText(it) }

        rootView.findViewById<TextView>(R.id.txtButton)?.let { txtButton ->
            buttonText?.let { txtButton.setText(it) }
            txtButton.setOnClickListener {
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
            val dialog = AlertDialogFragment()
            dialog.title = title
            dialog.description = description
            dialog.buttonText = buttonText
            dialog.listener = listener
            return dialog
        }
    }

}
