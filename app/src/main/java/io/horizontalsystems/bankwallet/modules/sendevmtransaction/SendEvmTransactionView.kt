package io.horizontalsystems.bankwallet.modules.sendevmtransaction

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.modules.transactionInfo.ColoredValue
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.Ellipsis
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportant
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_amount.*
import kotlinx.android.synthetic.main.view_holder_evm_confirmation_subhead.*
import kotlinx.android.synthetic.main.view_holder_title_value_hex.*
import kotlinx.android.synthetic.main.view_holder_title_value_hex.backgroundView
import kotlinx.android.synthetic.main.view_holder_title_value_hex.titleTextView
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
        Subhead, Value, Amount, Address, Input, Space, Warning
    }

    private val viewTypes = ViewType.values()

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position].viewItem) {
            is ViewItem.Subhead -> ViewType.Subhead.ordinal
            is ViewItem.Address -> ViewType.Address.ordinal
            is ViewItem.Value -> ViewType.Value.ordinal
            is ViewItem.Amount -> ViewType.Amount.ordinal
            is ViewItem.Input -> ViewType.Input.ordinal
            is ViewItem.Warning -> ViewType.Warning.ordinal
            null -> ViewType.Space.ordinal
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewTypes[viewType]) {
            ViewType.Subhead -> SubheadViewHolder.create(parent)
            ViewType.Space -> SpaceViewHolder.create(parent)
            ViewType.Address -> TitleValueHexViewHolder.create(parent)
            ViewType.Value -> TitleValueViewHolder.create(parent)
            ViewType.Amount -> AmountViewHolder.create(parent)
            ViewType.Input -> TitleValueHexViewHolder.create(parent)
            ViewType.Warning -> WarningViewHolder.create(parent)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is WarningViewHolder) {
            holder.composeView.disposeComposition()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listPosition = items[position].listPosition
        when (val item = items[position].viewItem) {
            is ViewItem.Subhead -> (holder as? SubheadViewHolder)?.bind(item, listPosition)
            is ViewItem.Address -> (holder as? TitleValueHexViewHolder)?.bind(item.title, item.valueTitle, item.value, listPosition)
            is ViewItem.Value -> (holder as? TitleValueViewHolder)?.bind(item, listPosition)
            is ViewItem.Amount -> (holder as? AmountViewHolder)?.bind(item.fiatAmount, item.coinAmount, listPosition)
            is ViewItem.Input -> (holder as? TitleValueHexViewHolder)?.bind("Input", item.value, item.value, listPosition)
            is ViewItem.Warning -> (holder as? WarningViewHolder)?.bind(item)
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

    class WarningViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        init {
            composeView.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
        }

        fun bind(item: ViewItem.Warning) {
            composeView.setContent {
                ComposeAppTheme {
                    TextImportant(text = item.description, title = item.title, icon = item.icon)
                }
            }
        }

        companion object {
            fun create(parent: ViewGroup) = WarningViewHolder(ComposeView(parent.context))
        }
    }

    class TitleValueHexViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(title: String, valueTitle: String, value: String, position: ListPosition) {
            titleTextView.text = title
            valueCompose.setContent {
                ComposeAppTheme {
                    ButtonSecondaryDefault(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        title = valueTitle,
                        onClick = {
                            TextHelper.copyText(value)
                            HudHelper.showSuccessMessage(containerView, R.string.Hud_Text_Copied)
                        },
                        ellipsis = Ellipsis.Middle(10)
                    )
                }
            }

            backgroundView.setBackgroundResource(position.getBackground())
        }

        companion object {
            fun create(parent: ViewGroup) = TitleValueHexViewHolder(inflate(parent, R.layout.view_holder_title_value_hex, false))
        }
    }

    class TitleValueViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(item: ViewItem.Value, position: ListPosition) {
            titleTextView.text = item.title
            valueTextView.text = item.value

            val textColor = when (item.type) {
                ValueType.Regular -> item.color ?: R.color.bran
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

    class AmountViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(fiatAmount: String?, coinAmount: ColoredValue, position: ListPosition) {
            fiatTextView.text = fiatAmount
            coinTextView.text = coinAmount.value

            coinTextView.setTextColor(containerView.context.getColor(coinAmount.color))

            backgroundView.setBackgroundResource(position.getBackground())
        }

        companion object {
            fun create(parent: ViewGroup) = AmountViewHolder(inflate(parent, R.layout.view_holder_amount, false))
        }
    }
}
