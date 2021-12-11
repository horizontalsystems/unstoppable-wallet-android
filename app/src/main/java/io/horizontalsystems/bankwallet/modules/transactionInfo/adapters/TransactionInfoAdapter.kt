package io.horizontalsystems.bankwallet.modules.transactionInfo.adapters

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.transactionInfo.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoItemType.*
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.Ellipsis
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_page_section_header.*
import kotlinx.android.synthetic.main.view_holder_transaction_info_item.*
import java.util.*

class TransactionInfoAdapter(
    viewItems: MutableLiveData<List<TransactionInfoViewItem?>>,
    viewLifecycleOwner: LifecycleOwner,
    private val listener: Listener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onAddressClick(address: String)
        fun onActionButtonClick(actionButton: TransactionInfoActionButton)
        fun onUrlClick(url: String)
        fun closeClick()
        fun onClickStatusInfo()
        fun onLockInfoClick(lockDate: Date)
        fun onDoubleSpendInfoClick(transactionHash: String, conflictingHash: String)
        fun onOptionButtonClick(optionType: TransactionInfoOption.Type)
    }

    private var items = listOf<TransactionInfoViewItem?>()
    private val viewTypeItem = 0
    private val viewTypeDivider = 1
    private val viewTypeExplorer = 2

    init {
        viewItems.observe(viewLifecycleOwner) { list ->
            if (list.isEmpty()) {
                return@observe
            }

            items = list
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            items[position] == null -> viewTypeDivider
            items[position]?.type is Explorer -> viewTypeExplorer
            else -> viewTypeItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            viewTypeItem -> ItemViewHolder(
                inflate(
                    parent,
                    R.layout.view_holder_transaction_info_item,
                    false
                ),
                listener
            )
            viewTypeDivider -> DividerViewHolder(
                inflate(
                    parent,
                    R.layout.view_holder_section_divider,
                    false
                )
            )
            viewTypeExplorer -> ExplorerViewHolder(
                inflate(
                    parent,
                    R.layout.view_holder_transaction_info_explorer,
                    false
                ),
                listener
            )
            else -> throw IllegalArgumentException("No such view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ItemViewHolder -> items[position]?.let { holder.bind(it) }
            is ExplorerViewHolder -> (items[position]?.type as? Explorer)?.let { holder.bind(it) }
        }
    }

    class ExplorerViewHolder(override val containerView: View, private val listener: Listener) :
        RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bind(explorer: Explorer) {
            containerView.findViewById<TextView>(R.id.txtTitle)?.let {
                it.text = explorer.title
            }
            containerView.setOnClickListener { explorer.url?.let { listener.onUrlClick(it) } }
        }
    }

    class ItemViewHolder(override val containerView: View, private val listener: Listener) :
        RecyclerView.ViewHolder(containerView),
        LayoutContainer {
        private val bodyTextSize = 16f
        private val subhead2TextSize = 14f
        private val greyColor = getColor(R.color.grey)
        private val ozColor = getColor(R.color.oz)

        fun bind(item: TransactionInfoViewItem) {
            setButtons(item)
            transactionStatusView.isVisible = false
            valueText.isVisible = false
            statusInfoIcon.isVisible = false
            rightInfoIcon.isVisible = false

            txViewBackground.setBackgroundResource(item.listPosition.getBackground())

            when (val type = item.type) {
                is TransactionType -> {
                    txtTitle.textSize = bodyTextSize
                    txtTitle.setTextColor(ozColor)
                    txtTitle.text = type.leftValue

                    valueText.setTextColor(greyColor)
                    valueText.text = type.rightValue
                    valueText.isVisible = true
                }
                is Amount -> {
                    setDefaultStyle()

                    txtTitle.text = type.leftValue
                    valueText.text = type.rightValue.value
                    valueText.setTextColor(getColor(type.rightValue.color))
                    valueText.isVisible = true
                }
                is Value -> {
                    setDefaultStyle()

                    txtTitle.text = type.title
                    valueText.text = type.value
                    valueText.isVisible = true
                }
                is Decorated -> {
                    setDefaultStyle()
                    txtTitle.text = type.title
                }
                is Status -> {
                    setDefaultStyle()
                    txtTitle.text = type.title
                    statusInfoIcon.setImageResource(type.leftIcon)
                    statusInfoIcon.isVisible = type.status !is TransactionStatusViewItem.Completed
                    if (type.status !is TransactionStatusViewItem.Completed) {
                        statusInfoIcon.setOnClickListener { listener.onClickStatusInfo() }
                    }
                    transactionStatusView.isVisible = true
                    transactionStatusView.bind(type.status)
                }
                is RawTransaction -> {
                    setDefaultStyle()
                    txtTitle.text = type.title
                }
                is LockState -> {
                    setDefaultStyle()

                    txtTitle.text = type.title
                    statusInfoIcon.setImageResource(type.leftIcon)
                    statusInfoIcon.isVisible = true

                    if (type.showLockInfo) {
                        rightInfoIcon.isVisible = true
                        containerView.setOnClickListener {
                            listener.onLockInfoClick(type.date)
                        }
                    }
                }
                is DoubleSpend -> {
                    txtTitle.text = type.title
                    statusInfoIcon.setImageResource(type.leftIcon)
                    statusInfoIcon.isVisible = true
                    rightInfoIcon.isVisible = true
                    containerView.setOnClickListener {
                        listener.onDoubleSpendInfoClick(type.transactionHash, type.conflictingHash)
                    }
                }
                is Options -> {
                    txtTitle.text = type.title
                }
            }
        }

        private fun setButtons(item: TransactionInfoViewItem) {
            buttonsCompose.setContent {
                ComposeAppTheme {
                    Row(modifier = Modifier.padding(start = 16.dp)) {
                        if (item.type is Decorated) {
                            val endPadding = if (item.type.actionButton != null) 8.dp else 0.dp
                            ButtonSecondaryDefault(
                                modifier = Modifier.padding(end = endPadding),
                                title = item.type.value,
                                onClick = {
                                    listener.onAddressClick(item.type.value)
                                },
                                ellipsis = Ellipsis.Middle(if (item.type.actionButton != null) 5 else 10)
                            )
                            item.type.actionButton?.let { button ->
                                ButtonSecondaryCircle(
                                    icon = button.getIcon(),
                                    onClick = {
                                        listener.onActionButtonClick(button)
                                    }
                                )
                            }
                        }
                        if (item.type is RawTransaction) {
                            item.type.actionButton?.let { button ->
                                ButtonSecondaryCircle(
                                    icon = button.getIcon(),
                                    onClick = {
                                        listener.onActionButtonClick(button)
                                    }
                                )
                            }
                        }
                        if (item.type is Options) {
                            ButtonSecondaryDefault(
                                modifier = Modifier.padding(end = 8.dp),
                                title = item.type.optionButtonOne.title,
                                onClick = {
                                    listener.onOptionButtonClick(item.type.optionButtonOne.type)
                                },
                                ellipsis = Ellipsis.End
                            )
                            ButtonSecondaryDefault(
                                title = item.type.optionButtonTwo.title,
                                onClick = {
                                    listener.onOptionButtonClick(item.type.optionButtonTwo.type)
                                },
                                ellipsis = Ellipsis.End
                            )
                        }
                    }
                }
            }
        }

        private fun setDefaultStyle() {
            txtTitle.textSize = subhead2TextSize
            txtTitle.setTextColor(greyColor)
            valueText.setTextColor(ozColor)
        }

        private fun getColor(colorRes: Int): Int {
            return containerView.context.getColor(colorRes)
        }
    }

    class DividerViewHolder(override val containerView: View) :
        RecyclerView.ViewHolder(containerView), LayoutContainer
}


data class TransactionInfoViewItem(
    val type: TransactionInfoItemType,
    var listPosition: ListPosition = ListPosition.Middle
)
