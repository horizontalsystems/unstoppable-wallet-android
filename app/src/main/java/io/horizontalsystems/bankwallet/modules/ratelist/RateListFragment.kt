package io.horizontalsystems.bankwallet.modules.ratelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.core.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.cryptonews.*
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartFragment
import kotlinx.android.synthetic.main.fragment_rates.*

class RatesListFragment : BaseFragment(), CoinRatesAdapter.Listener {

    private lateinit var coinRatesHeaderAdapter: CoinRatesHeaderAdapter
    private lateinit var coinRatesAdapter: CoinRatesAdapter
    private lateinit var cryptoNewsAdapter: CryptoNewsAdapter
    private lateinit var cryptoNewsHeaderAdapter: CryptoNewsHeaderAdapter
    private lateinit var cryptoNewsPresenter: CryptoNewsPresenter
    private val presenter: RateListPresenter by activityViewModels { RateListModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_rates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        coinRatesHeaderAdapter = CoinRatesHeaderAdapter(false, getString(R.string.RateList_portfolio))
        coinRatesAdapter = CoinRatesAdapter(this)
        cryptoNewsHeaderAdapter = CryptoNewsHeaderAdapter()
        cryptoNewsAdapter = CryptoNewsAdapter()

        coinRatesRecyclerView.itemAnimator = null
        coinRatesRecyclerView.adapter = ConcatAdapter(coinRatesHeaderAdapter, coinRatesAdapter, cryptoNewsHeaderAdapter, cryptoNewsAdapter, SourceAdapter())

        presenter.viewDidLoad()
        observeView(presenter.view)
        observeRouter(presenter.router)

        cryptoNewsPresenter = ViewModelProvider(this, CryptoNewsModule.Factory("")).get(CryptoNewsPresenter::class.java)
        observeCryptoNewsView(cryptoNewsPresenter.view)
        cryptoNewsPresenter.onLoad()
    }

    override fun onCoinClicked(coinItem: CoinItem) {
        presenter.onCoinClicked(coinItem)
    }

    private fun observeView(view: RateListView) {
        view.datesLiveData.observe(viewLifecycleOwner, Observer { lastUpdateTimestamp ->
            coinRatesHeaderAdapter.timestamp = lastUpdateTimestamp
        })

        view.portfolioViewItemsLiveData.observe(viewLifecycleOwner, Observer { viewItems ->
            coinRatesAdapter.submitList(viewItems)
        })
    }

    private fun observeRouter(router: RateListRouter) {
        router.openChartLiveEvent.observe(viewLifecycleOwner, Observer { (coinCode, coinTitle) ->
            val arguments = RateChartFragment.prepareParams(coinCode, coinTitle, null)

            findNavController().navigate(R.id.lockScreenFragment_to_rateChartFragment, arguments, navOptions())
        })
    }

    private fun observeCryptoNewsView(cryptoNewsView: CryptoNewsView) {
        cryptoNewsView.showNews.observe(viewLifecycleOwner, Observer { items ->
            cryptoNewsAdapter.submitList(items)
        })

        cryptoNewsView.showSpinner.observe(viewLifecycleOwner, Observer { show ->
            cryptoNewsHeaderAdapter.bind(show, false)
        })

        cryptoNewsView.showError.observe(viewLifecycleOwner, Observer {
            cryptoNewsHeaderAdapter.bind(false, true)
        })
    }
}
