package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.dataprovider

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.views.TopMenuItem
import io.horizontalsystems.views.ViewHolderProgressbar
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_explorer_switcher.*
import kotlinx.android.synthetic.main.view_holder_explorer_item.*

class DataProviderSettingsActivity : BaseActivity(), DataProviderSettingsAdapter.Listener {

    private var adapter: DataProviderSettingsAdapter? = null

    private lateinit var viewModel: DataProviderSettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val coin = intent.getParcelableExtra<Coin>(DataProviderSettingsModule.COIN_STRING) ?: run { finish(); return }
        val txHash = intent.getStringExtra(DataProviderSettingsModule.TRANSACTION_HASH) ?: run { finish(); return }
        viewModel = ViewModelProvider(this).get(DataProviderSettingsViewModel::class.java)
        viewModel.init(coin, txHash)

        setContentView(R.layout.activity_explorer_switcher)

        shadowlessToolbar.bind(
                title = getString(R.string.FullInfo_Source),
                leftBtnItem = TopMenuItem(R.drawable.ic_back, onClick = { onBackPressed() })
        )

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
            is ViewHolderDataProviderSettings -> holder.bind(items[position], showBottomShade = (position == itemCount - 1)) { listener.onChangeProvider(items[position]) }
        }
    }

}

class ViewHolderDataProviderSettings(private val context: Context, override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: DataProviderSettingsItem, showBottomShade: Boolean, onClick: () -> (Unit)) {

        containerView.setOnSingleClickListener { onClick.invoke() }
        title.text = item.name
        subtitle.text = context.getString(if (item.online) R.string.FullInfo_Source_Online else R.string.FullInfo_Source_Offline)

        if (item.online) {
            subtitle.setTextColor(ContextCompat.getColor(subtitle.context, R.color.green_d))
        } else {
            subtitle.setTextColor(ContextCompat.getColor(subtitle.context, R.color.red_d))
        }

        if (item.checking) {
            statusChecking.visibility = View.VISIBLE
            subtitle.visibility = View.GONE
        } else {
            subtitle.visibility = View.VISIBLE
            statusChecking.visibility = View.GONE
        }

        checkmarkIcon.visibility = if (item.selected) View.VISIBLE else View.GONE
        bottomShade.visibility = if (showBottomShade) View.VISIBLE else View.GONE
    }

    companion object {
        val layoutResourceId = R.layout.view_holder_explorer_item
    }

}
