package io.horizontalsystems.pin

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import io.horizontalsystems.pin.databinding.BiometricScannerDisabledDialogBinding

class BiometricScannerDisabledDialogFragment : DialogFragment() {

    private var _binding: BiometricScannerDisabledDialogBinding? = null
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateDialog(bundle: Bundle?): Dialog {
        _binding = BiometricScannerDisabledDialogBinding.inflate(LayoutInflater.from(context))

        binding.actionButtonTextView.setOnClickListener {
            dismiss()
        }

        val builder = activity?.let { AlertDialog.Builder(it, R.style.AlertDialog) }
        builder?.setView(binding.root)
        val mDialog = builder?.create()
        mDialog?.setCanceledOnTouchOutside(true)

        return mDialog as Dialog
    }

    companion object {
        fun newInstance(): BiometricScannerDisabledDialogFragment {
            return BiometricScannerDisabledDialogFragment()
        }
    }
}
