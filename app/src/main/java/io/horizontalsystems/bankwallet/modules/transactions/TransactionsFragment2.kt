package io.horizontalsystems.bankwallet.modules.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.core.findNavController
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_transactions.*
import kotlinx.android.synthetic.main.view_holder_transaction.*

class TransactionsFragment2 : BaseFragment(R.layout.fragment_transactions) {

    private val viewModel by navGraphViewModels<Transactions2ViewModel>(R.id.mainFragment) { Transactions2Module.Factory() }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tagsAdapter = TagsAdapter {
            viewModel.setFilterCoin(it)
        }

        recyclerTags.adapter = tagsAdapter

        val transactionsAdapter = TransactionsAdapter2(
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
        recyclerTransactions.adapter = transactionsAdapter
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
                val diff = 9
                if (diff + pastVisibleItems + visibleItemCount >= totalItemCount) { //End of list
                    viewModel.onBottomReached()
                }
            }
        })

        val transactionSectionHeader = TransactionSectionHeader()
        recyclerTransactions.addItemDecoration(transactionSectionHeader.itemDecoration)

        viewModel.transactionList.observe(viewLifecycleOwner) { itemsList ->
            when (itemsList) {
                Transactions2ViewModel.ItemsList.Blank -> TODO()
                is Transactions2ViewModel.ItemsList.Filled -> {
                    transactionSectionHeader.updateList(itemsList.items)
                    transactionsAdapter.submitList(itemsList.items)
                }
            }
        }

        viewModel.filterCoinsLiveData.observe(viewLifecycleOwner) {
            tagsAdapter.submitList(it)
        }

        viewModel.syncingLiveData.observe(viewLifecycleOwner) {
            toolbarSpinner.isVisible = it
        }
    }
}

data class Filter<T>(val item: T, val selected: Boolean)

class TransactionViewItemDiff2 : DiffUtil.ItemCallback<TransactionViewItem2>() {

    override fun areItemsTheSame(oldItem: TransactionViewItem2, newItem: TransactionViewItem2): Boolean {
        return oldItem.itemTheSame(newItem)
    }

    override fun areContentsTheSame(oldItem: TransactionViewItem2, newItem: TransactionViewItem2): Boolean {
        return oldItem.contentTheSame(newItem)
    }

    override fun getChangePayload(oldItem: TransactionViewItem2, newItem: TransactionViewItem2): Any {
        return oldItem
    }

}

class TransactionsAdapter2(private val onItemDisplay: (TransactionViewItem2) -> Unit, private val onItemClick: (TransactionViewItem2) -> Unit) : ListAdapter<TransactionViewItem2, ViewHolderTransaction2>(
    TransactionViewItemDiff2()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderTransaction2 {
        return ViewHolderTransaction2(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_transaction, parent, false), onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolderTransaction2, position: Int) = Unit

    override fun onBindViewHolder(holder: ViewHolderTransaction2, position: Int, payloads: MutableList<Any>) {

        val item = getItem(position)
        onItemDisplay(item)

        val prev = payloads.lastOrNull() as? TransactionViewItem2

        if (prev == null) {
            holder.bind(item, showBottomShade = (position == itemCount - 1))
        } else {
            holder.bindUpdate(item, prev)
        }
    }
}

class ViewHolderTransaction2(
    override val containerView: View,
    private val onItemClick: (TransactionViewItem2) -> Unit
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private var item: TransactionViewItem2? = null

    init {
        containerView.setOnSingleClickListener {
            item?.let {
                onItemClick(it)
            }
        }
    }

    fun bind(item: TransactionViewItem2, showBottomShade: Boolean) {
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

    fun bindUpdate(current: TransactionViewItem2, prev: TransactionViewItem2, ) {
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


