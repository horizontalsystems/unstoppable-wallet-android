package io.horizontalsystems.bankwallet.modules.transactions.q

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.TransactionLockState
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewHelper
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_transactions.*
import kotlinx.android.synthetic.main.view_holder_filter.*
import kotlinx.android.synthetic.main.view_holder_transaction.*

class TransactionsFragment2 : BaseFragment(R.layout.fragment_transactions) {

    private val viewModel by viewModels<Transactions2ViewModel> { Transactions2Module.Factory() }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tagsAdapter = TagsAdapter()

        recyclerTags.adapter = tagsAdapter

        val transactionsAdapter = TransactionsAdapter2()
        recyclerTransactions.adapter = transactionsAdapter

        viewModel.transactionList.observe(viewLifecycleOwner) { itemsList ->
            when (itemsList) {
                Transactions2ViewModel.ItemsList.Blank -> TODO()
                is Transactions2ViewModel.ItemsList.Filled -> {
                    transactionsAdapter.submitList(itemsList.items)
                }
            }
        }

//        viewModel.filtersLiveData.observe(viewLifecycleOwner) {
//            tagsAdapter.submitList(it)
//        }
    }
}

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

class TransactionsAdapter2 : ListAdapter<TransactionViewItem2, ViewHolderTransaction2>(TransactionViewItemDiff2()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderTransaction2 {
        return ViewHolderTransaction2(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_transaction, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolderTransaction2, position: Int) = Unit

    override fun onBindViewHolder(holder: ViewHolderTransaction2, position: Int, payloads: MutableList<Any>) {

        val item = getItem(position)
//        viewModel.delegate.willShow(item)

//        val prev = payloads.lastOrNull() as? TransactionViewItem

//        if (prev == null) {
            holder.bind(item, showBottomShade = (position == itemCount - 1))
//        } else {
//            holder.bindUpdate(item, prev)
//        }
    }
}

class ViewHolderTransaction2(override val containerView: View) :
    RecyclerView.ViewHolder(containerView), LayoutContainer {

    interface ClickListener {
        fun onClick(position: Int)
    }

    init {
//        containerView.setOnSingleClickListener { l.onClick(bindingAdapterPosition) }
    }

    fun bind(transactionRecord: TransactionViewItem2, showBottomShade: Boolean) {
        txIcon.setImageResource(transactionRecord.typeIcon)
        txTopText.text = transactionRecord.title
        txBottomText.text = transactionRecord.subtitle
        txPrimaryText.text = transactionRecord.primaryValue?.value
        txSecondaryText.text = transactionRecord.secondaryValue?.value

        transactionRecord.primaryValue?.color?.let {
            txPrimaryText.setTextColor(getColor(it))
        }
        transactionRecord.secondaryValue?.color?.let {
            txSecondaryText.setTextColor(getColor(it))
        }

//        setProgress(transactionRecord.status)

        doubleSpendIcon.isVisible = transactionRecord.doubleSpend
        sentToSelfIcon.isVisible = transactionRecord.sentToSelf
        bottomShade.isVisible = showBottomShade
//        setLockIcon(transactionRecord.lockState)
    }

//    fun bindUpdate(current: TransactionViewItem2, prev: TransactionViewItem2) {
//        if (current.image != prev.image) {
//            txIcon.setImageResource(current.image)
//        }
//
//        if (current.topText != prev.topText) {
//            txTopText.text = current.topText
//        }
//
//        if (current.bottomText != prev.bottomText) {
//            txBottomText.text = current.bottomText
//        }
//
//        if (current.primaryValueText != prev.primaryValueText) {
//            txPrimaryText.text = current.primaryValueText
//        }
//
//        if (current.secondaryValueText != prev.secondaryValueText) {
//            txSecondaryText.text = current.secondaryValueText
//        }
//
//        if (current.primaryValueTextColor != prev.primaryValueTextColor) {
//            txPrimaryText.setTextColor(getColor(current.primaryValueTextColor))
//        }
//
//        if (current.secondaryValueTextColor != prev.secondaryValueTextColor) {
//            txSecondaryText.setTextColor(getColor(current.secondaryValueTextColor))
//        }
//
//        if (current.status != prev.status) {
//            setProgress(current.status)
//        }
//
//        if (current.showDoubleSpend != prev.showDoubleSpend) {
//            doubleSpendIcon.isVisible = current.showDoubleSpend
//        }
//
//        if (current.showSentToSelf != prev.showSentToSelf) {
//            sentToSelfIcon.isVisible = current.showSentToSelf
//        }
//
//        if (current.lockState != prev.lockState) {
//            setLockIcon(current.lockState)
//        }
//    }

    private fun setProgress(status: TransactionStatus) {
        when (status) {
            is TransactionStatus.Pending -> {
                iconProgress.isVisible = true
                iconProgress.setProgressColored(15, getColor(R.color.grey_50), true)
            }
            is TransactionStatus.Processing -> {
                iconProgress.isVisible = true
                val progressValue = (status.progress * 100).toInt()
                iconProgress.setProgressColored(progressValue, getColor(R.color.grey_50), true)
            }
            else -> iconProgress.isVisible = false
        }
    }

    private fun getColor(primaryValueTextColor: Int) =
        containerView.context.getColor(primaryValueTextColor)

    private fun setLockIcon(lockState: TransactionLockState?) {
        val imgRes = TransactionViewHelper.getLockIcon(lockState)
        lockIcon.isVisible = imgRes > 0
        lockIcon.setImageResource(imgRes)
    }

}


class TagsAdapter : ListAdapter<Transactions2Module.Filter, ViewHolderFilter>(
    object : DiffUtil.ItemCallback<Transactions2Module.Filter>() {
        override fun areItemsTheSame(oldItem: Transactions2Module.Filter, newItem: Transactions2Module.Filter) = oldItem == newItem
        override fun areContentsTheSame(oldItem: Transactions2Module.Filter, newItem: Transactions2Module.Filter) = oldItem == newItem
    }
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderFilter {
        return ViewHolderFilter(inflate(parent, R.layout.view_holder_filter))
    }

    override fun onBindViewHolder(holder: ViewHolderFilter, position: Int) {
        holder.bind(getItem(position))
    }

}

class ViewHolderFilter(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    fun bind(filter: Transactions2Module.Filter) {
        buttonFilter.text = filter.coin?.code ?: "All"
    }

}