package io.horizontalsystems.bankwallet.modules.market.advancedsearch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.databinding.FragmentMarketAdvancedSearchResultsBinding
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.market.MarketItemsAdapter
import io.horizontalsystems.bankwallet.modules.market.MarketLoadingAdapter
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.ViewHolderMarketItem
import io.horizontalsystems.bankwallet.modules.market.list.MarketListViewModel
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class MarketAdvancedSearchResultsFragment : BaseFragment(), MarketListHeaderView.Listener,
    ViewHolderMarketItem.Listener {

    private val marketSearchFilterViewModel by navGraphViewModels<MarketAdvancedSearchViewModel>(R.id.marketAdvancedSearchFragment)
    private val marketListViewModel by viewModels<MarketListViewModel> {
        MarketAdvancedSearchResultsModule.Factory(
            marketSearchFilterViewModel.service
        )
    }

    private var _binding: FragmentMarketAdvancedSearchResultsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarketAdvancedSearchResultsBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.marketListHeader.listener = this
        binding.marketListHeader.isVisible = false
        marketListViewModel.marketViewItemsLiveData.observe(viewLifecycleOwner, { (list, _) ->
            binding.marketListHeader.isVisible = list.isNotEmpty()
        })

        val marketItemsAdapter = MarketItemsAdapter(
            this,
            marketListViewModel.marketViewItemsLiveData,
            marketListViewModel.loadingLiveData,
            marketListViewModel.errorLiveData,
            viewLifecycleOwner
        )
        val marketLoadingAdapter = MarketLoadingAdapter(
            marketListViewModel.loadingLiveData,
            marketListViewModel.errorLiveData,
            marketListViewModel::onErrorClick,
            viewLifecycleOwner
        )

        binding.coinRatesRecyclerView.adapter =
            ConcatAdapter(marketLoadingAdapter, marketItemsAdapter)
        binding.coinRatesRecyclerView.itemAnimator = null

        binding.pullToRefresh.setOnRefreshListener {
            marketListViewModel.refresh()

            binding.pullToRefresh.isRefreshing = false
        }

        marketListViewModel.networkNotAvailable.observe(viewLifecycleOwner, {
            HudHelper.showErrorMessage(requireView(), R.string.Hud_Text_NoInternet)
        })

        marketListViewModel.topMenuLiveData.observe(viewLifecycleOwner) { (sortMenu, toggleButton) ->
            binding.marketListHeader.setMenu(sortMenu, toggleButton)
        }

    }

    override fun onSortingClick() {
        val items = marketListViewModel.getSortingMenuItems()

        SelectorDialog
            .newInstance(items, getString(R.string.Market_Sort_PopupTitle)) { position ->
                val selectedSortingField = marketListViewModel.sortingFields[position]
                marketListViewModel.updateSorting(selectedSortingField)
            }
            .show(childFragmentManager, "sorting_field_selector")
    }

    override fun onToggleButtonClick() {
        marketListViewModel.onToggleButtonClick()
    }

    override fun onItemClick(marketViewItem: MarketViewItem) {
        val arguments = CoinFragment.prepareParams(marketViewItem.coinUid)

        findNavController().navigate(R.id.coinFragment, arguments, navOptions())
    }
}
