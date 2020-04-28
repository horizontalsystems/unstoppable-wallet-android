package io.horizontalsystems.bankwallet.modules.ratelist

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
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.views.setCoinImage
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_rates.*
import kotlinx.android.synthetic.main.view_holder_coin_rate.*
import java.util.*

class RatesListFragment : Fragment() {

    private lateinit var adapter: CoinRatesAdapter
    private lateinit var presenter: RateListPresenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_rates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        presenter = ViewModelProvider(this, RateListModule.Factory()).get(RateListPresenter::class.java)
        observeView(presenter.view)
        presenter.viewDidLoad()

        adapter = CoinRatesAdapter()
        coinRatesRecyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        presenter.loadTopList()
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
}


class CoinRatesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
            coinViewItem -> ViewHolderCoin(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin_rate, parent, false))
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
            (holder as? ViewHolderCoin)?.bind(item.coinItem, item.last)
        }
    }

}

class ViewHolderSectionHeaderPortfolio(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class ViewHolderSectionHeaderTop100(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class ViewHolderLoadingSpinner(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class ViewHolderSource(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer

class ViewHolderCoin(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(coinItem: CoinItem, isLast: Boolean) {
        coinIcon.isVisible = coinItem.coin != null
        coinItem.coin?.code?.let { coinIcon.setCoinImage(it) }
        titleText.text = coinItem.coinName
        subtitleText.text = coinItem.coinCode

        txValueInFiat.isActivated = !coinItem.rateDimmed //change color via state: activated/not activated
        txValueInFiat.text = coinItem.rate ?: containerView.context.getString(R.string.NotAvailable)

        if (coinItem.diff != null) {
            txDiff.diff = coinItem.diff
            txDiff.visibility = View.VISIBLE
            txDiffNa.visibility = View.GONE
        } else {
            txDiff.visibility = View.GONE
            txDiffNa.visibility = View.VISIBLE
        }

        bottomShade.visibility = if (isLast) View.VISIBLE else View.GONE
    }
}
