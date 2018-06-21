package bitcoin.wallet.modules.transactions

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import bitcoin.wallet.R
import bitcoin.wallet.modules.main.BaseTabFragment
import bitcoin.wallet.viewHelpers.DateHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_transactions.*
import kotlinx.android.synthetic.main.view_holder_transaction.*

class TransactionsFragment : BaseTabFragment() {

    override val title: Int
        get() = R.string.tab_title_transactions

    private lateinit var viewModel: TransactionsViewModel
    private val transactionsAdapter = TransactionsAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(TransactionsViewModel::class.java)
        viewModel.init()

        viewModel.transactionItems.observe(this, Observer { transactionItems ->
            transactionItems?.let {
                transactionsAdapter.items = it + it //todo for test purpose, remove + it afterwards
                transactionsAdapter.notifyDataSetChanged()
            }
        })

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerTransactions.adapter = transactionsAdapter
        recyclerTransactions.layoutManager = LinearLayoutManager(context)
    }
}

class TransactionsViewModel : ViewModel(), TransactionsModule.IView, TransactionsModule.IRouter {

    override lateinit var presenter: TransactionsModule.IPresenter

    val transactionItems = MutableLiveData<List<TransactionViewItem>>()

    fun init() {
        TransactionsModule.initModule(this, this)
        presenter.start()
    }

    override fun showTransactionItems(items: List<TransactionViewItem>) {
        transactionItems.value = items
    }
}


class TransactionsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = listOf<TransactionViewItem>()

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

    fun bind(transaction: TransactionViewItem) {
        val sign = if (transaction.type == TransactionViewItem.Type.IN) "+" else "-"
        val amountTextColor = if (transaction.type == TransactionViewItem.Type.IN) R.color.green else R.color.grey
        txAmount.setTextColor(ContextCompat.getColor(itemView.context, amountTextColor))
        txAmount.text = "$sign ${transaction.amount} ${transaction.currency}"
        txStatus.text = transaction.status
        txDate.text = DateHelper.getRelativeDateString(itemView.context, transaction.date)
    }
}