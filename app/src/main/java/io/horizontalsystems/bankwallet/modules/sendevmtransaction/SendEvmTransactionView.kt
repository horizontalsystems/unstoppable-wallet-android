package io.horizontalsystems.bankwallet.modules.sendevmtransaction

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_title_value_hex.*
import kotlinx.android.synthetic.main.view_send_evm_transaction.view.*

class SendEvmTransactionView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.view_send_evm_transaction, this)
    }

    fun init(
            transactionViewModel: SendEvmTransactionViewModel,
            ethereumFeeViewModel: EthereumFeeViewModel,
            viewLifecycleOwner: LifecycleOwner,
            fragmentManager: FragmentManager,
            showSpeedInfoListener: () -> Unit
    ) {
        feeSelectorView.setFeeSelectorViewInteractions(
                ethereumFeeViewModel,
                ethereumFeeViewModel,
                viewLifecycleOwner,
                fragmentManager,
                showSpeedInfoListener
        )

        val adapter = SendEvmTransactionAdapter()
        recyclerView.adapter = adapter

        transactionViewModel.viewItemsLiveData.observe(viewLifecycleOwner, {
            adapter.items = flattenSectionViewItems(it)
            adapter.notifyDataSetChanged()
        })

        transactionViewModel.errorLiveData.observe(viewLifecycleOwner, {
            error.text = it
        })
    }

    private fun flattenSectionViewItems(sections: List<SectionViewItem>): List<ViewItemWithPosition> {
        val viewItems = mutableListOf<ViewItemWithPosition>()

        sections.forEach { section ->
            section.viewItems.forEachIndexed { index, viewItem ->
                viewItems.add(ViewItemWithPosition(viewItem, ListPosition.getListPosition(section.viewItems.size, index)))
            }
            viewItems.add(ViewItemWithPosition(null, ListPosition.First))
        }

        return viewItems
    }

}

data class ViewItemWithPosition(
        val viewItem: ViewItem?,
        val listPosition: ListPosition
)

class SendEvmTransactionAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var items: List<ViewItemWithPosition> = listOf()

    enum class ViewType {
        Subhead, Value, Address, Input, Space
    }

    private val viewTypes = ViewType.values()

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position].viewItem) {
            is ViewItem.Subhead -> ViewType.Subhead.ordinal
            is ViewItem.Address -> ViewType.Address.ordinal
            is ViewItem.Value -> ViewType.Value.ordinal
            is ViewItem.Input -> ViewType.Input.ordinal
            null -> ViewType.Space.ordinal
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewTypes[viewType]) {
            ViewType.Subhead -> SubheadViewHolder.create(parent)
            ViewType.Space -> SpaceViewHolder.create(parent)
            ViewType.Address -> TitleValueHexViewHolder.create(parent)
            ViewType.Value -> TitleValueViewHolder.create(parent)
            ViewType.Input -> TitleValueHexViewHolder.create(parent)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listPosition = items[position].listPosition
        when (val item = items[position].viewItem) {
            is ViewItem.Subhead -> (holder as? SubheadViewHolder)?.bind(item, listPosition)
            is ViewItem.Address -> (holder as? TitleValueHexViewHolder)?.bind(item.title, item.valueTitle, item.value, listPosition)
            is ViewItem.Value -> (holder as? TitleValueViewHolder)?.bind(item.title, item.value, item.type, listPosition)
            is ViewItem.Input -> (holder as? TitleValueHexViewHolder)?.bind("Input", item.value, item.value, listPosition)
        }
    }

    class SpaceViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        companion object {
            fun create(parent: ViewGroup) = SpaceViewHolder(inflate(parent, R.layout.view_send_evm_space, false))
        }
    }

    class SubheadViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(item: ViewItem.Subhead, position: ListPosition) {
            titleTextView.text = item.title
            valueTextView.text = item.value

            backgroundView.setBackgroundResource(position.getBackground())
        }

        companion object {
            fun create(parent: ViewGroup) = SubheadViewHolder(inflate(parent, R.layout.view_holder_evm_confirmation_subhead, false))
        }
    }

    class TitleValueHexViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(title: String, valueTitle: String, value: String, position: ListPosition) {
            titleTextView.text = title
            valueTextView.text = valueTitle

            backgroundView.setBackgroundResource(position.getBackground())

            valueTextView.setOnSingleClickListener {
                TextHelper.copyText(value)
                HudHelper.showSuccessMessage(containerView, R.string.Hud_Text_Copied)
            }
        }

        companion object {
            fun create(parent: ViewGroup) = TitleValueHexViewHolder(inflate(parent, R.layout.view_holder_title_value_hex, false))
        }
    }

    class TitleValueViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(title: String, value: String, type: ValueType, position: ListPosition) {
            titleTextView.text = title
            valueTextView.text = value

            val textColor = when (type) {
                ValueType.Regular -> R.color.bran
                ValueType.Disabled -> R.color.grey
                ValueType.Outgoing -> R.color.jacob
                ValueType.Incoming -> R.color.remus
            }
            valueTextView.setTextColor(containerView.context.getColor(textColor))

            backgroundView.setBackgroundResource(position.getBackground())
        }

        companion object {
            fun create(parent: ViewGroup) = TitleValueViewHolder(inflate(parent, R.layout.view_holder_title_value, false))
        }
    }
}
