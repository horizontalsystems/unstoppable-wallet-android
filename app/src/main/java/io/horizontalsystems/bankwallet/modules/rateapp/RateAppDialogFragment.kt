package io.horizontalsystems.bankwallet.modules.rateapp

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import io.horizontalsystems.bankwallet.R

class RateAppDialogFragment(private var listener: Listener? = null) : DialogFragment() {

    interface Listener{
        fun onClickRateApp()
        fun onClickCancel() {}
        fun onDismiss() {}
    }

    override fun onCreateDialog(bundle: Bundle?): Dialog {
        val rootView = View.inflate(context, R.layout.fragment_rate_app_dialog, null) as ViewGroup

        rootView.findViewById<TextView>(R.id.btnRateApp)?.let { btn ->
            btn.setOnClickListener {
                listener?.onClickRateApp()
                dismiss()
            }
        }

        rootView.findViewById<TextView>(R.id.btnNotNow)?.let { btn ->
            btn.setOnClickListener {
                listener?.onClickCancel()
                dismiss()
            }
        }

        val builder = activity?.let { AlertDialog.Builder(it, R.style.AlertDialog) }
        builder?.setView(rootView)
        val mDialog = builder?.create()
        mDialog?.setCanceledOnTouchOutside(false)

        return mDialog as Dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        listener?.onDismiss()
        super.onDismiss(dialog)
    }

    companion object{

        fun show(activity: FragmentActivity, listener: Listener? = null){
            val fragmentManager: FragmentManager = activity.supportFragmentManager
            RateAppDialogFragment(listener).show(fragmentManager, "RateApp")
        }
    }
}
