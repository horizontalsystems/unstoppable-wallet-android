package io.horizontalsystems.views

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import io.horizontalsystems.views.databinding.FragmentKeyboardAlertDialogBinding


class AlertDialogKeyboardFragment(
    private var title: String? = null,
    private var description: String? = null,
    private var selectButtonText: Int,
    private var skipButtonText: Int,
    private var listener: Listener? = null
) : DialogFragment() {

    interface Listener {
        fun onButtonClick()
        fun onCancel()
        fun onSkipClick()
    }

    private var _binding: FragmentKeyboardAlertDialogBinding? = null
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateDialog(bundle: Bundle?): Dialog {
        _binding = FragmentKeyboardAlertDialogBinding.inflate(LayoutInflater.from(context))

        binding.txtTitle.text = title
        binding.txtTitle.isVisible = title != null

        binding.txtDescription.text = description

        binding.selectButtonTextView.setText(selectButtonText)
        binding.selectButtonTextView.setOnClickListener {
            listener?.onButtonClick()
            dismiss()
        }

        binding.skipButtonTextView.setText(skipButtonText)
        binding.skipButtonTextView.setOnClickListener {
            listener?.onSkipClick()
            dismiss()
        }

        val builder = activity?.let { AlertDialog.Builder(it, R.style.AlertDialog) }
        builder?.setView(binding.root)
        val mDialog = builder?.create()

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
            selectButtonText: Int,
            skipButtonText: Int,
            listener: Listener? = null
        ): AlertDialogKeyboardFragment {
            return AlertDialogKeyboardFragment(
                title = titleString,
                description = descriptionString,
                selectButtonText = selectButtonText,
                skipButtonText = skipButtonText,
                listener = listener
            )

        }
    }
}