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
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartActivity
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.views.setCoinImage
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_rates.*
import kotlinx.android.synthetic.main.view_holder_coin_rate.*
import java.util.*

class RatesListFragment : Fragment(), CoinRatesAdapter.Listener {

    private lateinit var adapter: CoinRatesAdapter
    private lateinit var presenter: RateListPresenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_rates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        presenter = ViewModelProvider(this, RateListModule.Factory()).get(RateListPresenter::class.java)
        observeView(presenter.view)
        observeRouter(presenter.router)
        presenter.viewDidLoad()

        adapter = CoinRatesAdapter(this)
        coinRatesRecyclerView.adapter = adapter
    }

    override fun onCoinClicked(coinViewItem: ViewItem.CoinViewItem) {
        presenter.onCoinClicked(coinViewItem)
    }

    private fun observeView(view: RateListView) {
        view.datesLiveData.observe(viewLifecycleOwner, Observer { lastUpdateTimestamp ->
            val dateAndTime = DateHelper.getDayAndTime(Date(lastUpdateTimestamp * 1000))
            timeAgoText.text = dateAndTime
        })

        view.viewItemsLiveData.observe(viewLifecycleOwner, Observer { viewItems ->
            adapter.viewItems = viewItems
            adapter.notifyDataSetChanged()
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

    private val portfolioHeader = 0
    private val coinViewItem = 1
    private val topListHeader = 2
    private val loadingSpinner = 3
    private val sourceView = 4

    override fun getItemCount(): Int = viewItems.size

    override fun getItemViewType(position: Int): Int {
        return when (viewItems[position]) {
            is ViewItem.CoinViewItem -> coinViewItem
            ViewItem.PortfolioHeader -> portfolioHeader
            ViewItem.TopListHeader -> topListHeader
            ViewItem.LoadingSpinner -> loadingSpinner
            ViewItem.SourceText -> sourceView
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            coinViewItem -> ViewHolderCoin(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin_rate, parent, false), listener)
            portfolioHeader -> ViewHolderSectionHeaderPortfolio(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin_section_header_portfolio, parent, false))
            topListHeader -> ViewHolderSectionHeaderTop100(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin_section_header_top_100, parent, false))
            loadingSpinner -> ViewHolderLoadingSpinner(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_rate_list_spinner, parent, false))
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
