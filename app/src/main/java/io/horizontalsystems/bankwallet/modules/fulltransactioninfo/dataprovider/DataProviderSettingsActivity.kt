package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.dataprovider

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.ui.view.ViewHolderProgressbar
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_explorer_switcher.*
import kotlinx.android.synthetic.main.view_holder_explorer_item.*

class DataProviderSettingsActivity : BaseActivity(), DataProviderSettingsAdapter.Listener {

    private var adapter: DataProviderSettingsAdapter? = null

    private lateinit var viewModel: DataProviderSettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val coinCode = intent.extras.getString(DataProviderSettingsModule.COIN_CODE)
        val txHash = intent.extras.getString(DataProviderSettingsModule.TRANSACTION_HASH)
        viewModel = ViewModelProviders.of(this).get(DataProviderSettingsViewModel::class.java)
        viewModel.init(coinCode, txHash)

        setContentView(R.layout.activity_explorer_switcher)

        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.FullInfo_Source)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.back)

        adapter = DataProviderSettingsAdapter(this)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.providerItems.observe(this, Observer { items ->
            items?.let {
                adapter?.items = it
                adapter?.notifyDataSetChanged()
            }
        })

        viewModel.closeLiveEvent.observe(this, Observer {
            finish()
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onChangeProvider(item: DataProviderSettingsItem) {
        viewModel.delegate.onSelect(item)
    }
}

class DataProviderSettingsAdapter(private var listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val VIEW_TYPE_ITEM = 1
    private val VIEW_TYPE_LOADING = 2

    interface Listener {
        fun onChangeProvider(item: DataProviderSettingsItem)
    }

    var items = listOf<DataProviderSettingsItem>()

    override fun getItemCount() = if (items.isEmpty()) 1 else items.size

    override fun getItemViewType(position: Int): Int = if (items.isEmpty()) {
        VIEW_TYPE_LOADING
    } else {
        VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_ITEM -> {
                val containerView = inflater.inflate(ViewHolderDataProviderSettings.layoutResourceId, parent, false)
                ViewHolderDataProviderSettings(parent.context, containerView)
            }
            else -> ViewHolderProgressbar(inflater.inflate(ViewHolderProgressbar.layoutResourceId, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderDataProviderSettings -> holder.bind(items[position]) { listener.onChangeProvider(items[position]) }
        }
    }

}

class ViewHolderDataProviderSettings(private val context: Context, override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: DataProviderSettingsItem, onClick: () -> (Unit)) {

        containerView.setOnSingleClickListener { onClick.invoke() }
        title.text = item.name
        subtitle.text = context.getString(if (item.online) R.string.FullInfo_Source_Online else R.string.FullInfo_Source_Offline)

        if (item.online) {
            subtitle.setTextColor(subtitle.resources.getColor(R.color.green_crypto))
        } else {
            subtitle.setTextColor(subtitle.resources.getColor(R.color.red_warning))
        }

        if (item.checking) {
            statusChecking.visibility = View.VISIBLE
            subtitle.visibility = View.GONE
        } else {
            subtitle.visibility = View.VISIBLE
            statusChecking.visibility = View.GONE
        }

        checkmarkIcon.visibility = if (item.selected) View.VISIBLE else View.GONE
    }

    companion object {
        val layoutResourceId: Int
            get() = R.layout.view_holder_explorer_item
    }

}
