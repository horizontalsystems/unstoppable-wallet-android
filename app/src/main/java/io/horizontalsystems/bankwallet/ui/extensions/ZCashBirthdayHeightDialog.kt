package io.horizontalsystems.bankwallet.ui.extensions

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.databinding.FragmentDialogZcashBdayHeightBinding

class ZcashBirthdayHeightDialog : DialogFragment() {

    lateinit var onEnter: ((String?) -> Unit)
    lateinit var onCancel: (() -> Unit)

    private var _binding: FragmentDialogZcashBdayHeightBinding? = null
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentDialogZcashBdayHeightBinding.inflate(LayoutInflater.from(context))

        binding.btnDone.setOnSingleClickListener {
            onEnter(binding.input.text.toString())
            dismiss()
        }

        binding.btnCancel.setOnSingleClickListener {
            onCancel()
            dismiss()
        }

        val builder = activity?.let { AlertDialog.Builder(it, R.style.AlertDialog) }
        builder?.setView(binding.root)
        val mDialog = builder?.create()
        mDialog?.setCanceledOnTouchOutside(false)

        return mDialog as Dialog

    }

    private fun hideKeyBoard() {
        (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(
            activity?.currentFocus?.windowToken,
            0
        )
    }
}

