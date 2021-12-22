package io.horizontalsystems.bankwallet.modules.rateapp

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.databinding.FragmentRateAppDialogBinding

class RateAppDialogFragment(private var listener: Listener? = null) : DialogFragment() {

    interface Listener {
        fun onClickRateApp()
        fun onClickCancel() {}
        fun onDismiss() {}
    }

    private var _binding: FragmentRateAppDialogBinding? = null
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateDialog(bundle: Bundle?): Dialog {
        _binding = FragmentRateAppDialogBinding.inflate(LayoutInflater.from(context))

        binding.btnRateApp.setOnClickListener {
            listener?.onClickRateApp()
            dismiss()
        }

        binding.btnNotNow.setOnClickListener {
            listener?.onClickCancel()
            dismiss()
        }

        val builder = activity?.let { AlertDialog.Builder(it, R.style.AlertDialog) }
        builder?.setView(binding.root)
        val mDialog = builder?.create()
        mDialog?.setCanceledOnTouchOutside(false)

        return mDialog as Dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        listener?.onDismiss()
        super.onDismiss(dialog)
    }

    companion object {

        fun show(activity: FragmentActivity, listener: Listener? = null) {
            val fragmentManager: FragmentManager = activity.supportFragmentManager
            RateAppDialogFragment(listener).show(fragmentManager, "RateApp")
        }
    }
}
