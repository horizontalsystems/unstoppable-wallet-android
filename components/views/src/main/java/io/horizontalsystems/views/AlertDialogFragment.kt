package io.horizontalsystems.views

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import io.horizontalsystems.views.databinding.FragmentAlertDialogBinding

class AlertDialogFragment(
    private var title: String? = null,
    private var description: String? = null,
    private var buttonText: Int,
    private var cancelButtonText: Int? = null,
    private var canCancel: Boolean,
    private var listener: Listener? = null
) : DialogFragment() {

    interface Listener {
        fun onButtonClick()
        fun onCancel()
    }

    private var _binding: FragmentAlertDialogBinding? = null
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateDialog(bundle: Bundle?): Dialog {
        _binding = FragmentAlertDialogBinding.inflate(LayoutInflater.from(context))

        binding.txtTitle.text = title
        binding.txtTitle.isVisible = title != null

        binding.txtDescription.text = description

        binding.actionButtonTextView.setText(buttonText)
        binding.actionButtonTextView.setOnClickListener {
            listener?.onButtonClick()
            dismiss()
        }
        cancelButtonText?.let {
            binding.cancelButtonTextView.setText(it)
            binding.cancelButtonTextView.isVisible = true
            binding.cancelButtonTextView.setOnClickListener {
                listener?.onCancel()
                dismiss()
            }
        }

        val builder = activity?.let { AlertDialog.Builder(it, R.style.AlertDialog) }
        builder?.setView(binding.root)
        val mDialog = builder?.create()
        mDialog?.setCanceledOnTouchOutside(canCancel)

        return mDialog as Dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        listener?.onCancel()
        super.onDismiss(dialog)
    }

    companion object {

        fun newInstance(
            titleString: String? = null,
            descriptionString: String?,
            buttonText: Int,
            cancelButtonText: Int? = null,
            cancelable: Boolean = false,
            listener: Listener? = null
        ): AlertDialogFragment {
            return AlertDialogFragment(
                title = titleString,
                description = descriptionString,
                buttonText = buttonText,
                cancelButtonText = cancelButtonText,
                canCancel = cancelable,
                listener = listener
            )
        }
    }
}
