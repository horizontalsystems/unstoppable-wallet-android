package io.horizontalsystems.bankwallet.ui.selector

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R

class SelectorPopupDialog<ItemClass> : DialogFragment() {

    var items: List<ItemClass>? = null
    var selectedItem: ItemClass? = null
    var onSelectListener: ((ItemClass) -> Unit)? = null

    var titleText: String = ""

    lateinit var itemViewHolderFactory: ItemViewHolderFactory<ItemViewHolder<ItemClass>>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.setBackgroundDrawableResource(R.drawable.alert_background_themed)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED)

        val view = inflater.inflate(R.layout.fragment_alert_dialog_single_select, container, false)

        val dialogTitle = view.findViewById<TextView>(R.id.dialogTitle)

        dialogTitle.isVisible = titleText.isNotBlank()
        dialogTitle.text = titleText

        items?.let {
            val itemsAdapter = SelectorAdapter(it, selectedItem, itemViewHolderFactory, {
                onSelectListener?.invoke(it)
                dismiss()
            })

            val recyclerView = view.findViewById<RecyclerView>(R.id.dialogRecyclerView)
            recyclerView.adapter = itemsAdapter
        }

        hideKeyBoard()

        return view
    }

    private fun hideKeyBoard() {
        (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
    }
}
