package io.horizontalsystems.bankwallet.modules.ratelist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.MergeAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartActivity
import io.horizontalsystems.views.setCoinImage
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_rates.*
import kotlinx.android.synthetic.main.view_holder_coin_rate.*

class RatesListFragment : Fragment(), CoinRatesAdapter.Listener {

    private lateinit var coinRatesAdapter: CoinRatesAdapter
    private lateinit var coinRatesHeaderAdapter: CoinRatesHeaderAdapter
    private lateinit var presenter: RateListPresenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_rates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        coinRatesHeaderAdapter = CoinRatesHeaderAdapter(getString(R.string.RateList_portfolio))
        coinRatesAdapter = CoinRatesAdapter(this)
        coinRatesRecyclerView.adapter = MergeAdapter(coinRatesHeaderAdapter, coinRatesAdapter)

        presenter = ViewModelProvider(this, RateListModule.Factory()).get(RateListPresenter::class.java)
        observeView(presenter.view)
        observeRouter(presenter.router)
        presenter.viewDidLoad()
    }

    override fun onCoinClicked(coinViewItem: ViewItem.CoinViewItem) {
        presenter.onCoinClicked(coinViewItem)
    }

    private fun observeView(view: RateListView) {
        view.datesLiveData.observe(viewLifecycleOwner, Observer { lastUpdateTimestamp ->
            coinRatesHeaderAdapter.timestamp = lastUpdateTimestamp
        })

        view.viewItemsLiveData.observe(viewLifecycleOwner, Observer { viewItems ->
            coinRatesAdapter.viewItems = viewItems
            coinRatesAdapter.notifyDataSetChanged()
        })
    }

    private fun observeRouter(router: RateListRouter) {
        router.openChartLiveEvent.observe(viewLifecycleOwner, Observer { (coinCode, coinTitle) ->
            startActivity(Intent(activity, RateChartActivity::class.java).apply {
                putExtra(ModuleField.COIN_CODE, coinCode)
                putExtra(ModuleField.COIN_TITLE, coinTitle)
            })
        })
    }
}


class CoinRatesAdapter(private val listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onCoinClicked(coinViewItem: ViewItem.CoinViewItem)
    }

    var viewItems = listOf<ViewItem>()

    private val coinViewItem = 1
    private val sourceView = 4

    override fun getItemCount(): Int = viewItems.size

    override fun getItemViewType(position: Int): Int {
        return when (viewItems[position]) {
            is ViewItem.CoinViewItem -> coinViewItem
            is ViewItem.SourceText -> sourceView
            else -> throw UnsupportedOperationException()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            coinViewItem -> ViewHolderCoin(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin_rate, parent, false), listener)
            sourceView -> ViewHolderSource(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin_list_source, parent, false))
            else -> throw Exception("No such view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = viewItems[position]
        if (item is ViewItem.CoinViewItem) {
            (holder as? ViewHolderCoin)?.bind(item)
        }
    }

}

class ViewHolderSectionHeaderPortfolio(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class ViewHolderSectionHeaderTop100(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class ViewHolderLoadingSpinner(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class ViewHolderSource(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer

class ViewHolderCoin(override val containerView: View, listener: CoinRatesAdapter.Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    private var coinViewItem: ViewItem.CoinViewItem? = null

    init {
        containerView.setOnClickListener {
            coinViewItem?.let {
                listener.onCoinClicked(it)
            }
        }
    }

    fun bind(viewItem: ViewItem.CoinViewItem) {
        this.coinViewItem = viewItem

        coinIcon.isVisible = viewItem.coinItem.coin != null
        viewItem.coinItem.coin?.code?.let { coinIcon.setCoinImage(it) }
        titleText.text = viewItem.coinItem.coinName
        subtitleText.text = viewItem.coinItem.coinCode

        txValueInFiat.isActivated = !viewItem.coinItem.rateDimmed //change color via state: activated/not activated
        txValueInFiat.text = viewItem.coinItem.rate ?: containerView.context.getString(R.string.NotAvailable)

        if (viewItem.coinItem.diff != null) {
            txDiff.diff = viewItem.coinItem.diff
            txDiff.visibility = View.VISIBLE
            txDiffNa.visibility = View.GONE
        } else {
            txDiff.visibility = View.GONE
            txDiffNa.visibility = View.VISIBLE
        }

        bottomShade.visibility = if (viewItem.last) View.VISIBLE else View.GONE
    }
}
