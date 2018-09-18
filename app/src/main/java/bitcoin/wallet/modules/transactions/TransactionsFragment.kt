package bitcoin.wallet.modules.transactions

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import bitcoin.wallet.R
import bitcoin.wallet.core.App
import bitcoin.wallet.core.setOnSingleClickListener
import bitcoin.wallet.modules.transactionInfo.TransactionInfoModule
import bitcoin.wallet.viewHelpers.DateHelper
import bitcoin.wallet.viewHelpers.LayoutHelper
import bitcoin.wallet.viewHelpers.NumberFormatHelper
import bitcoin.wallet.viewHelpers.TextHelper
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(TransactionsViewModel::class.java)
        viewModel.init()

        viewModel.transactionItems.observe(this, Observer { transactionItems ->
            transactionItems?.let {
                transactionsAdapter.items = it
                transactionsAdapter.notifyDataSetChanged()

                recyclerTransactions.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
                emptyListText.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            }
        })

        viewModel.filterItems.observe(this, Observer { filters ->
            filters?.let {
                filterAdapter.filters = it
                filterAdapter.notifyDataSetChanged()
            }
        })

        viewModel.showTransactionInfoLiveEvent.observe(this, Observer { transactionRecordViewItem ->
            transactionRecordViewItem?.let { transaction ->
                activity?.let { TransactionInfoModule.start(it, transaction) }
            }
        })

        viewModel.didRefreshLiveEvent.observe(this, Observer {
            pullToRefresh.isRefreshing = false
        })

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setTitle(R.string.transactions_title)
        recyclerTransactions.adapter = transactionsAdapter
        recyclerTransactions.layoutManager = LinearLayoutManager(context)

        recyclerTags.adapter = filterAdapter
        recyclerTags.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        pullToRefresh.setOnRefreshListener {
            viewModel.delegate.refresh()
        }
    }

    override fun onItemClick(item: TransactionRecordViewItem) {
        viewModel.delegate.onTransactionItemClick(item)
    }

    override fun onFilterItemClick(item: TransactionFilterItem) {
        viewModel.delegate.onFilterSelect(item.adapterId)
    }
}

class TransactionsAdapter(private var listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onItemClick(item: TransactionRecordViewItem)
    }

    var items = listOf<TransactionRecordViewItem>()

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolderTransaction(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_transaction, parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderTransaction -> holder.bind(items[position]) { listener.onItemClick(items[position]) }
        }
    }

}

class ViewHolderTransaction(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(transactionRecord: TransactionRecordViewItem, onClick: () -> (Unit)) {

        containerView.setOnSingleClickListener { onClick.invoke() }

        val sign = if (transactionRecord.incoming) "+" else "-"
        val amountTextColor = if (transactionRecord.incoming) R.color.green_crypto else R.color.yellow_crypto
        txAmount.setTextColor(ContextCompat.getColor(itemView.context, amountTextColor))
        txAmount.text = "$sign ${NumberFormatHelper.cryptoAmountFormat.format(Math.abs(transactionRecord.amount.value))} ${transactionRecord.amount.coin.code}"
        txDate.text = transactionRecord.date?.let { DateHelper.getRelativeDateString(itemView.context, it) }
        val addressExcerpt = TextHelper.randomHashGenerator().take(6) + "\u2026"//todo replace after from starts to show address (transactionRecord.from)
        txValueInFiat.text = "\$${NumberFormatHelper.fiatAmountFormat.format(transactionRecord.currencyAmount?.value)}" + " from " + addressExcerpt
        statusIcon.setImageDrawable(getStatusIcon(transactionRecord.status))
    }

    private fun getStatusIcon(status: TransactionRecordViewItem.Status?): Drawable? {
        return if (status == TransactionRecordViewItem.Status.SUCCESS)
            LayoutHelper.getTintedDrawable(R.drawable.checkmark, R.color.grey, App.instance)
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
            is ViewHolderFilter -> holder.bind(filters[position].name, active = selectedFilterId == filters[position].adapterId) {
                listener.onFilterItemClick(filters[position])
                selectedFilterId = filters[position].adapterId
                notifyDataSetChanged()
            }
        }
    }

}

class ViewHolderFilter(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(filterName: String, active: Boolean, onClick: () -> (Unit)) {
        filter_text.setOnClickListener { onClick.invoke() }

        filter_text.text = filterName
        filter_text.isActivated = active
    }
}
