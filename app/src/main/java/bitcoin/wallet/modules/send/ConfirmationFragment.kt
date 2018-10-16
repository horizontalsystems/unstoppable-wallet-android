package bitcoin.wallet.modules.send

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import bitcoin.wallet.R

class ConfirmationFragment : DialogFragment() {

    interface Listener {
        fun onButtonClick()
    }

    private var amountInCrypto: String = ""
    private var amountInFiat: String = ""

    private var listener: Listener? = null

    override fun onCreateDialog(bundle: Bundle?): Dialog {

        val builder = activity?.let { AlertDialog.Builder(it, R.style.AlertDialog) }

        val rootView = View.inflate(context, R.layout.fragment_confirmation, null) as ViewGroup
        builder?.setView(rootView)

        rootView.findViewById<TextView>(R.id.txtAmountInCrypto)?.text = amountInCrypto
        rootView.findViewById<TextView>(R.id.txtAmountInFiat)?.text = amountInFiat

        rootView.findViewById<TextView>(R.id.txtButtonConfirm)?.let { txtButton ->
            txtButton.setOnClickListener {
                listener?.onButtonClick()
                dismiss()
            }
        }

        rootView.findViewById<TextView>(R.id.txtButtonCancel)?.let { txtButton ->
            txtButton.setOnClickListener {
                dismiss()
            }
        }

        val mDialog = builder?.create()
        mDialog?.setCanceledOnTouchOutside(false)

        return mDialog as Dialog
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)

        listener?.onButtonClick()
    }


    companion object {
        fun newInstance(amountInCrypto: String, amountInFiat: String, listener: Listener? = null): ConfirmationFragment {
            val dialog = ConfirmationFragment()
            dialog.amountInCrypto = amountInCrypto
            dialog.amountInFiat = amountInFiat
            dialog.listener = listener
            return dialog
        }
    }

}
