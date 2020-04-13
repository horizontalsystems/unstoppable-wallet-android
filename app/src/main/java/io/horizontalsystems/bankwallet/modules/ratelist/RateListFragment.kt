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
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_rates.*
import kotlinx.android.synthetic.main.view_holder_coin_rate.*
import java.util.*

class RatesListFragment : Fragment() {

    private lateinit var adapter: CoinRatesAdapter
    private var presenter: RateListPresenter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_rates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        presenter = ViewModelProvider(this, RateListModule.Factory()).get(RateListPresenter::class.java)
        observeView(presenter?.view)
        presenter?.viewDidLoad()

        adapter = CoinRatesAdapter()
        coinRatesRecyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        presenter?.loadTopList(adapter.topListItems.size)
    }

    private fun observeView(view: RateListView?) {
        view?.datesLiveEvent?.observe(viewLifecycleOwner, Observer { (date, lastUpdateTimestamp)->
            dateText.text = DateHelper.formatDate(date, "MMM dd")
            setLastUpdatedTime(lastUpdateTimestamp)
        })

        view?.portfolioViewItems?.observe(viewLifecycleOwner, Observer { viewItems->
            adapter.portfolioItems = viewItems
            adapter.notifyDataSetChanged()
        })

        view?.topListViewItems?.observe(viewLifecycleOwner, Observer { viewItems ->
            adapter.topListItems = viewItems
            adapter.notifyDataSetChanged()
        })
    }

    private fun setLastUpdatedTime(lastUpdateTimestamp: Long?) {
        if (lastUpdateTimestamp == null)
            return

        val time = DateHelper.getOnlyTime(Date(lastUpdateTimestamp * 1000))
        timeAgoText.text = getString(R.string.RateList_updated, time)
    }
}


class CoinRatesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var portfolioItems = listOf<ViewItem>()
    var topListItems = listOf<ViewItem>()

    private val portfolioHeader = 0
    private val coinViewItem = 1
    private val topListHeader = 2
    private val loadingSpinner = 3
    private val sourceView = 4

    override fun getItemCount(): Int {
        var otherViewsCount = 1 //for loading spinner or for topListHeader
        if (portfolioItems.isNotEmpty()){
            otherViewsCount++
        }
        if (topListItems.isNotEmpty()){
            otherViewsCount++
        }
        return portfolioItems.size + topListItems.size + otherViewsCount
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position == 0 -> portfolioHeader
            topListItems.isEmpty() && position == portfolioItems.size + 1 -> loadingSpinner
            position == portfolioItems.size + 1 -> topListHeader
            topListItems.isNotEmpty() && position == itemCount - 1 -> sourceView
            else -> coinViewItem
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
        if (holder is ViewHolderCoin) {
            if (position <= portfolioItems.size) {
                holder.bind(portfolioItems[position-1], position == portfolioItems.size)
            } else if(topListItems.isNotEmpty()){
                holder.bind(topListItems[position - portfolioItems.size - 2], position == itemCount - 2)
            }
        }
    }

}

class ViewHolderSectionHeaderPortfolio(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class ViewHolderSectionHeaderTop100(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class ViewHolderLoadingSpinner(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class ViewHolderSource(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer

class ViewHolderCoin(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(viewItem: ViewItem, isLast: Boolean) {
        coinIcon.isVisible = viewItem.coin != null
        viewItem.coin?.code?.let { coinIcon.bind(it) }
        titleText.text = viewItem.coinName
        subtitleText.text = viewItem.coinCode

        txValueInFiat.isActivated = !viewItem.rateDimmed //change color via state: activated/not activated
        txValueInFiat.text = viewItem.rate ?: containerView.context.getString(R.string.NotAvailable)

        if (viewItem.diff != null){
            txDiff.diff = viewItem.diff
            txDiff.visibility = View.VISIBLE
            txDiffNa.visibility = View.GONE
        } else {
            txDiff.visibility = View.GONE
            txDiffNa.visibility = View.VISIBLE
        }

        bottomShade.visibility = if (isLast) View.VISIBLE else View.GONE
    }
}
