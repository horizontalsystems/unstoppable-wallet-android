package io.horizontalsystems.bankwallet.modules.sendevmtransaction

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.setRemoteImage
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.databinding.*
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCell
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeSettingsFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.Ellipsis
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.views.helpers.LayoutHelper

class SendEvmTransactionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

    private val binding = ViewSendEvmTransactionBinding.inflate(LayoutInflater.from(context), this)

    fun init(
        transactionViewModel: SendEvmTransactionViewModel,
        feeCellViewModel: EvmFeeCellViewModel,
        viewLifecycleOwner: LifecycleOwner,
        navController: NavController,
        parentNavGraphId: Int,
        description: String? = null
    ) {
        binding.description.isVisible = description != null
        binding.description.text = description

        val adapter = SendEvmTransactionAdapter()
        binding.recyclerView.adapter = adapter

        transactionViewModel.viewItemsLiveData.observe(viewLifecycleOwner) {
            adapter.items = flattenSectionViewItems(it)
            adapter.notifyDataSetChanged()
        }

        binding.feeViewCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        binding.feeViewCompose.setContent {
            ComposeAppTheme {
                val fee by feeCellViewModel.feeLiveData.observeAsState("")
                val viewState by feeCellViewModel.viewStateLiveData.observeAsState()
                val loading by feeCellViewModel.loadingLiveData.observeAsState(false)

                EvmFeeCell(
                    title = stringResource(R.string.FeeSettings_Fee),
                    value = fee,
                    loading = loading,
                    highlightEditButton = feeCellViewModel.highlightEditButton,
                    viewState = viewState
                ) {
                    navController.slideFromBottom(
                        resId = R.id.sendEvmFeeSettingsFragment,
                        args = EvmFeeSettingsFragment.prepareParams(parentNavGraphId)
                    )
                }
            }
        }

        binding.cautionsViewCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        binding.cautionsViewCompose.setContent {
            ComposeAppTheme {
                val cautions by transactionViewModel.cautionsLiveData.observeAsState()
                cautions?.let {
                    Cautions(it)
                }
            }
        }
    }

    private fun flattenSectionViewItems(sections: List<SectionViewItem>): List<ViewItemWithPosition> {
        val viewItems = mutableListOf<ViewItemWithPosition>()

        sections.forEach { section ->
            section.viewItems.forEachIndexed { index, viewItem ->
                viewItems.add(
                    ViewItemWithPosition(
                        viewItem,
                        ListPosition.getListPosition(section.viewItems.size, index)
                    )
                )
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
        Subhead, Value, AmountMulti, Amount, Address, Input, Space, Warning
    }

    private val viewTypes = ViewType.values()

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position].viewItem) {
            is ViewItem.Subhead -> ViewType.Subhead.ordinal
            is ViewItem.Address -> ViewType.Address.ordinal
            is ViewItem.Value -> ViewType.Value.ordinal
            is ViewItem.AmountMulti -> ViewType.AmountMulti.ordinal
            is ViewItem.Amount -> ViewType.Amount.ordinal
            is ViewItem.Input -> ViewType.Input.ordinal
            is ViewItem.Warning -> ViewType.Warning.ordinal
            null -> ViewType.Space.ordinal
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewTypes[viewType]) {
            ViewType.Subhead -> SubheadViewHolder(
                ViewHolderEvmConfirmationSubheadBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            ViewType.Space -> SpaceViewHolder(
                ViewSendEvmSpaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            ViewType.Address -> TitleValueHexViewHolder(
                ViewHolderTitleValueHexBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            ViewType.Value -> TitleValueViewHolder(
                ViewHolderTitleValueBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            ViewType.AmountMulti -> AmountMultiViewHolder(
                ViewHolderAmountMultiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            ViewType.Amount -> AmountViewHolder(
                ViewHolderAmountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            ViewType.Input -> TitleValueHexViewHolder(
                ViewHolderTitleValueHexBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
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
            is ViewItem.Address -> (holder as? TitleValueHexViewHolder)?.bind(
                item.title,
                item.valueTitle,
                item.value,
                listPosition
            )
            is ViewItem.Value -> (holder as? TitleValueViewHolder)?.bind(item, listPosition)
            is ViewItem.AmountMulti -> (holder as? AmountMultiViewHolder)?.bind(
                item.amounts,
                item.type,
                item.platformCoin,
                listPosition
            )
            is ViewItem.Amount -> (holder as? AmountViewHolder)?.bind(
                item.fiatAmount,
                item.coinAmount,
                item.type,
                item.platformCoin,
                listPosition
            )
            is ViewItem.Input -> (holder as? TitleValueHexViewHolder)?.bind(
                "Input",
                item.value,
                item.value,
                listPosition
            )
            is ViewItem.Warning -> (holder as? WarningViewHolder)?.bind(item)
        }
    }

    class SpaceViewHolder(private val binding: ViewSendEvmSpaceBinding) :
        RecyclerView.ViewHolder(binding.root) {
    }

    class SubheadViewHolder(private val binding: ViewHolderEvmConfirmationSubheadBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ViewItem.Subhead, position: ListPosition) {
            binding.titleTextView.text = item.title
            binding.valueTextView.text = item.value
            item.iconRes?.let { binding.icon.setImageResource(it) }
            binding.icon.isVisible = item.iconRes != null

            binding.backgroundView.setBackgroundResource(position.getBackground())
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
                    TextImportantWarning(text = item.description, title = item.title, icon = item.icon)
                }
            }
        }

        companion object {
            fun create(parent: ViewGroup) = WarningViewHolder(ComposeView(parent.context))
        }
    }

    class TitleValueHexViewHolder(private val binding: ViewHolderTitleValueHexBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String, valueTitle: String, value: String, position: ListPosition) {
            binding.titleTextView.text = title
            binding.valueCompose.setContent {
                ComposeAppTheme {
                    ButtonSecondaryDefault(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        title = valueTitle,
                        onClick = {
                            TextHelper.copyText(value)
                            HudHelper.showSuccessMessage(binding.wrapper, R.string.Hud_Text_Copied)
                        },
                        ellipsis = Ellipsis.Middle(10)
                    )
                }
            }

            binding.backgroundView.setBackgroundResource(position.getBackground())
        }

    }

    class TitleValueViewHolder(private val binding: ViewHolderTitleValueBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ViewItem.Value, position: ListPosition) {
            binding.titleTextView.text = item.title
            binding.valueTextView.text = item.value

            binding.valueTextView.setTextColor(binding.wrapper.context.getColor(getTypeColor(item.type)))

            binding.backgroundView.setBackgroundResource(position.getBackground())
        }
    }

    class AmountMultiViewHolder(private val binding: ViewHolderAmountMultiBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(amounts: List<AmountValues>, type: ValueType, platformCoin: PlatformCoin, position: ListPosition) {
            binding.wrapper.layoutParams.height = LayoutHelper.dp(if (amounts.size == 2) 60f else 48f, binding.wrapper.context)
            binding.coinIcon.setRemoteImage(platformCoin.coin.iconUrl, platformCoin.coinType.iconPlaceholder)

            binding.coinTextView.text = amounts[0].coinAmount
            binding.fiatTextView.text = amounts[0].fiatAmount

            if (amounts.size == 2) {
                binding.coinSecondaryTextView.text = amounts[1].coinAmount
                binding.fiatSecondaryTextView.text = amounts[1].fiatAmount
            }

            binding.coinSecondaryTextView.isVisible = amounts.size == 2
            binding.fiatSecondaryTextView.isVisible = amounts.size == 2

            binding.coinTextView.setTextColor(binding.wrapper.context.getColor(getTypeColor(type)))

            binding.backgroundView.setBackgroundResource(position.getBackground())
        }

    }

    class AmountViewHolder(private val binding: ViewHolderAmountBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(fiatAmount: String?, coinAmount: String, type: ValueType, platformCoin: PlatformCoin, position: ListPosition) {
            binding.coinIcon.setRemoteImage(platformCoin.coin.iconUrl, platformCoin.coinType.iconPlaceholder)
            binding.fiatTextView.text = fiatAmount
            binding.coinTextView.text = coinAmount

            binding.coinTextView.setTextColor(binding.wrapper.context.getColor(getTypeColor(type)))

            binding.backgroundView.setBackgroundResource(position.getBackground())
        }

    }

    companion object{
        private fun getTypeColor(type: ValueType): Int = when (type) {
            ValueType.Regular -> R.color.bran
            ValueType.Disabled -> R.color.grey
            ValueType.Outgoing -> R.color.leah
            ValueType.Incoming -> R.color.remus
        }
    }
}
