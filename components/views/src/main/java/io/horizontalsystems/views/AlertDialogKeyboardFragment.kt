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


class AlertDialogKeyboardFragment (
        private var title: String? = null,
        private var description: String? = null,
        private var selectButtonText: Int,
        private var skipButtonText: Int,
        private var listener: Listener? = null)
    : DialogFragment() {

    interface Listener {
        fun onButtonClick()
        fun onCancel()
        fun onSkipClick()
    }

    override fun onCreateDialog(bundle: Bundle?): Dialog {
        val rootView = View.inflate(context, R.layout.fragment_keyboard_alert_dialog, null) as ViewGroup

        rootView.findViewById<TextView>(R.id.txtTitle)?.apply {
            text = title
            isVisible = title != null
        }
        rootView.findViewById<TextView>(R.id.txtDescription)?.text = description
        rootView.findViewById<TextView>(R.id.selectButtonTextView)?.let { btn ->
            btn.setText(selectButtonText)
            btn.setOnClickListener {
                listener?.onButtonClick()
                dismiss()
            }
        }

        rootView.findViewById<TextView>(R.id.skipButtonTextView)?.let { btn ->
            btn.setText(skipButtonText)
            btn.setOnClickListener {
                listener?.onSkipClick()
                dismiss()
            }
        }

        val builder = activity?.let { AlertDialog.Builder(it, R.style.AlertDialog) }
        builder?.setView(rootView)
        val mDialog = builder?.create()

        return mDialog as Dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        listener?.onCancel()
        super.onDismiss(dialog)
    }

    companion object {

        fun newInstance(titleString: String? = null, descriptionString: String?, selectButtonText: Int, skipButtonText: Int, listener: Listener? = null): AlertDialogKeyboardFragment {
            return AlertDialogKeyboardFragment(
                    title = titleString,
                    description = descriptionString,
                    selectButtonText = selectButtonText,
                    skipButtonText = skipButtonText,
                    listener = listener)

        }
    }
}