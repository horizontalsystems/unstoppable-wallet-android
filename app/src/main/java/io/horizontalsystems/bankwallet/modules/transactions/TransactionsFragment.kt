package io.horizontalsystems.bankwallet.modules.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
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
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_transactions.*
import kotlinx.android.synthetic.main.view_holder_filter.*
import kotlinx.android.synthetic.main.view_holder_transaction.*

class TransactionsFragment : Fragment(), TransactionsAdapter.Listener, FilterAdapter.Listener {

    private lateinit var viewModel: TransactionsViewModel
    private val transactionsAdapter = TransactionsAdapter(this)
    private val filterAdapter = FilterAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProvider(this).get(TransactionsViewModel::class.java)
        viewModel.init()

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

        viewModel.filterItems.observe(viewLifecycleOwner, Observer { filters ->
            filters?.let {
                filterAdapter.setFilters(it)
            }
        })

        viewModel.transactionViewItemLiveEvent.observe(viewLifecycleOwner, Observer { transactionViewItem ->
            activity?.let {
                TransactionInfoModule.start(it, transactionViewItem.record, transactionViewItem.wallet)
            }
        })

        viewModel.items.observe(viewLifecycleOwner, Observer {
            transactionsAdapter.updateItems(it)
        })

        viewModel.reloadTransactions.observe(viewLifecycleOwner, Observer {
            transactionsAdapter.notifyDataSetChanged()
        })

        viewModel.showSyncing.observe(viewLifecycleOwner, Observer { show ->
            toolbarSpinner.visibility = if (show) View.VISIBLE else View.INVISIBLE
        })

    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        if (menuVisible) {
            viewModel.delegate.onVisible()
        }
    }

    override fun onItemClick(item: TransactionViewItem) {
        viewModel.delegate.onTransactionItemClick(item)
    }

    override fun onFilterItemClick(item: Wallet?) {
        recyclerTransactions.layoutManager?.scrollToPosition(0)
        viewModel.delegate.onFilterSelect(item)
    }

}

class TransactionsAdapter(private var listener: Listener) : Adapter<ViewHolder>(), ViewHolderTransaction.ClickListener {

    private val noTransactionsView = 0
    private val transactionView = 1
    private var items = listOf<TransactionViewItem>()

    interface Listener {
        fun onItemClick(item: TransactionViewItem)
    }

    lateinit var viewModel: TransactionsViewModel

    override fun getItemCount(): Int {
        return if (items.size == 0) 1 else items.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (items.size == 0) noTransactionsView else transactionView
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

        viewModel.delegate.willShow(items[position])

        val prev = payloads.lastOrNull() as? TransactionViewItem

        if (prev == null) {
            holder.bind(items[position], showBottomShade = (position == itemCount - 1))
        } else {
            holder.bindUpdate(items[position], prev)
        }
    }

    override fun onClick(position: Int) {
        listener.onItemClick(items[position])
    }

    fun updateItems(items: List<TransactionViewItem>) {
        if (this.items.isEmpty()) {
            this.items = items
            notifyDataSetChanged()
        } else {
            val diffResult = DiffUtil.calculateDiff(TransactionViewItemDiff(this.items, items))
            this.items = items
            diffResult.dispatchUpdatesTo(this)
        }
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
        val incoming = transactionRecord.type == TransactionType.Incoming
        val sentToSelf = transactionRecord.type == TransactionType.SentToSelf

        txValueInFiat.text = transactionRecord.currencyValue?.let {
            App.numberFormatter.format(it, showNegativeSign = false, trimmable = true, canUseLessSymbol = true)
        }
        txValueInFiat.setTextColor(getAmountColor(incoming))

        txValueInFiat.setCompoundDrawablesWithIntrinsicBounds(0, 0, getLockIcon(transactionRecord.lockState), 0)
        txValueInCoin.text = App.numberFormatter.format(transactionRecord.coinValue)
        directionIcon.setImageResource(if (incoming) R.drawable.ic_incoming else R.drawable.ic_outgoing)
        txDate.text = transactionRecord.date?.let { DateHelper.shortDate(it) }
        val time = transactionRecord.date?.let { DateHelper.getOnlyTime(it) }
        txStatusWithTimeView.bind(transactionRecord.status, incoming, time)
        bottomShade.visibility = if (showBottomShade) View.VISIBLE else View.GONE
        sentToSelfIcon.visibility = if (sentToSelf) View.VISIBLE else View.GONE
        doubleSpendIcon.visibility = if (transactionRecord.doubleSpend) View.VISIBLE else View.GONE
    }

    private fun getLockIcon(lockState: TransactionLockState?) = when {
        lockState == null -> 0
        lockState.locked -> R.drawable.ic_lock
        else -> R.drawable.ic_unlock
    }

    fun bindUpdate(current: TransactionViewItem, prev: TransactionViewItem) {
        val incoming = current.type == TransactionType.Incoming

        if (current.currencyValue != prev.currencyValue) {
            txValueInFiat.text = current.currencyValue?.let {
                App.numberFormatter.format(it, showNegativeSign = false, trimmable = true, canUseLessSymbol = true)
            }
            txValueInFiat.setTextColor(getAmountColor(incoming))
        }

        if (current.lockState != prev.lockState) {
            txValueInFiat.setCompoundDrawablesWithIntrinsicBounds(0, 0, getLockIcon(current.lockState), 0)
        }

        if (current.coinValue != prev.coinValue) {
            txValueInCoin.text = App.numberFormatter.format(current.coinValue)
        }

        if (current.status != prev.status || current.date != prev.date) {
            txDate.text = current.date?.let { DateHelper.shortDate(it) }
            val time = current.date?.let { DateHelper.getOnlyTime(it) }
            txStatusWithTimeView.bind(current.status, incoming, time)
        }

        if (current.doubleSpend != prev.doubleSpend) {
            doubleSpendIcon.visibility = if (current.doubleSpend) View.VISIBLE else View.GONE
        }
    }

    private fun getAmountColor(incoming: Boolean): Int {
        val amountTextColor = if (incoming) R.color.green_d else R.color.yellow_d
        return itemView.context.getColor(amountTextColor)
    }
}

class ViewHolderEmptyScreen(override val containerView: View) : ViewHolder(containerView), LayoutContainer

class FilterAdapter(private var listener: Listener) : Adapter<ViewHolder>(), ViewHolderFilter.ClickListener {

    interface Listener {
        fun onFilterItemClick(item: Wallet?)
    }

    var filterChangeable = true

    private var selectedFilterId: Wallet? = null
    private var filters: List<Wallet?> = listOf()

    fun setFilters(filters: List<Wallet?>) {
        this.filters = filters
        selectedFilterId = null
        notifyDataSetChanged()
    }

    override fun getItemCount() = filters.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolderFilter(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_filter, parent, false), this)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderFilter -> holder.bind(filters[position], selectedFilterId == filters[position])
        }
    }

    override fun onClickItem(position: Int) {
        if (filterChangeable) {
            listener.onFilterItemClick(filters[position])
            selectedFilterId = filters[position]
            notifyDataSetChanged()
        }
    }
}

class ViewHolderFilter(override val containerView: View, private val l: ClickListener) : ViewHolder(containerView), LayoutContainer {

    interface ClickListener {
        fun onClickItem(position: Int)
    }

    fun bind(wallet: Wallet?, active: Boolean) {
        buttonFilter.text = wallet?.coin?.code ?: containerView.context.getString(R.string.Transactions_FilterAll)
        buttonFilter.isActivated = active
        buttonFilter.setOnClickListener { l.onClickItem(adapterPosition) }
    }
}
