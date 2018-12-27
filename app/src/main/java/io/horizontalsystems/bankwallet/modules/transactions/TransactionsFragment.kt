package io.horizontalsystems.bankwallet.modules.transactions

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.annotation.NonNull
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoModule
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import io.horizontalsystems.bankwallet.viewHelpers.ValueFormatter
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_transactions.*
import kotlinx.android.synthetic.main.view_holder_filter.*
import kotlinx.android.synthetic.main.view_holder_transaction.*

class TransactionsFragment : android.support.v4.app.Fragment(), TransactionsAdapter.Listener, FilterAdapter.Listener {

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

        val bottomSheetBehavior = BottomSheetBehavior.from(nestedScrollView)

        transactionsDim.alpha = 0f

        var bottomSheetSlideOffOld = 0f

        bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(@NonNull bottomSheet: View, newState: Int) {}

            override fun onSlide(@NonNull bottomSheet: View, slideOffset: Float) {
                transactionsDim.alpha = slideOffset
                if (bottomSheetSlideOffOld >= 0.7 && slideOffset < 0.7) {
                    (activity as? MainActivity)?.setBottomNavigationVisible(true)
                } else if (bottomSheetSlideOffOld >= 0.8 && slideOffset > 0.9) {
                    (activity as? MainActivity)?.setBottomNavigationVisible(false)
                }
                bottomSheetSlideOffOld = slideOffset
            }
        })

        viewModel.filterItems.observe(this, Observer { filters ->
            filters?.let {
                filterAdapter.filters = it
                filterAdapter.notifyDataSetChanged()
            }
        })

        viewModel.showTransactionInfoLiveEvent.observe(this, Observer { transactionHash ->
            transactionHash?.let { transactionHash ->
//               (activity as? MainActivity)?.setBottomNavigationVisible(false)
//                Handler().postDelayed({
//                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
//                }, 50)
                activity?.let { TransactionInfoModule.start(it, transactionHash) }
            }
        })

        viewModel.didRefreshLiveEvent.observe(this, Observer {
            pullToRefresh.isRefreshing = false
        })

        viewModel.reloadLiveEvent.observe(this, Observer {
            transactionsAdapter.notifyDataSetChanged()

            recyclerTransactions.visibility = if (viewModel.delegate.itemsCount == 0) View.GONE else View.VISIBLE
            emptyListText.visibility = if (viewModel.delegate.itemsCount == 0) View.VISIBLE else View.GONE
        })

        toolbar.setTitle(R.string.Transactions_Title)
        recyclerTransactions.adapter = transactionsAdapter
        recyclerTransactions.layoutManager = LinearLayoutManager(context)

        recyclerTags.adapter = filterAdapter
        recyclerTags.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        pullToRefresh.setOnRefreshListener {
            viewModel.delegate.refresh()
        }

    }

    override fun onItemClick(item: TransactionViewItem) {
        viewModel.delegate.onTransactionItemClick(item)
    }

    override fun onFilterItemClick(item: TransactionFilterItem) {
        viewModel.delegate.onFilterSelect(item.adapterId)
    }
}

class TransactionsAdapter(private var listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onItemClick(item: TransactionViewItem)
    }

    lateinit var viewModel: TransactionsViewModel

    override fun getItemCount() = viewModel.delegate.itemsCount

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolderTransaction(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_transaction, parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderTransaction -> {
                val transactionRecord = viewModel.delegate.itemForIndex(position)
                holder.bind(transactionRecord) { listener.onItemClick(transactionRecord) }
            }
        }
    }

}

class ViewHolderTransaction(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(transactionRecord: TransactionViewItem, onClick: () -> (Unit)) {

        containerView.setOnSingleClickListener { onClick.invoke() }

        val amountTextColor = if (transactionRecord.incoming) R.color.green_crypto else R.color.yellow_crypto
        txAmount.setTextColor(ContextCompat.getColor(itemView.context, amountTextColor))
        txAmount.text = ValueFormatter.format(transactionRecord.coinValue, true)
        txDate.text = transactionRecord.date?.let { DateHelper.getShortDateForTransaction(it) }
        txTime.text = transactionRecord.date?.let { DateHelper.getOnlyTime(it) }
        txValueInFiat.text = transactionRecord.currencyValue?.let { ValueFormatter.format(it, true) }
        statusIcon.setImageDrawable(getStatusIcon(transactionRecord.status))
        pendingShade.visibility = if (transactionRecord.status == TransactionStatus.Pending) View.VISIBLE else View.GONE
    }

    private fun getStatusIcon(status: TransactionStatus?): Drawable? {
        return if (status is TransactionStatus.Completed)
            LayoutHelper.d(R.drawable.checkmark_small_grey, App.instance)
        else
            LayoutHelper.d(R.drawable.pending, App.instance)
    }
}

class FilterAdapter(private var listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onFilterItemClick(item: TransactionFilterItem)
    }

    var selectedFilterId: String? = null
    var filters: List<TransactionFilterItem> = listOf()

    override fun getItemCount() = filters.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolderFilter(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_filter, parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderFilter -> holder.bind(xxx(filters[position].name), active = selectedFilterId == filters[position].adapterId) {
                listener.onFilterItemClick(filters[position])
                selectedFilterId = filters[position].adapterId
                notifyDataSetChanged()
            }
        }
    }

    fun xxx(coinCode: CoinCode) = when (coinCode) {
        "BTC" -> "Bitcoin"
        "BTCt" -> "Bitcoin-T"
        "BTCr" -> "Bitcoin-R"
        "BCH" -> "Bitcoin Cash"
        "BCHt" -> "Bitcoin Cash-T"
        "ETH" -> "Ethereum"
        "ETHt" -> "Ethereum-T"
        else -> coinCode
    }


}

class ViewHolderFilter(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(filterName: String, active: Boolean, onClick: () -> (Unit)) {
        filter_text.setOnClickListener { onClick.invoke() }

        filter_text.text = filterName
        filter_text.isActivated = active
    }
}
