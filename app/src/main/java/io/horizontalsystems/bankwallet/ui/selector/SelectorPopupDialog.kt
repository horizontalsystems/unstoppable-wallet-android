package io.horizontalsystems.bankwallet.ui.selector

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.databinding.FragmentAlertDialogSingleSelectBinding

class SelectorPopupDialog<ItemClass> : DialogFragment() {

    var items: List<ItemClass>? = null
    var selectedItem: ItemClass? = null
    var onSelectListener: ((ItemClass) -> Unit)? = null

    var titleText: String = ""

    lateinit var itemViewHolderFactory: ItemViewHolderFactory<ItemViewHolder<ItemClass>>

    private var _binding: FragmentAlertDialogSingleSelectBinding? = null
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.setBackgroundDrawableResource(R.drawable.alert_background_themed)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED)

        _binding = FragmentAlertDialogSingleSelectBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.dialogTitle.isVisible = titleText.isNotBlank()
        binding.dialogTitle.text = titleText

        items?.let {
            val itemsAdapter = SelectorAdapter(it, selectedItem, itemViewHolderFactory, {
                onSelectListener?.invoke(it)
                dismiss()
            })

            binding.dialogRecyclerView.adapter = itemsAdapter
        }

        hideKeyBoard()

        return view
    }

    private fun hideKeyBoard() {
        (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
    }
}
