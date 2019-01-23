package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.FullTransactionItem
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.dataprovider.DataProviderSettingsModule
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_full_transaction_info.*
import kotlinx.android.synthetic.main.view_holder_full_transaction.*
import kotlinx.android.synthetic.main.view_holder_full_transaction_item.*
import kotlinx.android.synthetic.main.view_holder_full_transaction_provider.*

class FullTransactionInfoActivity : BaseActivity(), FullTransactionInfoErrorFragment.Listener {

    private val transactionRecordAdapter = SectionViewAdapter(this)
    private lateinit var viewModel: FullTransactionInfoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val transactionHash = intent.extras.getString(transactionHashKey)
        val coinCodeString = intent.extras.getString(coinCodeKey)

        viewModel = ViewModelProviders.of(this).get(FullTransactionInfoViewModel::class.java)
        viewModel.init(transactionHash, coinCodeString)

        setContentView(R.layout.activity_full_transaction_info)

        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.FullInfo_Title)

        closeBtn.setOnClickListener { onBackPressed() }
        shareBtn.setOnClickListener { viewModel.share() }

        //
        // LiveData
        //
        viewModel.reloadLiveEvent.observe(this, Observer {
            recyclerTransactionInfo.visibility = View.VISIBLE
            transactionRecordAdapter.notifyDataSetChanged()
        })

        viewModel.loadingLiveData.observe(this, Observer { coinCode ->
            if (coinCode == true) {
                progressLoading.visibility = View.VISIBLE
                recyclerTransactionInfo.visibility = View.INVISIBLE
                transactionRecordAdapter.notifyDataSetChanged()
            } else {
                progressLoading.visibility = View.INVISIBLE
            }
        })

        viewModel.showCopiedLiveEvent.observe(this, Observer {
            HudHelper.showSuccessMessage(R.string.Hud_Text_Copied)
        })

        viewModel.openLinkLiveEvent.observe(this, Observer { url ->
            url?.let {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
            }
        })

        viewModel.openProviderSettingsEvent.observe(this, Observer { data ->
            data?.let { (coinCode, transactionHash) ->
                DataProviderSettingsModule.start(this, coinCode, transactionHash)
            }
        })

        viewModel.showErrorLiveEvent.observe(this, Observer { error ->
            error?.let { (show, providerName) ->
                if (show && providerName != null) {
                    errorContainer.visibility = View.VISIBLE

                    val fragment = FullTransactionInfoErrorFragment.newInstance(providerName)
                    val transaction = supportFragmentManager.beginTransaction()

                    transaction.replace(R.id.errorContainer, fragment)
                    transaction.commit()
                } else {
                    errorContainer.visibility = View.INVISIBLE
                }
            }
        })

        viewModel.showShareLiveEvent.observe(this, Observer { url ->
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, url)
                type = "text/plain"
            }
            startActivity(sendIntent)
        })

        recyclerTransactionInfo.hasFixedSize()
        recyclerTransactionInfo.adapter = transactionRecordAdapter
        recyclerTransactionInfo.layoutManager = LinearLayoutManager(this)

        transactionRecordAdapter.viewModel = viewModel
    }

    //
    // FullTransactionInfoErrorFragment Listener
    //
    override fun onRetry() {
        viewModel.retry()
    }

    override fun onChangeProvider() {
        viewModel.changeProvider()
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
                    holder.sectionRecyclerView.hasFixedSize()
                    holder.sectionRecyclerView.isNestedScrollingEnabled = false

                    holder.sectionRecyclerView.layoutManager = LinearLayoutManager(context)
                    holder.sectionRecyclerView.adapter = SectionItemViewAdapter(context, viewModel, section.items)
                }

            }
            is SectionProviderViewHolder -> {
                val providerName = viewModel.delegate.providerName
                holder.sectionProvider.bind(title = context.getString(R.string.FullInfo_Source), value = providerName, dimmed = false, icon = null)
                holder.sectionProvider.setOnClickListener {
                    viewModel.delegate.onTapProvider()
                }

                providerName?.let {
                    val changeProviderStyle = SpannableString(providerName)
                    changeProviderStyle.setSpan(UnderlineSpan(), 0, changeProviderStyle.length, 0)

                    holder.providerSite.text = changeProviderStyle
                    holder.providerSite.setOnClickListener {
                        viewModel.delegate.onTapResource()
                    }
                }
            }
        }
    }
}

class SectionItemViewAdapter(val context: Context, val viewModel: FullTransactionInfoViewModel, val items: List<FullTransactionItem>)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
                val notLast = items.size != position + 1

                bindTransaction(item, notLast, holder.sectionItem)
            }
        }
    }

    private fun bindTransaction(item: FullTransactionItem, showBorder: Boolean, viewItem: FullTransactionInfoItemView) {
        val title = if (item.titleResId != null) context.getString(item.titleResId) else item.title

        viewItem.bind(title, item.value, item.icon, item.dimmed, showBorder)

        if (item.clickable) {
            viewItem.setOnClickListener {
                viewModel.delegate.onTapItem(item)
            }
        }
    }
}

class SectionViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class SectionProviderViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class SectionItemViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
