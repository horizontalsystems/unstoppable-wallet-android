package io.horizontalsystems.bankwallet.modules.transactionInfo.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.databinding.ViewHolderSectionDividerBinding
import io.horizontalsystems.bankwallet.databinding.ViewHolderTransactionInfoExplorerBinding
import io.horizontalsystems.bankwallet.databinding.ViewHolderTransactionInfoItemBinding
import io.horizontalsystems.bankwallet.modules.transactionInfo.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoItemType.*
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.Ellipsis
import io.horizontalsystems.views.ListPosition
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
                ViewHolderTransactionInfoItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ),
                listener
            )
            viewTypeDivider -> DividerViewHolder(
                ViewHolderSectionDividerBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
            viewTypeExplorer -> ExplorerViewHolder(
                ViewHolderTransactionInfoExplorerBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
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

    class ExplorerViewHolder(
        private val binding: ViewHolderTransactionInfoExplorerBinding,
        private val listener: Listener
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(explorer: Explorer) {
            binding.txtTitle.text = explorer.title
            binding.wrapper.setOnClickListener { explorer.url?.let { listener.onUrlClick(it) } }
        }
    }

    class ItemViewHolder(
        private val binding: ViewHolderTransactionInfoItemBinding,
        private val listener: Listener
    ) :
        RecyclerView.ViewHolder(binding.root) {
        private val bodyTextSize = 16f
        private val subhead2TextSize = 14f
        private val greyColor = getColor(R.color.grey)
        private val ozColor = getColor(R.color.oz)

        fun bind(item: TransactionInfoViewItem) {
            setButtons(item)
            binding.transactionStatusView.isVisible = false
            binding.valueText.isVisible = false
            binding.statusInfoIcon.isVisible = false
            binding.rightInfoIcon.isVisible = false

            binding.txViewBackground.setBackgroundResource(item.listPosition.getBackground())

            when (val type = item.type) {
                is TransactionType -> {
                    binding.txtTitle.textSize = bodyTextSize
                    binding.txtTitle.setTextColor(ozColor)
                    binding.txtTitle.text = type.leftValue

                    binding.valueText.setTextColor(greyColor)
                    binding.valueText.text = type.rightValue
                    binding.valueText.isVisible = true
                }
                is Amount -> {
                    setDefaultStyle()

                    binding.txtTitle.text = type.leftValue
                    binding.valueText.text = type.rightValue.value
                    binding.valueText.setTextColor(getColor(type.rightValue.color))
                    binding.valueText.isVisible = true
                }
                is Value -> {
                    setDefaultStyle()

                    binding.txtTitle.text = type.title
                    binding.valueText.text = type.value
                    binding.valueText.isVisible = true
                }
                is Decorated -> {
                    setDefaultStyle()
                    binding.txtTitle.text = type.title
                }
                is Status -> {
                    setDefaultStyle()
                    binding.txtTitle.text = type.title
                    binding.statusInfoIcon.setImageResource(type.leftIcon)
                    binding.statusInfoIcon.isVisible =
                        type.status !is TransactionStatusViewItem.Completed
                    if (type.status !is TransactionStatusViewItem.Completed) {
                        binding.statusInfoIcon.setOnClickListener { listener.onClickStatusInfo() }
                    }
                    binding.transactionStatusView.isVisible = true
                    binding.transactionStatusView.bind(type.status)
                }
                is RawTransaction -> {
                    setDefaultStyle()
                    binding.txtTitle.text = type.title
                }
                is LockState -> {
                    setDefaultStyle()

                    binding.txtTitle.text = type.title
                    binding.statusInfoIcon.setImageResource(type.leftIcon)
                    binding.statusInfoIcon.isVisible = true

                    if (type.showLockInfo) {
                        binding.rightInfoIcon.isVisible = true
                        binding.wrapper.setOnClickListener {
                            listener.onLockInfoClick(type.date)
                        }
                    }
                }
                is DoubleSpend -> {
                    binding.txtTitle.text = type.title
                    binding.statusInfoIcon.setImageResource(type.leftIcon)
                    binding.statusInfoIcon.isVisible = true
                    binding.rightInfoIcon.isVisible = true
                    binding.wrapper.setOnClickListener {
                        listener.onDoubleSpendInfoClick(type.transactionHash, type.conflictingHash)
                    }
                }
                is Options -> {
                    binding.txtTitle.text = type.title
                }
            }
        }

        private fun setButtons(item: TransactionInfoViewItem) {
            binding.buttonsCompose.setContent {
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
            binding.txtTitle.textSize = subhead2TextSize
            binding.txtTitle.setTextColor(greyColor)
            binding.valueText.setTextColor(ozColor)
        }

        private fun getColor(colorRes: Int): Int {
            return binding.wrapper.context.getColor(colorRes)
        }
    }

    class DividerViewHolder(binding: ViewHolderSectionDividerBinding) :
        RecyclerView.ViewHolder(binding.root)
}


data class TransactionInfoViewItem(
    val type: TransactionInfoItemType,
    var listPosition: ListPosition = ListPosition.Middle
)
