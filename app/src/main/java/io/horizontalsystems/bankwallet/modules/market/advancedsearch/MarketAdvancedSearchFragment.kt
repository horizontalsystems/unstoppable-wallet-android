package io.horizontalsystems.bankwallet.modules.market.advancedsearch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.ui.selector.*
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_market_search_filter.*

class MarketAdvancedSearchFragment : BaseFragment() {

    private val marketSearchFilterViewModel by viewModels<MarketAdvancedSearchViewModel> { MarketAdvancedSearchModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_market_search_filter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuClose -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        filterCoinList.setValueColored(marketSearchFilterViewModel.coinList.title, marketSearchFilterViewModel.coinList.color)
        filterCoinList.setOnSingleClickListener {
            showSelectorDialog(
                    title = R.string.Market_Filter_CoinList,
                    subtitleText = "Range",
                    headerIcon = R.drawable.ic_circle_coin_24,
                    items = marketSearchFilterViewModel.coinLists,
                    selectedItem = marketSearchFilterViewModel.coinList,
                    itemViewHolderFactory = SelectorItemViewHolderFactory()
            ) {
                marketSearchFilterViewModel.coinList = it
                filterCoinList.setValueColored(it.title, it.color)
            }
        }

        filterMarketCap.setValueColored(marketSearchFilterViewModel.marketCap.title, marketSearchFilterViewModel.marketCap.color)
        filterMarketCap.setOnSingleClickListener {
            showSelectorDialog(
                    title = R.string.Market_Filter_MarketCap,
                    subtitleText = "Range",
                    headerIcon = R.drawable.ic_usd_24,
                    items = marketSearchFilterViewModel.marketCaps,
                    selectedItem = marketSearchFilterViewModel.marketCap,
                    itemViewHolderFactory = SelectorItemViewHolderFactory()
            ) {
                marketSearchFilterViewModel.marketCap = it
                filterMarketCap.setValueColored(it.title, it.color)
            }
        }

        filterVolume.setValueColored(marketSearchFilterViewModel.volume.title, marketSearchFilterViewModel.volume.color)
        filterVolume.setOnSingleClickListener {
            showSelectorDialog(
                    title = R.string.Market_Filter_Volume,
                    subtitleText = "Range",
                    headerIcon = R.drawable.ic_chart_24,
                    items = marketSearchFilterViewModel.volumes,
                    selectedItem = marketSearchFilterViewModel.volume,
                    itemViewHolderFactory = SelectorItemViewHolderFactory()
            ) {
                marketSearchFilterViewModel.volume = it
                filterVolume.setValueColored(it.title, it.color)
            }
        }

        filterLiquidity.setValueColored(marketSearchFilterViewModel.liquidity.title, marketSearchFilterViewModel.liquidity.color)
        filterLiquidity.setOnSingleClickListener {
            showSelectorDialog(
                    title = R.string.Market_Filter_Liquidity,
                    subtitleText = "Range",
                    headerIcon = R.drawable.ic_circle_check_24,
                    items = marketSearchFilterViewModel.liquidities,
                    selectedItem = marketSearchFilterViewModel.liquidity,
                    itemViewHolderFactory = SelectorItemViewHolderFactory()
            ) {
                marketSearchFilterViewModel.liquidity = it
                filterLiquidity.setValueColored(it.title, it.color)
            }
        }

        filterPeriod.setValueColored(marketSearchFilterViewModel.period.title, marketSearchFilterViewModel.period.color)
        filterPeriod.setOnSingleClickListener {
            showSelectorDialog(
                    title = R.string.Market_Filter_Period,
                    subtitleText = "Range",
                    headerIcon = R.drawable.ic_circle_clock_24,
                    items = marketSearchFilterViewModel.periods,
                    selectedItem = marketSearchFilterViewModel.period,
                    itemViewHolderFactory = SelectorItemViewHolderFactory()
            ) {
                marketSearchFilterViewModel.period = it
                filterPeriod.setValueColored(it.title, it.color)
            }
        }

        submit.setOnSingleClickListener {
            marketSearchFilterViewModel.showResults()
        }

    }

    private fun <ItemClass> showSelectorDialog(title: Int, subtitleText: String, headerIcon: Int, items: List<ItemClass>, selectedItem: ItemClass, itemViewHolderFactory: ItemViewHolderFactory<ItemViewHolder<ItemClass>>, onSelectListener: (ItemClass) -> Unit) {
        val dialog = SelectorBottomSheetDialog<ItemClass>()
        dialog.titleText = getString(title)
        dialog.subtitleText = subtitleText
        dialog.headerIconResourceId = headerIcon
        dialog.items = items
        dialog.selectedItem = selectedItem
        dialog.onSelectListener = onSelectListener
        dialog.itemViewHolderFactory = itemViewHolderFactory

        dialog.show(childFragmentManager, "selector_dialog")
    }
}


