package io.horizontalsystems.views

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment

class AlertDialogFragment(
        private var title: String? = null,
        private var description: String? = null,
        private var buttonText: Int,
        private var cancelButtonText: Int? = null,
        private var canCancel: Boolean,
        private var listener: Listener? = null)
    : DialogFragment() {

    interface Listener {
        fun onButtonClick()
        fun onCancel()
    }

    override fun onCreateDialog(bundle: Bundle?): Dialog {
        val rootView = View.inflate(context, R.layout.fragment_alert_dialog, null) as ViewGroup

        rootView.findViewById<TextView>(R.id.txtTitle)?.apply {
            text = title
            isVisible = title != null
        }
        rootView.findViewById<TextView>(R.id.txtDescription)?.text = description
        rootView.findViewById<TextView>(R.id.actionButtonTextView)?.let { btn ->
            btn.setText(buttonText)
            btn.setOnClickListener {
                listener?.onButtonClick()
                dismiss()
            }
        }
        cancelButtonText?.let{
            rootView.findViewById<TextView>(R.id.cancelButtonTextView)?.let { btn ->
                btn.setText(it)
                btn.isVisible = true
                btn.setOnClickListener {
                    listener?.onCancel()
                    dismiss()
                }
            }
        }

        val builder = activity?.let { AlertDialog.Builder(it, R.style.AlertDialog) }
        builder?.setView(rootView)
        val mDialog = builder?.create()
        mDialog?.setCanceledOnTouchOutside(canCancel)

        return mDialog as Dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        listener?.onCancel()
        super.onDismiss(dialog)
    }

    companion object {

        fun newInstance(titleString: String? = null, descriptionString: String?, buttonText: Int, cancelButtonText: Int? = null, cancelable: Boolean = false, listener: Listener? = null): AlertDialogFragment {
            return AlertDialogFragment(
                    title = titleString,
                    description = descriptionString,
                    buttonText = buttonText,
                    cancelButtonText = cancelButtonText,
                    canCancel = cancelable,
                    listener = listener)
        }
    }
}
