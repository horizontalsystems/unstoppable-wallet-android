package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.entities.FeeRateInfo
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeModule.FeeRateInfoViewItem
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_item_selector.*

class FeeRatePrioritySelector : DialogFragment(), FeeRatesAdapter.Listener {

    interface Listener {
        fun onSelectFeeRate(feeRate: FeeRateInfo)
    }

    private var listener: Listener? = null
    private var feeRates: List<FeeRateInfoViewItem>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.setBackgroundDrawableResource(R.drawable.alert_background_themed)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED)

        val view = inflater.inflate(R.layout.fragment_alert_dialog_single_select, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.dialogRecyclerView)
        val dialogTitle = view.findViewById<TextView>(R.id.dialogTitle)

        val feeRates = this.feeRates

        if (feeRates != null) {
            recyclerView.adapter = FeeRatesAdapter(feeRates, this)
        } else {
            dismiss()
        }

        recyclerView.layoutManager = LinearLayoutManager(context)

        dialogTitle.setText(R.string.Send_DialogSpeed)

        hideKeyBoard()

        return view
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        showKeyBoard()
    }

    //FeeRatesAdapter.Listener
    override fun onClick(feeRate: FeeRateInfo) {
        listener?.onSelectFeeRate(feeRate)
        dismiss()
    }

    private fun showKeyBoard() {
        (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.toggleSoftInput(0, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyBoard() {
        (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    companion object {
        fun newInstance(listener: Listener? = null, feeRates: List<FeeRateInfoViewItem>): FeeRatePrioritySelector {
            val dialog = FeeRatePrioritySelector()
            dialog.listener = listener
            dialog.feeRates = feeRates
            return dialog
        }
    }

}

class FeeRatesAdapter(private val list: List<FeeRateInfoViewItem>, private val listener: Listener)
    : RecyclerView.Adapter<ViewHolderFeeRatePriority>(), ViewHolderFeeRatePriority.ClickListener {

    interface Listener {
        fun onClick(feeRate: FeeRateInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderFeeRatePriority {
        return ViewHolderFeeRatePriority(inflate(parent, R.layout.view_holder_item_selector), this)
    }

    override fun onBindViewHolder(holder: ViewHolderFeeRatePriority, position: Int) {
        val item = list[position].feeRateInfo
        val isSelected = list[position].selected
        holder.bind(item.priority, item.duration, isSelected)
    }

    override fun getItemCount() = list.size

    override fun onClick(position: Int) {
        listener.onClick(list[position].feeRateInfo)
    }
}

class ViewHolderFeeRatePriority(override val containerView: View, private val listener: ClickListener)
    : RecyclerView.ViewHolder(containerView), LayoutContainer {

    interface ClickListener {
        fun onClick(position: Int)
    }

    init {
        containerView.setOnClickListener { listener.onClick(adapterPosition) }
    }

    fun bind(priority: FeeRatePriority, duration: Long?, isSelected: Boolean) {
        var priorityString = TextHelper.getFeeRatePriorityString(itemView.context, priority)
        if (duration != null) {
            priorityString += " (~${DateHelper.getTxDurationString(itemView.context, duration)})"
        }

        itemTitle.text = priorityString
        itemTitle.isSelected = isSelected
    }
}
