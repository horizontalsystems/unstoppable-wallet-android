package io.horizontalsystems.bankwallet.modules.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import com.google.android.material.appbar.AppBarLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.ui.extensions.NpaLinearLayoutManager
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
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

        viewModel = ViewModelProviders.of(this).get(TransactionsViewModel::class.java)
        viewModel.init()

        transactionsAdapter.viewModel = viewModel
        toolbarTitle.setText(R.string.Transactions_Title)

        recyclerTransactions.setHasFixedSize(true)
        recyclerTransactions.adapter = transactionsAdapter
        recyclerTransactions.layoutManager = NpaLinearLayoutManager(context)
        recyclerTransactions.addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                filterAdapter.filterChangeable = newState == SCROLL_STATE_IDLE
            }
        })

        recyclerTags.adapter = filterAdapter
        recyclerTags.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        viewModel.filterItems.observe(viewLifecycleOwner, Observer { filters ->
            filters?.let {
                filterAdapter.setFilters(it)
            }
        })

        viewModel.transactionViewItemLiveEvent.observe(viewLifecycleOwner, Observer { transactionViewItem ->
            transactionViewItem?.let {
                (activity as? MainActivity)?.setTransactionInfoItem(it)
            }
        })

        viewModel.reloadLiveEvent.observe(viewLifecycleOwner, Observer {
            transactionsAdapter.notifyDataSetChanged()

            if (transactionsAdapter.itemCount == 0) {
                viewModel.delegate.onBottomReached()
            }

            recyclerTransactions.visibility = if (viewModel.delegate.itemsCount == 0) View.GONE else View.VISIBLE
            emptyListText.visibility = if (viewModel.delegate.itemsCount == 0) View.VISIBLE else View.GONE
        })

        viewModel.reloadChangeEvent.observe(viewLifecycleOwner, Observer { diff ->
            diff?.dispatchUpdatesTo(transactionsAdapter)

            if (transactionsAdapter.itemCount == 0) {
                viewModel.delegate.onBottomReached()
            }

            recyclerTransactions.visibility = if (viewModel.delegate.itemsCount == 0) View.GONE else View.VISIBLE
            emptyListText.visibility = if (viewModel.delegate.itemsCount == 0) View.VISIBLE else View.GONE
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

        setAppBarAnimation()
    }

    private fun setAppBarAnimation() {
        toolbarTitle.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                toolbarTitle.pivotX = 0f
                toolbarTitle.pivotY = toolbarTitle.height.toFloat()
                toolbarTitle.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        app_bar_layout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val fraction = Math.abs(verticalOffset).toFloat() / appBarLayout.totalScrollRange
            var alphaFract = 1f - fraction
            if (alphaFract < 0.20) {
                alphaFract = 0f
            }
            toolbarTitle.alpha = alphaFract
            toolbarTitle.scaleX = (1f - fraction / 3)
            toolbarTitle.scaleY = (1f - fraction / 3)
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

    override fun onFilterItemClick(item: Coin?) {
        viewModel.delegate.onFilterSelect(item)
    }

}

class TransactionsAdapter(private var listener: Listener) : Adapter<ViewHolder>(), ViewHolderTransaction.ClickListener {

    interface Listener {
        fun onItemClick(item: TransactionViewItem)
    }

    lateinit var viewModel: TransactionsViewModel

    override fun getItemCount(): Int {
        return viewModel.delegate.itemsCount
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolderTransaction(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_transaction, parent, false), this)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position > itemCount - 9) {
            viewModel.delegate.onBottomReached()
        }

        when (holder) {
            is ViewHolderTransaction -> {
                holder.bind(viewModel.delegate.itemForIndex(position), showBottomShade = (position == itemCount - 1))
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
        txValueInFiat.text = transactionRecord.currencyValue?.let {
            App.numberFormatter.formatForTransactions(it, transactionRecord.incoming)
        }
        txValueInCoin.text = App.numberFormatter.formatForTransactions(transactionRecord.coinValue)
        txDate.text = transactionRecord.date?.let { DateHelper.getShortDateForTransaction(it) }
        val time = transactionRecord.date?.let { DateHelper.getOnlyTime(it) }
        txStatusWithTimeView.bind(transactionRecord.status, time)
        bottomShade.visibility = if (showBottomShade) View.VISIBLE else View.GONE
    }
}

class FilterAdapter(private var listener: Listener) : Adapter<ViewHolder>(), ViewHolderFilter.ClickListener {

    interface Listener {
        fun onFilterItemClick(item: Coin?)
    }

    var filterChangeable = true

    private var selectedFilterId: Coin? = null
    private var filters: List<Coin?> = listOf()

    fun setFilters(filters: List<Coin?>) {
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

    fun bind(coin: Coin?, active: Boolean) {
        filter_text.text = coin?.code ?: containerView.context.getString(R.string.Transactions_FilterAll)
        filter_text.isActivated = active
        filter_text.setOnClickListener { l.onClickItem(adapterPosition) }
    }
}
