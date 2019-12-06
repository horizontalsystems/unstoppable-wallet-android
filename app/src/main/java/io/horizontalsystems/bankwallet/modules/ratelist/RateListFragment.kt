package io.horizontalsystems.bankwallet.modules.ratelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_rates.*
import kotlinx.android.synthetic.main.view_holder_coin_rate.*
import java.util.*

class RatesFragment : Fragment() {

    private lateinit var adapter: CoinRatesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_rates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val presenter = ViewModelProvider(this, RateListModule.Factory()).get(RateListPresenter::class.java)
        observeView(presenter.view)
        presenter.viewDidLoad()

        adapter = CoinRatesAdapter()
        coinRatesRecyclerView.adapter = adapter
    }

    private fun observeView(view: RateListView) {
        view.rateListViewItem.observe(viewLifecycleOwner, Observer { rateListViewItem->
            dateText.text = DateHelper.formatDate(rateListViewItem.currentDate, "MMM dd")
            setLastUpdatedTime(rateListViewItem.lastUpdateTimestamp)

            adapter.items = rateListViewItem.rateViewItems
            adapter.notifyDataSetChanged()
        })
    }

    private fun setLastUpdatedTime(lastUpdateTimestamp: Long?) {
        if (lastUpdateTimestamp == null)
            return

        val time = DateHelper.formatDate(Date(lastUpdateTimestamp * 1000), "HH:mm")
        timeAgoText.text = getString(R.string.RateList_updated, time)
    }
}


class CoinRatesAdapter : RecyclerView.Adapter<ViewHolderCoinRate>() {

    var items = listOf<RateViewItem>()

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderCoinRate {
        return ViewHolderCoinRate(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin_rate, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolderCoinRate, position: Int) {
        holder.bind(items[position], position == itemCount - 1)
    }

}

class ViewHolderCoinRate(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(viewItem: RateViewItem, isLast: Boolean) {
        coinIcon.bind(viewItem.coin)
        txCoinCode.text = viewItem.coin.code
        txCoinName.text = viewItem.coin.title

        txValueInFiat.text =  containerView.context.getString(R.string.NotAvailable)
        LayoutHelper.getAttr(R.attr.ColorLeah, containerView.context.theme)?.let { color ->
            txValueInFiat.setTextColor(color)
        }
        viewItem.rate?.let { rate ->
            val rateString = App.numberFormatter.format(rate, trimmable = true, canUseLessSymbol = false)
            txValueInFiat.text = rateString
            if (viewItem.rateExpired == true){
                txValueInFiat.setTextColor(ContextCompat.getColor(containerView.context, R.color.grey_50))
            }
        }

        val diff = viewItem.diff
        if (viewItem.rateExpired == false && diff != null){
            txDiff.diff = diff
            txDiff.visibility = View.VISIBLE
            txDiffNa.visibility = View.GONE
        } else {
            txDiff.visibility = View.GONE
            txDiffNa.visibility = View.VISIBLE
        }

        bottomShade.visibility = if (isLast) View.VISIBLE else View.GONE
    }
}
