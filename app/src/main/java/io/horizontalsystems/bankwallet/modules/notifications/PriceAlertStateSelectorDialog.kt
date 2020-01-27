package io.horizontalsystems.bankwallet.modules.notifications

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.PriceAlert
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_item_selector.*

class PriceAlertStateSelectorDialog : DialogFragment() {

    interface Listener {
        fun onSelect(position: Int)
    }

    private var listener: Listener? = null
    private lateinit var items: List<PriceAlert.State>
    private lateinit var selectedState: PriceAlert.State

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.setBackgroundDrawableResource(R.drawable.alert_background_themed)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED)

        val view = inflater.inflate(R.layout.fragment_alert_dialog_single_select, container, false)
        view.findViewById<TextView>(R.id.dialogTitle).visibility = View.GONE

        val recyclerView = view.findViewById<RecyclerView>(R.id.dialogRecyclerView)
        recyclerView.adapter = PriceAlertAdapter(items, selectedState) { position ->
            listener?.onSelect(position)
            dismiss()
        }
        recyclerView.layoutManager = LinearLayoutManager(context)

        return view
    }

    companion object {
        fun newInstance(listener: Listener? = null, items: List<PriceAlert.State>, selectedState: PriceAlert.State): PriceAlertStateSelectorDialog {
            val dialog = PriceAlertStateSelectorDialog()
            dialog.listener = listener
            dialog.items = items
            dialog.selectedState = selectedState
            return dialog
        }
    }
}

class PriceAlertAdapter(
        private val list: List<PriceAlert.State>,
        private val selectedItem: PriceAlert.State,
        private val onClickCallback: ((Int) -> Unit)
) : RecyclerView.Adapter<PriceAlertStateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            PriceAlertStateViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_item_selector, parent, false), onClickCallback)

    override fun onBindViewHolder(holder: PriceAlertStateViewHolder, position: Int) {
        val item = list[position]
        holder.bind(item, item == selectedItem, position == 0)
    }

    override fun getItemCount() = list.size
}

class PriceAlertStateViewHolder(
        override val containerView: View,
        private val onClickCallback: ((Int) -> Unit)
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        containerView.setOnClickListener { onClickCallback(adapterPosition) }
    }

    fun bind(state: PriceAlert.State, isSelected: Boolean, firstItem: Boolean) {
        itemTitle.text = state.value?.let { "$it%" }
                ?: itemView.context.getString(R.string.SettingsNotifications_Off)
        itemTitle.isSelected = isSelected
        topDivider.visibility = if (firstItem) View.INVISIBLE else View.VISIBLE
    }
}
