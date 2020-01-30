package io.horizontalsystems.bankwallet.modules.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.entities.TransactionType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.ui.extensions.NpaLinearLayoutManager
import io.horizontalsystems.bankwallet.ui.helpers.DateHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_transactions.*
import kotlinx.android.synthetic.main.view_holder_filter.*
import kotlinx.android.synthetic.main.view_holder_transaction.*
import java.util.logging.Logger

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
        recyclerTransactions.setHasFixedSize(true)
        recyclerTransactions.adapter = transactionsAdapter
        recyclerTransactions.layoutManager = layoutManager
        recyclerTransactions.addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                filterAdapter.filterChangeable = newState == SCROLL_STATE_IDLE
            }
        })

        viewModel.filterItems.observe(viewLifecycleOwner, Observer { filters ->
            filters?.let {
                filterAdapter.setFilters(it)
            }
        })

        viewModel.transactionViewItemLiveEvent.observe(viewLifecycleOwner, Observer { transactionViewItem ->
            transactionViewItem?.let {
                (activity as? MainActivity)?.openTransactionInfo(it)
            }
        })

        viewModel.reloadLiveEvent.observe(viewLifecycleOwner, Observer {
            transactionsAdapter.notifyDataSetChanged()

            if (viewModel.delegate.itemsCount == 0) {
                viewModel.delegate.onBottomReached()
            }
        })

        viewModel.reloadChangeEvent.observe(viewLifecycleOwner, Observer { diff ->
            diff?.dispatchUpdatesTo(transactionsAdapter)

            if (viewModel.delegate.itemsCount == 0) {
                viewModel.delegate.onBottomReached()
            }
        })

        viewModel.addItemsLiveEvent.observe(viewLifecycleOwner, Observer {
            it?.let { (fromIndex, count) ->
                transactionsAdapter.notifyItemRangeInserted(fromIndex, count)
            }
        })

        viewModel.reloadItemsLiveEvent.observe(viewLifecycleOwner, Observer {
            it?.forEach { index ->
                transactionsAdapter.notifyItemChanged(index)
            }
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
        viewModel.delegate.onFilterSelect(item)
    }

}

class TransactionsAdapter(private var listener: Listener) : Adapter<ViewHolder>(), ViewHolderTransaction.ClickListener {

    private val noTransactionsView = 0
    private val transactionView = 1
    private val logger = Logger.getLogger("TransactionsAdapter")

    interface Listener {
        fun onItemClick(item: TransactionViewItem)
    }

    lateinit var viewModel: TransactionsViewModel

    override fun getItemCount(): Int {
        return if (viewModel.delegate.itemsCount == 0) 1 else viewModel.delegate.itemsCount
    }

    override fun getItemViewType(position: Int): Int {
        return if (viewModel.delegate.itemsCount == 0) noTransactionsView else transactionView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            noTransactionsView -> ViewHolderEmptyScreen(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_empty_screen, parent, false))
            else -> ViewHolderTransaction(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_transaction, parent, false), this)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position > itemCount - 9) {
            viewModel.delegate.onBottomReached()
        }

        if (holder is ViewHolderTransaction) {
            try {
                holder.bind(viewModel.delegate.itemForIndex(position), showBottomShade = (position == itemCount - 1))
            } catch (e: ArrayIndexOutOfBoundsException) {
                logger.warning("throwing exception ArrayIndexOutOfBoundsException in TransactionsFragment")
            }
        }
    }

    override fun onClick(position: Int) {
        listener.onItemClick(viewModel.delegate.itemForIndex(position))
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
            App.numberFormatter.formatForTransactions(it, incoming, canUseLessSymbol = true, trimmable = true)
        }

        val lockIcon = when {
            transactionRecord.lockInfo == null -> 0
            transactionRecord.unlocked -> R.drawable.ic_unlock
            else -> R.drawable.ic_lock
        }
        txValueInFiat.setCompoundDrawablesWithIntrinsicBounds(0, 0, lockIcon, 0)
        txValueInCoin.text = App.numberFormatter.formatForTransactions(transactionRecord.coinValue)
        directionIcon.setImageResource(if (incoming) R.drawable.ic_incoming else R.drawable.ic_outgoing)
        txDate.text = transactionRecord.date?.let { DateHelper.getShortDateForTransaction(it) }
        val time = transactionRecord.date?.let { DateHelper.getOnlyTime(it) }
        txStatusWithTimeView.bind(transactionRecord.status, incoming, time)
        bottomShade.visibility = if (showBottomShade) View.VISIBLE else View.GONE
        sentToSelfIcon.visibility = if (sentToSelf) View.VISIBLE else View.GONE
        doubleSpendIcon.visibility = if (transactionRecord.conflictingTxHash == null) View.GONE else View.VISIBLE
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
        filter_text.text = wallet?.coin?.code
                ?: containerView.context.getString(R.string.Transactions_FilterAll)
        filter_text.isActivated = active
        filter_text.setOnClickListener { l.onClickItem(adapterPosition) }
    }
}
