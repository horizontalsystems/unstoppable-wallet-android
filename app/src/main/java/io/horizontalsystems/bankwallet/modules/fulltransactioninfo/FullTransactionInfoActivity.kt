package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.FullTransactionItem
import io.horizontalsystems.bankwallet.entities.FullTransactionIcon
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_full_transaction_info.*
import kotlinx.android.synthetic.main.view_holder_full_transaction.*
import kotlinx.android.synthetic.main.view_holder_full_transaction_item.*
import kotlinx.android.synthetic.main.view_holder_full_transaction_provider.*

class FullTransactionInfoActivity : BaseActivity() {

    private val transactionRecordAdapter = SectionViewAdapter(this)
    private lateinit var viewModel: FullTransactionInfoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val transactionHash = intent.extras.getString(transactionHashKey)
        val coinCode = intent.extras.getString(coinCodeKey)

        viewModel = ViewModelProviders.of(this).get(FullTransactionInfoViewModel::class.java)
        viewModel.init(transactionHash, coinCode)

        setContentView(R.layout.activity_full_transaction_info)

        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.FullInfo_Title)

        closeBtn.setOnClickListener { onBackPressed() }

        //
        // LiveData
        //
        viewModel.reloadLiveEvent.observe(this, Observer {
            recyclerTransactionInfo.visibility = View.VISIBLE
            transactionRecordAdapter.notifyDataSetChanged()
        })

        viewModel.loadingLiveData.observe(this, Observer { show ->
            progressLoading.visibility = if (show == true) View.VISIBLE else View.INVISIBLE
        })

        recyclerTransactionInfo.hasFixedSize()
        recyclerTransactionInfo.adapter = transactionRecordAdapter
        recyclerTransactionInfo.layoutManager = LinearLayoutManager(this)

        transactionRecordAdapter.viewModel = viewModel
    }

    companion object {
        const val transactionHashKey = "transaction_hash"
        const val coinCodeKey = "coin_code"

        fun start(context: Context, transactionHash: String, coinCode: CoinCode) {
            val intents = Intent(context, FullTransactionInfoActivity::class.java)
            intents.putExtra(transactionHashKey, transactionHash)
            intents.putExtra(coinCodeKey, coinCode)
            context.startActivity(intents)
        }
    }
}

class SectionViewAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    lateinit var viewModel: FullTransactionInfoViewModel

    private val sectionView = 1
    private val sectionViewProvider = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
        return if (viewType == sectionView) {
            SectionViewHolder(view.inflate(R.layout.view_holder_full_transaction, parent, false))
        } else {
            SectionProviderViewHolder(view.inflate(R.layout.view_holder_full_transaction_provider, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1) {
            sectionViewProvider
        } else {
            sectionView
        }
    }

    override fun getItemCount(): Int {
        return viewModel.delegate.sectionCount + 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SectionViewHolder -> {
                viewModel.delegate.getSection(position)?.let { section ->
                    if (section.title == null) {
                        holder.sectionLabel.setPadding(0, 0, 0, 0)
                    }

                    holder.sectionLabel.text = section.title
                    holder.sectionRecyclerView.hasFixedSize()
                    holder.sectionRecyclerView.isNestedScrollingEnabled = false

                    holder.sectionRecyclerView.layoutManager = LinearLayoutManager(context)
                    holder.sectionRecyclerView.adapter = SectionItemViewAdapter(context, section.items)
                }

            }
            is SectionProviderViewHolder -> {
                holder.sectionProvider.bind(title = viewModel.delegate.resource)
            }
        }
    }
}

class SectionItemViewAdapter(val context: Context, val items: List<FullTransactionItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.view_holder_full_transaction_item, parent, false)

        return SectionItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SectionItemViewHolder -> {
                val item = items[position]
                val last = items.size == position + 1

                val valueIcon = when (item.icon) {
                    FullTransactionIcon.HASH -> R.drawable.hash
                    FullTransactionIcon.PERSON -> R.drawable.round_person_18px
                    FullTransactionIcon.NONE -> null
                }

                holder.sectionItem.bind(
                        title = item.title,
                        valueTitle = item.value,
                        valueIcon = valueIcon,
                        showBottomBorder = !last)
            }
        }
    }
}

class SectionViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class SectionProviderViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class SectionItemViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer

