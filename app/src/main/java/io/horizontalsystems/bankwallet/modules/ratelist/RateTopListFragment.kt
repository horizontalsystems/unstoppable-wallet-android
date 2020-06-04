package io.horizontalsystems.bankwallet.modules.ratelist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.MergeAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartActivity
import kotlinx.android.synthetic.main.fragment_rates.*

class RatesTopListFragment : Fragment(), CoinRatesAdapter.Listener {

    private lateinit var coinRatesHeaderAdapter: CoinRatesHeaderAdapter
    private lateinit var coinRatesAdapter: CoinRatesAdapter
    private val presenter: RateListPresenter by activityViewModels { RateListModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_rates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        coinRatesHeaderAdapter = CoinRatesHeaderAdapter(getString(R.string.RateList_top100))
        coinRatesAdapter = CoinRatesAdapter(this)

        coinRatesRecyclerView.itemAnimator = null
        coinRatesRecyclerView.adapter = MergeAdapter(coinRatesHeaderAdapter, coinRatesAdapter)

        presenter.viewDidLoad()
        observeView(presenter.view)
        observeRouter(presenter.router)
    }

    override fun onCoinClicked(coinViewItem: ViewItem.CoinViewItem) {
        presenter.onCoinClicked(coinViewItem)
    }

    private fun observeView(view: RateListView) {
        view.datesLiveData.observe(viewLifecycleOwner, Observer { lastUpdateTimestamp ->
            coinRatesHeaderAdapter.timestamp = lastUpdateTimestamp
        })

        view.topViewItemsLiveData.observe(viewLifecycleOwner, Observer { viewItems ->
            coinRatesAdapter.submitList(viewItems)
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
