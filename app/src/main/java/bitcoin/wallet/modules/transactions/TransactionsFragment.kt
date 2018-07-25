package bitcoin.wallet.modules.transactions

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import bitcoin.wallet.R
import bitcoin.wallet.viewHelpers.DateHelper
import bitcoin.wallet.viewHelpers.NumberFormatHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_transactions.*
import kotlinx.android.synthetic.main.view_holder_filter.*
import kotlinx.android.synthetic.main.view_holder_transaction.*

class TransactionsFragment : android.support.v4.app.Fragment() {

    private lateinit var viewModel: TransactionsViewModel
    private val transactionsAdapter = TransactionsAdapter()
    private val filterAdapter = FilterAdapter()

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
            }
        })

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setTitle(R.string.tab_title_transactions)
        recyclerTransactions.adapter = transactionsAdapter
        recyclerTransactions.layoutManager = LinearLayoutManager(context)

        recyclerTags.adapter = filterAdapter
        recyclerTags.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }
}

class TransactionsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = listOf<TransactionRecordViewItem>()

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolderTransaction(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_transaction, parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderTransaction -> holder.bind(items[position])
        }
    }

}

class ViewHolderTransaction(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(transactionRecord: TransactionRecordViewItem) {
        val sign = if (transactionRecord.incoming) "+" else "-"
        val amountTextColor = if (transactionRecord.incoming) R.color.green_crypto else R.color.yellow_crypto
        txAmount.setTextColor(ContextCompat.getColor(itemView.context, amountTextColor))
        txAmount.text = "$sign ${NumberFormatHelper.cryptoAmountFormat.format(Math.abs(transactionRecord.amount.value))} ${transactionRecord.amount.coin.code}"
        txDate.text = DateHelper.getRelativeDateString(itemView.context, transactionRecord.date)
        txValueInFiat.text = "\$${transactionRecord.valueInBaseCurrency} when " + (if (transactionRecord.incoming) "received" else "sent")
    }
}

class FilterAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var filters = listOf("All", "Bitcon", "Bitcon Cash", "Ethereum", "Litecoin")

    override fun getItemCount() = filters.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolderFilter(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_filter, parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderFilter -> holder.bind(filters[position], active = position == 0)
        }
    }

}

class ViewHolderFilter(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(filterName: String, active: Boolean) {
        filter_text.text = filterName
        filter_text.isActivated = active
    }
}
