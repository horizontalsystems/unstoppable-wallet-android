package io.horizontalsystems.bankwallet.modules.transactionInfo.adapters

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.transactionInfo.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoItemType.*
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
        fun onAdditionalButtonClick(buttonType: TransactionInfoButtonType)
        fun closeClick()
        fun onClickStatusInfo()
        fun onLockInfoClick(lockDate: Date)
        fun onDoubleSpendInfoClick(transactionHash: String, conflictingHash: String)
        fun onOptionButtonClick(optionType: TransactionInfoOption.Type)
    }

    private var items = listOf<TransactionInfoViewItem?>()
    private val viewTypeItem = 0
    private val viewTypeDivider = 1
    private val viewTypeButton = 2

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
            items[position]?.type is Button -> viewTypeButton
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
            viewTypeButton -> ButtonViewHolder(
                inflate(
                    parent,
                    R.layout.view_holder_transaction_info_additional_button,
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
            is ButtonViewHolder -> (items[position]?.type as? Button)?.let { holder.bind(it) }
        }
    }

    class ButtonViewHolder(override val containerView: View, private val listener: Listener) :
        RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bind(button: Button) {
            containerView.findViewById<ImageView>(R.id.leftIcon)?.let {
                it.setImageResource(button.leftIcon)
            }
            containerView.findViewById<TextView>(R.id.txtTitle)?.let {
                it.text = button.title
            }
            containerView.setOnClickListener { listener.onAdditionalButtonClick(button.type) }
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
            btnAction.isVisible = false
            decoratedText.isVisible = false
            decoratedTextLeft.isVisible = false
            transactionStatusView.isVisible = false
            valueText.isVisible = false
            btnAction.isVisible = false
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
                    decoratedText.text = type.value
                    decoratedText.isVisible = true

                    type.actionButton?.let { actionButton ->
                        btnAction.setImageResource(actionButton.getIcon())
                        btnAction.isVisible = true
                        btnAction.setOnClickListener {
                            listener.onActionButtonClick(actionButton)
                        }
                    }

                    decoratedText.setOnClickListener {
                        listener.onAddressClick(type.value)
                    }
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

                    type.actionButton?.let { actionButton ->
                        btnAction.setImageResource(actionButton.getIcon())
                        btnAction.isVisible = true
                        btnAction.setOnClickListener {
                            listener.onActionButtonClick(actionButton)
                        }
                    }
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

                    decoratedTextLeft.text = type.optionButtonOne.title
                    decoratedTextLeft.isVisible = true
                    decoratedTextLeft.setOnClickListener {
                        listener.onOptionButtonClick(type.optionButtonOne.type)
                    }

                    decoratedText.text = type.optionButtonTwo.title
                    decoratedText.isVisible = true
                    decoratedText.setOnClickListener {
                        listener.onOptionButtonClick(type.optionButtonTwo.type)
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
