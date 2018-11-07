package io.horizontalsystems.bankwallet.modules.settings.basecurrency

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
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
import kotlinx.android.synthetic.main.activity_currency_switcher.*
import kotlinx.android.synthetic.main.view_holder_currency_item.*

class BaseCurrencySettingsActivity: BaseActivity(), CurrencySwitcherAdapter.Listener {

    private lateinit var viewModel: BaseCurrencySettingsViewModel
    private var adapter: CurrencySwitcherAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(BaseCurrencySettingsViewModel::class.java)
        viewModel.init()

        setContentView(R.layout.activity_currency_switcher)

        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.settings_currency_switcher_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.back)

        adapter = CurrencySwitcherAdapter(this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.currencyItems.observe(this, Observer { items ->
            items?.let {
                adapter?.items = it
                adapter?.notifyDataSetChanged()
            }
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

    override fun onItemClick(item: CurrencyItem) {
        viewModel.delegate.didSelect(item)
    }
}

class CurrencySwitcherAdapter(private var listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val VIEW_TYPE_ITEM = 1
    private val VIEW_TYPE_LOADING = 2

    interface Listener {
        fun onItemClick(item: CurrencyItem)
    }

    var items = listOf<CurrencyItem>()

    override fun getItemCount() = if (items.isEmpty()) 1 else items.size

    override fun getItemViewType(position: Int): Int = if(items.isEmpty()) {
        VIEW_TYPE_LOADING
    } else {
        VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_ITEM -> ViewHolderCurrency(inflater.inflate(ViewHolderCurrency.layoutResourceId, parent, false))
            else -> ViewHolderProgressbar(inflater.inflate(ViewHolderProgressbar.layoutResourceId, parent, false))
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderCurrency -> holder.bind(items[position]) { listener.onItemClick(items[position]) }
        }
    }

}

class ViewHolderCurrency(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: CurrencyItem, onClick: () -> (Unit)) {

        containerView.setOnSingleClickListener { onClick.invoke() }
        title.text = item.code
        subtitle.text = item.symbol
        checkmarkIcon.visibility = if (item.selected) View.VISIBLE else View.GONE
    }

    companion object {
        val layoutResourceId: Int
            get() = R.layout.view_holder_currency_item
    }

}
