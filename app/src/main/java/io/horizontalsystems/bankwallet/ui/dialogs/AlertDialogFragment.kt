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
        private var title: Int?,
        private var description: Int? = null,
        private var descriptionString: String? = null,
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
            title?.let { setText(it) }
            visibility = if (title == null) View.GONE else View.VISIBLE
        }
        rootView.findViewById<TextView>(R.id.txtDescription)?.text = description?.let { getString(it) } ?: descriptionString
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
        fun newInstance(title: Int? = null, description: Int, buttonText: Int, cancelable: Boolean = false, listener: Listener? = null): AlertDialogFragment {
            return AlertDialogFragment(
                    title = title,
                    description = description,
                    buttonText = buttonText,
                    canCancel = cancelable,
                    listener = listener)
        }

        fun newInstance(title: Int? = null, descriptionString: String?, buttonText: Int, cancelable: Boolean = false, listener: Listener? = null): AlertDialogFragment {
            return AlertDialogFragment(
                    title = title,
                    descriptionString = descriptionString,
                    buttonText = buttonText,
                    canCancel = cancelable,
                    listener = listener)
        }
    }
}
