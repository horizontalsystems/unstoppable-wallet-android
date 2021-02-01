package io.horizontalsystems.bankwallet.modules.market.favorites

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
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_market_favorites.*

class MarketFavoritesFragment : BaseFragment(), MarketListHeaderView.Listener, ViewHolderMarketTopItem.Listener {

    private lateinit var marketTopItemsAdapter: MarketTopItemsAdapter
    private lateinit var marketLoadingAdapter: MarketLoadingAdapter

    private val marketTopViewModel by viewModels<MarketTopViewModel> { MarketFavoritesModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_market_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        marketListHeader.listener = this
        marketListHeader.setSortingField(marketTopViewModel.sortingField)
        marketListHeader.setMarketField(marketTopViewModel.marketField)

        marketTopItemsAdapter = MarketTopItemsAdapter(
                this,
                marketTopViewModel.marketTopViewItemsLiveData,
                marketTopViewModel.loadingLiveData,
                marketTopViewModel.errorLiveData,
                viewLifecycleOwner
        )
        marketLoadingAdapter = MarketLoadingAdapter(marketTopViewModel.loadingLiveData, marketTopViewModel.errorLiveData, marketTopViewModel::onErrorClick, viewLifecycleOwner)

        coinRatesRecyclerView.adapter = ConcatAdapter(marketLoadingAdapter, marketTopItemsAdapter)
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
                    val selectedSortingField = marketTopViewModel.sortingFields[position]

                    marketListHeader.setSortingField(selectedSortingField)
                    marketTopViewModel.update(sortingField = selectedSortingField)
                }
                .show(childFragmentManager, "sorting_field_selector")
    }

    override fun onSelectMarketField(marketField: MarketField) {
        marketTopViewModel.update(marketField = marketField)
    }

    override fun onItemClick(marketTopViewItem: MarketTopViewItem) {
        val arguments = RateChartFragment.prepareParams(marketTopViewItem.coinCode, marketTopViewItem.coinName, null)

        findNavController().navigate(R.id.rateChartFragment, arguments, navOptions())
    }
}
