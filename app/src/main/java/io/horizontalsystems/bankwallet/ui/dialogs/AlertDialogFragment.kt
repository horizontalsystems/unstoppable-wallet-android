package io.horizontalsystems.bankwallet.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import io.horizontalsystems.bankwallet.R

class AlertDialogFragment(
        private var title: String? = null,
        private var description: String? = null,
        private var buttonText: Int,
        private var canCancel: Boolean,
        private var listener: Listener? = null
) : DialogFragment() {

    interface Listener {
        fun onButtonClick()
    }

    override fun onCreateDialog(bundle: Bundle?): Dialog {
        val rootView = View.inflate(context, R.layout.fragment_alert_dialog, null) as ViewGroup

        rootView.findViewById<TextView>(R.id.txtTitle)?.apply {
            text = title
            visibility = if (title == null) View.GONE else View.VISIBLE
        }
        rootView.findViewById<TextView>(R.id.txtDescription)?.text = description
        rootView.findViewById<TextView>(R.id.buttonTextView)?.let { btn ->
            btn.setText(buttonText)
            btn.setOnClickListener {
                listener?.onButtonClick()
                dismiss()
            }
        }

        val builder = activity?.let { AlertDialog.Builder(it, R.style.AlertDialog) }
        builder?.setView(rootView)
        val mDialog = builder?.create()
        mDialog?.setCanceledOnTouchOutside(canCancel)

        return mDialog as Dialog
    }

    companion object {

        fun newInstance(titleString: String? = null, descriptionString: String?, buttonText: Int, cancelable: Boolean = false, listener: Listener? = null): AlertDialogFragment {
            return AlertDialogFragment(
                    title = titleString,
                    description = descriptionString,
                    buttonText = buttonText,
                    canCancel = cancelable,
                    listener = listener)
        }
    }
}
