package io.horizontalsystems.bankwallet.modules.market.defi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.market.top.*
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartFragment
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_rates.*

class MarketDefiFragment : BaseFragment(), MarketTopHeaderAdapter.Listener, ViewHolderMarketTopItem.Listener {

    private lateinit var marketTopHeaderAdapter: MarketTopHeaderAdapter
    private lateinit var marketTopItemsAdapter: MarketTopItemsAdapter
    private lateinit var marketLoadingAdapter: MarketLoadingAdapter

    private val marketTopViewModel by viewModels<MarketTopViewModel> { MarketDefiModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_rates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        marketTopHeaderAdapter = MarketTopHeaderAdapter(this, marketTopViewModel, viewLifecycleOwner)
        marketTopItemsAdapter = MarketTopItemsAdapter(
                this,
                marketTopViewModel.marketTopViewItemsLiveData,
                marketTopViewModel.loadingLiveData,
                marketTopViewModel.errorLiveData,
                viewLifecycleOwner,
        )
        marketLoadingAdapter = MarketLoadingAdapter(marketTopViewModel, viewLifecycleOwner)

        coinRatesRecyclerView.adapter = ConcatAdapter(marketTopHeaderAdapter, marketLoadingAdapter, marketTopItemsAdapter)
        coinRatesRecyclerView.itemAnimator = null

        pullToRefresh.setOnRefreshListener {
            marketTopViewModel.refresh()

            pullToRefresh.isRefreshing = false
        }

        marketTopViewModel.networkNotAvailable.observe(viewLifecycleOwner, {
            HudHelper.showErrorMessage(requireView(), R.string.Hud_Text_NoInternet)
        })
    }

    override fun onClickSortingField() {
        val items = marketTopViewModel.sortingFields.map {
            SelectorItem(getString(it.titleResId), it == marketTopViewModel.sortingField)
        }

        SelectorDialog
                .newInstance(items, getString(R.string.Market_Sort_PopupTitle)) { position ->
                    marketTopViewModel.sortingField = marketTopViewModel.sortingFields[position]
                }
                .show(childFragmentManager, "sorting_field_selector")
    }

    override fun onClickPeriod() {
        val items = marketTopViewModel.periods.map {
            SelectorItem(getString(it.titleResId), it == marketTopViewModel.period)
        }

        SelectorDialog
                .newInstance(items, getString(R.string.Market_Period_PopupTitle)) { position ->
                    marketTopViewModel.period = marketTopViewModel.periods[position]
                }
                .show(childFragmentManager, "sorting_period_selector")
    }


    override fun onItemClick(marketTopViewItem: MarketTopViewItem) {
        val arguments = RateChartFragment.prepareParams(marketTopViewItem.coinCode, marketTopViewItem.coinName, null, marketTopViewItem.coinType)

        findNavController().navigate(R.id.rateChartFragment, arguments, navOptions())
    }
}
