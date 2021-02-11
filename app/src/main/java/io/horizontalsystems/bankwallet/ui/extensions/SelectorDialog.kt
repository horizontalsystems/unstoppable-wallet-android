package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_item_selector.*

class SelectorDialog : DialogFragment(), SelectorAdapter.Listener {

    private var onSelectItem: ((Int) -> Unit)? = null
    private var items = listOf<SelectorItem>()
    private var title: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.setBackgroundDrawableResource(R.drawable.alert_background_themed)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED)

        val view = inflater.inflate(R.layout.fragment_alert_dialog_single_select, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.dialogRecyclerView)
        val dialogTitle = view.findViewById<TextView>(R.id.dialogTitle)

        recyclerView.adapter = SelectorAdapter(items, this, title != null)
        recyclerView.layoutManager = LinearLayoutManager(context)

        dialogTitle.isVisible = title != null
        dialogTitle.text = title

        hideKeyBoard()

        return view
    }

    override fun onClick(position: Int) {
        onSelectItem?.invoke(position)
        dismiss()
    }

    private fun hideKeyBoard() {
        (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
    }

    companion object {
        fun newInstance(items: List<SelectorItem>, title: String? = null, onSelectItem: ((Int) -> Unit)? = null): SelectorDialog {
            val dialog = SelectorDialog()
            dialog.onSelectItem = onSelectItem
            dialog.items = items
            dialog.title = title
            return dialog
        }
    }

}

class SelectorAdapter(private val list: List<SelectorItem>,
                      private val listener: Listener,
                      private val hasTitle: Boolean) : RecyclerView.Adapter<SelectorOptionViewHolder>() {

    interface Listener {
        fun onClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            SelectorOptionViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_item_selector, parent, false), listener)

    override fun onBindViewHolder(holder: SelectorOptionViewHolder, position: Int) {
        holder.bind(list[position], hasTitle || position > 0)
    }

    override fun getItemCount() = list.size
}

class SelectorOptionViewHolder(override val containerView: View, private val listener: SelectorAdapter.Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        containerView.setOnClickListener { listener.onClick(bindingAdapterPosition) }
    }

    fun bind(item: SelectorItem, showTopDivider: Boolean) {
        itemTitle.text = item.caption
        itemTitle.isSelected = item.selected
        topDivider.isVisible = showTopDivider
    }
}

data class SelectorItem(val caption: String, val selected: Boolean)
