package io.horizontalsystems.bankwallet.modules.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.*
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CardTabs
import io.horizontalsystems.bankwallet.ui.compose.components.ScrollableTabs
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_transactions.*
import kotlinx.android.synthetic.main.view_holder_transaction.*

class TransactionsFragment : BaseFragment(R.layout.fragment_transactions) {

    private val viewModel by navGraphViewModels<TransactionsViewModel>(R.id.mainFragment) { TransactionsModule.Factory() }

    private var scrollToTopAfterUpdate = false
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val transactionsAdapter = TransactionsAdapter(
            {
                viewModel.willShow(it)
            },
            {

                viewModel.getTransactionItem(it)?.let {
                    viewModel.tmpItemToShow = it

                    findNavController().navigate(
                        R.id.mainFragment_to_transactionInfoFragment,
                        null,
                        navOptionsFromBottom()
                    )
                }
            })

        val layoutManager = LinearLayoutManager(context)
        recyclerTransactions.adapter = ConcatAdapter(transactionsAdapter, LoadingAdapter())
        recyclerTransactions.layoutManager = layoutManager
        recyclerTransactions.itemAnimator = null
        recyclerTransactions.setHasFixedSize(true)

        recyclerTransactions.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
//                filterAdapter.filterChangeable = newState == RecyclerView.SCROLL_STATE_IDLE
//            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val pastVisibleItems = layoutManager.findFirstVisibleItemPosition()
                val diff = 15
                if (diff + pastVisibleItems + visibleItemCount >= totalItemCount) { //End of list
                    viewModel.onBottomReached()
                }
            }
        })

        val transactionSectionHeader = TransactionSectionHeader()
        recyclerTransactions.addItemDecoration(transactionSectionHeader.itemDecoration)

        viewModel.transactionList.observe(viewLifecycleOwner) { itemsList ->
            emptyListText.isVisible = itemsList is TransactionsViewModel.ItemsList.Blank
            recyclerTransactions.isVisible = itemsList is TransactionsViewModel.ItemsList.Filled

            transactionSectionHeader.setHeaders(itemsList.headers)
            transactionsAdapter.submitList(itemsList.items) {
                if (layoutManager.findFirstVisibleItemPosition() == 0 || scrollToTopAfterUpdate) {
                    recyclerTransactions.scrollToPosition(0)
                    scrollToTopAfterUpdate = false
                }
            }
        }

        viewModel.syncingLiveData.observe(viewLifecycleOwner) {
            toolbarSpinner.isVisible = it
        }

        transactionTypeFilterTabCompose.setContent {
            val filterTypes by viewModel.filterTypesLiveData.observeAsState()

            filterTypes?.let {
                val tabItems = it.map {
                    TabItem(it.item.name, it.selected, it.item)
                }

                ComposeAppTheme {
                    ScrollableTabs(tabItems) { index ->
                        viewModel.setFilterTransactionType(index)
                        scrollToTopAfterUpdate = true
                    }
                }
            }
        }

        transactionTypeFilterTabCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        transactionCoinFilterTabCompose.setContent {
            val filterCoins by viewModel.filterCoinsLiveData.observeAsState()

            filterCoins?.let {
                val tabItems = it.mapNotNull {
                    it.item.platformCoin?.let { platformCoin ->
                        TabItem(
                            platformCoin.code,
                            it.selected,
                            it.item,
                            ImageSource.Remote(platformCoin.coin.iconUrl, platformCoin.coinType.iconPlaceholder),
                            it.item.badge
                        )
                    }
                }

                CardTabs(
                    tabItems = tabItems,
                    edgePadding = 16.dp
                ) {
                    viewModel.setFilterCoin(it)
                    scrollToTopAfterUpdate = true
                }
            }
        }

        transactionCoinFilterTabCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )
    }
}

class LoadingAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(View(parent.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LayoutHelper.dp(60f, context)
            )
        })
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit

    override fun getItemCount() = 1

    class ViewHolder(containerView: View) : RecyclerView.ViewHolder(containerView)
}

data class Filter<T>(val item: T, val selected: Boolean)

class TransactionViewItemDiff : DiffUtil.ItemCallback<TransactionViewItem>() {

    override fun areItemsTheSame(oldItem: TransactionViewItem, newItem: TransactionViewItem): Boolean {
        return oldItem.itemTheSame(newItem)
    }

    override fun areContentsTheSame(oldItem: TransactionViewItem, newItem: TransactionViewItem): Boolean {
        return oldItem.contentTheSame(newItem)
    }

    override fun getChangePayload(oldItem: TransactionViewItem, newItem: TransactionViewItem): Any {
        return oldItem
    }

}

class TransactionsAdapter(private val onItemDisplay: (TransactionViewItem) -> Unit, private val onItemClick: (TransactionViewItem) -> Unit) : ListAdapter<TransactionViewItem, ViewHolderTransaction>(
    TransactionViewItemDiff()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderTransaction {
        return ViewHolderTransaction(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_transaction, parent, false), onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolderTransaction, position: Int) = Unit

    override fun onBindViewHolder(holder: ViewHolderTransaction, position: Int, payloads: MutableList<Any>) {

        val item = getItem(position)
        onItemDisplay(item)

        val prev = payloads.lastOrNull() as? TransactionViewItem

        if (prev == null) {
            holder.bind(item, showBottomShade = (position == itemCount - 1))
        } else {
            holder.bindUpdate(item, prev)
        }
    }
}

class ViewHolderTransaction(
    override val containerView: View,
    private val onItemClick: (TransactionViewItem) -> Unit
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private var item: TransactionViewItem? = null

    init {
        containerView.setOnSingleClickListener {
            item?.let {
                onItemClick(it)
            }
        }
    }

    fun bind(item: TransactionViewItem, showBottomShade: Boolean) {
        this.item = item

        txIcon.setImageResource(item.typeIcon)
        txTopText.text = item.title
        txBottomText.text = item.subtitle
        txPrimaryText.text = item.primaryValue?.value
        txSecondaryText.text = item.secondaryValue?.value

        item.primaryValue?.color?.let {
            txPrimaryText.setTextColor(getColor(it))
        }
        item.secondaryValue?.color?.let {
            txSecondaryText.setTextColor(getColor(it))
        }

        iconProgress.isVisible = item.progress != null
        item.progress?.let { progress ->
            iconProgress.setProgressColored(progress, getColor(R.color.grey_50), true)
        }

        doubleSpendIcon.isVisible = item.doubleSpend
        sentToSelfIcon.isVisible = item.sentToSelf
        bottomShade.isVisible = showBottomShade

        val imgRes = when (item.locked) {
            true -> R.drawable.ic_lock_20
            false -> R.drawable.ic_unlock_20
            null -> 0
        }

        lockIcon.isVisible = imgRes != 0
        lockIcon.setImageResource(imgRes)
    }

    fun bindUpdate(current: TransactionViewItem, prev: TransactionViewItem, ) {
        this.item = current

        if (current.typeIcon != prev.typeIcon) {
            txIcon.setImageResource(current.typeIcon)
        }

        if (current.title != prev.title) {
            txTopText.text = current.title
        }

        if (current.subtitle != prev.subtitle) {
            txBottomText.text = current.subtitle
        }

        if (current.primaryValue != prev.primaryValue) {
            txPrimaryText.text = current.primaryValue?.value
            current.primaryValue?.color?.let {
                txPrimaryText.setTextColor(getColor(it))
            }
        }

        if (current.secondaryValue != prev.secondaryValue) {
            txSecondaryText.text = current.secondaryValue?.value
            current.secondaryValue?.color?.let {
                txSecondaryText.setTextColor(getColor(it))
            }
        }

        if (current.progress != prev.progress) {
            iconProgress.isVisible = current.progress != null
            current.progress?.let { progress ->
                iconProgress.setProgressColored(progress, getColor(R.color.grey_50), true)
            }
        }

        if (current.doubleSpend != prev.doubleSpend) {
            doubleSpendIcon.isVisible = current.doubleSpend
        }

        if (current.sentToSelf != prev.sentToSelf) {
            sentToSelfIcon.isVisible = current.sentToSelf
        }

        if (current.locked != prev.locked) {
            val imgRes = when (current.locked) {
                true -> R.drawable.ic_lock_20
                false -> R.drawable.ic_unlock_20
                null -> 0
            }

            lockIcon.isVisible = imgRes != 0
            lockIcon.setImageResource(imgRes)
        }
    }

    private fun getColor(primaryValueTextColor: Int) =
        containerView.context.getColor(primaryValueTextColor)

}


