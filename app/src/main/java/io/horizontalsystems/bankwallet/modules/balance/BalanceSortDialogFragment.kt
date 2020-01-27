package io.horizontalsystems.bankwallet.modules.balance

import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_item_selector.*


class BalanceSortDialogFragment : DialogFragment(), SortingAdapter.Listener {

    interface Listener {
        fun onSortItemSelect(sortType: BalanceSortType)
    }

    override fun onClick(sortType: BalanceSortType) {
        listener?.onSortItemSelect(sortType)
        dismiss()
    }

    private val sortTypes = listOf(BalanceSortType.Name, BalanceSortType.Value, BalanceSortType.PercentGrowth)
    private var listener: Listener? = null
    private var selectedSortingType: BalanceSortType? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.setBackgroundDrawableResource(R.drawable.alert_background_themed)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED)

        val view = inflater.inflate(R.layout.fragment_alert_dialog_single_select, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.dialogRecyclerView)

        selectedSortingType?.let {
            recyclerView.adapter = SortingAdapter(sortTypes, it, this)
        } ?: run { dismiss() }

        recyclerView.layoutManager = LinearLayoutManager(context)

        return view
    }

    companion object {
        fun newInstance(listener: Listener? = null, selectedSortType: BalanceSortType): BalanceSortDialogFragment {
            val dialog = BalanceSortDialogFragment()
            dialog.listener = listener
            dialog.selectedSortingType = selectedSortType
            return dialog
        }
    }
}


class SortingAdapter(
        private val list: List<BalanceSortType>,
        private val currentSortingType: BalanceSortType,
        private val listener: Listener
) : RecyclerView.Adapter<ViewHolderSortType>(), ViewHolderSortType.ClickListener {

    interface Listener {
        fun onClick(sortType: BalanceSortType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolderSortType(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_item_selector, parent, false), this)


    override fun onBindViewHolder(holder: ViewHolderSortType, position: Int) {
        holder.bind(list[position].getTitleRes(), list[position] == currentSortingType)
    }

    override fun getItemCount() = list.size

    override fun onClick(position: Int) {
        listener.onClick(list[position])
    }
}

class ViewHolderSortType(override val containerView: View, private val listener: ClickListener) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    interface ClickListener {
        fun onClick(position: Int)
    }

    init {
        containerView.setOnClickListener { listener.onClick(adapterPosition) }
    }

    fun bind(titleRes: Int, selected: Boolean) {
        itemTitle.text = containerView.context.getString(titleRes)
        itemTitle.isSelected = selected
    }
}
