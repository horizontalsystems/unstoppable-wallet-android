package io.horizontalsystems.bankwallet.ui.extensions

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener

class ZcashBirthdayHeightDialog : DialogFragment() {

    lateinit var onEnter: ((String?) -> Unit)
    lateinit var onCancel: (() -> Unit)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val rootView = View.inflate(context, R.layout.fragment_dialog_zcash_bday_height, null)

        val input = rootView.findViewById<EditText>(R.id.input)

        rootView.findViewById<Button>(R.id.btnDone)?.let { btnDone ->
            btnDone.setOnSingleClickListener {
                onEnter(input.text.toString())
                dismiss()
            }
        }

        rootView.findViewById<Button>(R.id.btnCancel)?.let { btnCancel ->
            btnCancel.setOnSingleClickListener {
                onCancel()
                dismiss()
            }
        }

        val builder = activity?.let { AlertDialog.Builder(it, R.style.AlertDialog) }
        builder?.setView(rootView)
        val mDialog = builder?.create()
        mDialog?.setCanceledOnTouchOutside(false)

        return mDialog as Dialog

    }

    private fun hideKeyBoard() {
        (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
    }
}

