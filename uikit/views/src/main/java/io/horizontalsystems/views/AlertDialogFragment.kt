package io.horizontalsystems.views

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.fragment_alert_dialog.*

class AlertDialogFragment(
        private var title: String? = null,
        private var description: String? = null,
        private var buttonText: Int,
        private var canCancel: Boolean,
        private var listener: Listener? = null)
    : DialogFragment() {

    interface Listener {
        fun onButtonClick()
    }

    override fun onCreateDialog(bundle: Bundle?): Dialog {
        val rootView = View.inflate(context, R.layout.fragment_alert_dialog, null) as ViewGroup

        txtTitle.text = title
        txtTitle.visibility = if (title == null) View.GONE else View.VISIBLE

        txtDescription.text = description

        buttonTextView.setText(buttonText)
        buttonTextView.setOnClickListener {
            listener?.onButtonClick()
            dismiss()
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
