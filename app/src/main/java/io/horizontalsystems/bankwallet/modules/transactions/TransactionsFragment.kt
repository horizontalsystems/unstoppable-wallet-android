package io.horizontalsystems.bankwallet.modules.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.entities.TransactionType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.TransactionInfoModule
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.TransactionLockState
import io.horizontalsystems.bankwallet.ui.extensions.NpaLinearLayoutManager
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_transactions.*
import kotlinx.android.synthetic.main.view_holder_filter.*
import kotlinx.android.synthetic.main.view_holder_transaction.*

class TransactionsFragment : Fragment(), TransactionsAdapter.Listener, FilterAdapter.Listener {

    private val viewModel by viewModels<TransactionsViewModel> { TransactionsModule.Factory() }
    private val transactionsAdapter = TransactionsAdapter(this)
    private val filterAdapter = FilterAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = NpaLinearLayoutManager(context)
        transactionsAdapter.viewModel = viewModel
        transactionsAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == 0) {
                    layoutManager.scrollToPosition(0)
                }
            }
        })

        recyclerTags.adapter = filterAdapter
        recyclerTransactions.itemAnimator = null
        recyclerTransactions.setHasFixedSize(true)
        recyclerTransactions.adapter = transactionsAdapter
        recyclerTransactions.layoutManager = layoutManager
        recyclerTransactions.addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                filterAdapter.filterChangeable = newState == SCROLL_STATE_IDLE
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val pastVisibleItems = layoutManager.findFirstVisibleItemPosition()
                val diff = 9
                if (diff + pastVisibleItems + visibleItemCount >= totalItemCount) { //End of list
                    viewModel.delegate.onBottomReached()
                }
            }
        })

        viewModel.filterItems.observe(viewLifecycleOwner, { filters ->
            filters?.let {
                filterAdapter.setFilters(it)
            }
        })

        viewModel.transactionViewItemLiveEvent.observe(viewLifecycleOwner, { transactionViewItem ->
            activity?.let {
                TransactionInfoModule.start(it, transactionViewItem.record, transactionViewItem.wallet)
            }
        })

        viewModel.items.observe(viewLifecycleOwner, {
            transactionsAdapter.submitList(it)
        })

        viewModel.reloadTransactions.observe(viewLifecycleOwner, {
            transactionsAdapter.notifyDataSetChanged()
        })

        viewModel.showSyncing.observe(viewLifecycleOwner, { show ->
            toolbarSpinner.isInvisible = !show
        })

    }

    override fun onDestroyView() {
        super.onDestroyView()

        recyclerTags.adapter = null
        recyclerTransactions.adapter = null
        recyclerTransactions.layoutManager = null
    }

    override fun onItemClick(item: TransactionViewItem) {
        viewModel.delegate.onTransactionItemClick(item)
    }

    override fun onFilterItemClick(item: FilterAdapter.FilterItem?) {
        recyclerTransactions.layoutManager?.scrollToPosition(0)
        viewModel.delegate.onFilterSelect(item as? Wallet)
    }

}

class TransactionsAdapter(private var listener: Listener) : ListAdapter<TransactionViewItem, ViewHolder>(TransactionViewItemDiff()), ViewHolderTransaction.ClickListener {

    private val noTransactionsView = 0
    private val transactionView = 1

    interface Listener {
        fun onItemClick(item: TransactionViewItem)
    }

    lateinit var viewModel: TransactionsViewModel

    override fun getItemCount(): Int {
        return if (super.getItemCount() == 0) 1 else super.getItemCount()
    }

    override fun getItemViewType(position: Int): Int {
        return if (super.getItemCount() == 0) noTransactionsView else transactionView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            noTransactionsView -> ViewHolderEmptyScreen(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_empty_screen, parent, false))
            else -> ViewHolderTransaction(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_transaction, parent, false), this)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = Unit

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (holder !is ViewHolderTransaction) return

        val item = getItem(position)
        viewModel.delegate.willShow(item)

        val prev = payloads.lastOrNull() as? TransactionViewItem

        if (prev == null) {
            holder.bind(item, showBottomShade = (position == itemCount - 1))
        } else {
            holder.bindUpdate(item, prev)
        }
    }

    override fun onClick(position: Int) {
        listener.onItemClick(getItem(position))
    }
}

class ViewHolderTransaction(override val containerView: View, private val l: ClickListener) : ViewHolder(containerView), LayoutContainer {

    interface ClickListener {
        fun onClick(position: Int)
    }

    init {
        containerView.setOnSingleClickListener { l.onClick(adapterPosition) }
    }

    fun bind(transactionRecord: TransactionViewItem, showBottomShade: Boolean) {
        txValueInFiat.text = transactionRecord.currencyValue?.let {
            App.numberFormatter.formatFiat(it.value, it.currency.symbol, 0, 2)
        }
        txValueInFiat.setTextColor(TransactionViewHelper.getAmountColor(transactionRecord.type, itemView.context))
        txValueInFiat.setCompoundDrawablesWithIntrinsicBounds(0, 0, TransactionViewHelper.getLockIcon(transactionRecord.lockState), 0)
        txValueInCoin.text = App.numberFormatter.formatCoin(transactionRecord.coinValue.value, transactionRecord.coinValue.coin.code, 0, 8)
        txTypeIcon.setImageResource(TransactionViewHelper.getTransactionTypeIcon(transactionRecord.type))
        txDate.text = transactionRecord.date?.let { DateHelper.shortDate(it) }
        val time = transactionRecord.date?.let { DateHelper.getOnlyTime(it) }
        txStatusWithTimeView.bind(transactionRecord.status, transactionRecord.type, time)
        bottomShade.isVisible = showBottomShade
        sentToSelfIcon.isVisible = transactionRecord.type == TransactionType.SentToSelf
        doubleSpendIcon.isVisible = transactionRecord.doubleSpend
    }

    fun bindUpdate(current: TransactionViewItem, prev: TransactionViewItem) {
        if (current.currencyValue != prev.currencyValue) {
            txValueInFiat.text = current.currencyValue?.let {
                App.numberFormatter.formatFiat(it.value, it.currency.symbol, 0, 2)
            }
            txValueInFiat.setTextColor(TransactionViewHelper.getAmountColor(current.type, itemView.context))
        }

        if (current.lockState != prev.lockState) {
            txValueInFiat.setCompoundDrawablesWithIntrinsicBounds(0, 0, TransactionViewHelper.getLockIcon(current.lockState), 0)
        }

        if (current.coinValue != prev.coinValue) {
            txValueInCoin.text = App.numberFormatter.formatCoin(current.coinValue.value, current.coinValue.coin.code, 0, 8)
        }

        if (current.status != prev.status || current.date != prev.date) {
            txDate.text = current.date?.let { DateHelper.shortDate(it) }
            val time = current.date?.let { DateHelper.getOnlyTime(it) }
            txStatusWithTimeView.bind(current.status, current.type, time)
        }

        if (current.doubleSpend != prev.doubleSpend) {
            doubleSpendIcon.isVisible = current.doubleSpend
        }
    }

}

class ViewHolderEmptyScreen(override val containerView: View) : ViewHolder(containerView), LayoutContainer

class FilterAdapter(private var listener: Listener) : Adapter<ViewHolder>(), ViewHolderFilter.ClickListener {

    interface Listener {
        fun onFilterItemClick(item: FilterItem?)
    }

    open class FilterItem(val filterId: String)

    var filterChangeable = true

    private var selectedFilterItem: FilterItem? = null
    private var filters: List<FilterItem?> = listOf()

    fun setFilters(filters: List<FilterItem?>) {
        this.filters = filters
        selectedFilterItem = filters.firstOrNull()
        notifyDataSetChanged()
    }

    override fun getItemCount() = filters.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolderFilter(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_filter, parent, false), this)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderFilter -> {
                holder.bind(filters[position]?.filterId, selectedFilterItem == filters[position])
            }
        }
    }

    override fun onClickItem(position: Int) {
        if (filterChangeable) {
            listener.onFilterItemClick(filters[position])
            selectedFilterItem = filters[position]
            notifyDataSetChanged()
        }
    }
}

class ViewHolderFilter(override val containerView: View, private val l: ClickListener) : ViewHolder(containerView), LayoutContainer {

    interface ClickListener {
        fun onClickItem(position: Int)
    }

    fun bind(filterId: String?, active: Boolean) {
        buttonFilter.text = filterId
                ?: containerView.context.getString(R.string.Transactions_FilterAll)
        buttonFilter.isActivated = active
        buttonFilter.setOnClickListener { l.onClickItem(adapterPosition) }
    }
}
