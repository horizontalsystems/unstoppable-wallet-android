package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.databinding.FragmentAlertDialogSingleSelectBinding
import io.horizontalsystems.bankwallet.databinding.ViewHolderItemSelectorBinding

class SelectorDialog : DialogFragment(), SelectorAdapter.Listener {

    private var onSelectItem: ((Int) -> Unit)? = null
    private var items = listOf<SelectorItem>()
    private var title: String? = null

    private var _binding: FragmentAlertDialogSingleSelectBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertDialogSingleSelectBinding.inflate(inflater, container, false)
        val view = binding.root

        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.setBackgroundDrawableResource(R.drawable.alert_background_themed)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED)

        binding.dialogRecyclerView.adapter = SelectorAdapter(items, this, title != null)

        binding.dialogTitle.isVisible = title != null
        binding.dialogTitle.text = title

        hideKeyBoard()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onClick(position: Int) {
        onSelectItem?.invoke(position)
        dismiss()
    }

    private fun hideKeyBoard() {
        (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(
            activity?.currentFocus?.windowToken,
            0
        )
    }

    companion object {
        fun newInstance(
            items: List<SelectorItem>,
            title: String? = null,
            onSelectItem: ((Int) -> Unit)? = null
        ): SelectorDialog {
            val dialog = SelectorDialog()
            dialog.onSelectItem = onSelectItem
            dialog.items = items
            dialog.title = title
            return dialog
        }
    }

}

class SelectorAdapter(
    private val list: List<SelectorItem>,
    private val listener: Listener,
    private val hasTitle: Boolean
) : RecyclerView.Adapter<SelectorOptionViewHolder>() {

    interface Listener {
        fun onClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        SelectorOptionViewHolder(
            ViewHolderItemSelectorBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ), listener
        )

    override fun onBindViewHolder(holder: SelectorOptionViewHolder, position: Int) {
        holder.bind(list[position], hasTitle || position > 0)
    }

    override fun getItemCount() = list.size
}

class SelectorOptionViewHolder(
    private val binding: ViewHolderItemSelectorBinding,
    private val listener: SelectorAdapter.Listener
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.wrapper.setOnClickListener { listener.onClick(bindingAdapterPosition) }
    }

    fun bind(item: SelectorItem, showTopDivider: Boolean) {
        binding.itemTitle.text = item.caption
        binding.itemTitle.isSelected = item.selected
        binding.topDivider.isVisible = showTopDivider
    }
}

data class SelectorItem(val caption: String, val selected: Boolean)
