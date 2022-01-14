package io.horizontalsystems.bankwallet.modules.market.advancedsearch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.databinding.FragmentMarketSearchFilterBinding
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellowWithSpinner
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetMarketSearchFilterSelectDialog
import io.horizontalsystems.bankwallet.ui.selector.ViewItemWrapper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class MarketAdvancedSearchFragment : BaseFragment() {

    private val marketAdvancedSearchViewModel by navGraphViewModels<MarketAdvancedSearchViewModel>(R.id.marketAdvancedSearchFragment) {
        MarketAdvancedSearchModule.Factory()
    }

    private var _binding: FragmentMarketSearchFilterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarketSearchFilterBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuReset -> {
                    marketAdvancedSearchViewModel.reset()
                    true
                }
                else -> false
            }
        }

        marketAdvancedSearchViewModel.coinListViewItemLiveData.observe(viewLifecycleOwner) {
            binding.filterCoinList.setValueColored(it.title, it.color)
        }
        binding.filterCoinList.setOnSingleClickListener {
            showSelectorDialog(
                title = R.string.Market_Filter_ChooseSet,
                headerIcon = R.drawable.ic_circle_coin_24,
                items = marketAdvancedSearchViewModel.coinListsViewItemOptions,
                selectedItem = marketAdvancedSearchViewModel.coinListViewItem,
            ) {
                marketAdvancedSearchViewModel.coinListViewItem = it
            }
        }

        marketAdvancedSearchViewModel.marketCapViewItemLiveData.observe(viewLifecycleOwner) {
            binding.filterMarketCap.setValueColored(it.title, it.color)
        }
        binding.filterMarketCap.setOnSingleClickListener {
            showSelectorDialog(
                title = R.string.Market_Filter_MarketCap,
                headerIcon = R.drawable.ic_usd_24,
                items = marketAdvancedSearchViewModel.marketCapViewItemOptions,
                selectedItem = marketAdvancedSearchViewModel.marketCapViewItem,
            ) {
                marketAdvancedSearchViewModel.marketCapViewItem = it
            }
        }

        marketAdvancedSearchViewModel.volumeViewItemLiveData.observe(viewLifecycleOwner) {
            binding.filterVolume.setValueColored(it.title, it.color)
        }
        binding.filterVolume.setOnSingleClickListener {
            showSelectorDialog(
                title = R.string.Market_Filter_Volume,
                subtitleText = getString(R.string.TimePeriod_24h),
                headerIcon = R.drawable.ic_chart_24,
                items = marketAdvancedSearchViewModel.volumeViewItemOptions,
                selectedItem = marketAdvancedSearchViewModel.volumeViewItem,
            ) {
                marketAdvancedSearchViewModel.volumeViewItem = it
            }
        }

        marketAdvancedSearchViewModel.periodViewItemLiveData.observe(viewLifecycleOwner) {
            binding.filterPeriod.setValueColored(it.title, it.color)
        }
        binding.filterPeriod.setOnSingleClickListener {
            showSelectorDialog(
                title = R.string.Market_Filter_PricePeriod,
                headerIcon = R.drawable.ic_circle_clock_24,
                items = marketAdvancedSearchViewModel.periodViewItemOptions,
                selectedItem = marketAdvancedSearchViewModel.periodViewItem,
            ) {
                marketAdvancedSearchViewModel.periodViewItem = it
            }
        }

        marketAdvancedSearchViewModel.priceChangeViewItemLiveData.observe(viewLifecycleOwner) {
            binding.filterPriceChange.setValueColored(it.title, it.color)
        }
        binding.filterPriceChange.setOnSingleClickListener {
            showSelectorDialog(
                title = R.string.Market_Filter_PriceChange,
                headerIcon = R.drawable.ic_market_24,
                items = marketAdvancedSearchViewModel.priceChangeViewItemOptions,
                selectedItem = marketAdvancedSearchViewModel.priceChangeViewItem,
            ) {
                marketAdvancedSearchViewModel.priceChangeViewItem = it
            }
        }

        marketAdvancedSearchViewModel.outperformedBtcOnFilter.observe(
            viewLifecycleOwner,
            { checked ->
                binding.filterOutperformedBtc.setChecked(checked)
            })
        binding.filterOutperformedBtc.onCheckedChange { checked ->
            marketAdvancedSearchViewModel.outperformedBtcOn = checked
        }

        marketAdvancedSearchViewModel.outperformedEthOnFilter.observe(
            viewLifecycleOwner,
            { checked ->
                binding.filterOutperformedEth.setChecked(checked)
            })
        binding.filterOutperformedEth.onCheckedChange { checked ->
            marketAdvancedSearchViewModel.outperformedEthOn = checked
        }

        marketAdvancedSearchViewModel.outperformedBnbOnFilter.observe(
            viewLifecycleOwner,
            { checked ->
                binding.filterOutperformedBnb.setChecked(checked)
            })
        binding.filterOutperformedBnb.onCheckedChange { checked ->
            marketAdvancedSearchViewModel.outperformedBnbOn = checked
        }

        marketAdvancedSearchViewModel.priceCloseToAthFilter.observe(viewLifecycleOwner, { checked ->
            binding.filterPriceCloseToAth.setChecked(checked)
        })
        binding.filterPriceCloseToAth.onCheckedChange { checked ->
            marketAdvancedSearchViewModel.priceCloseToAth = checked
        }

        marketAdvancedSearchViewModel.priceCloseToAtlFilter.observe(viewLifecycleOwner, { checked ->
            binding.filterPriceCloseToAtl.setChecked(checked)
        })
        binding.filterPriceCloseToAtl.onCheckedChange { checked ->
            marketAdvancedSearchViewModel.priceCloseToAtl = checked
        }

        marketAdvancedSearchViewModel.updateResultButton.observe(
            viewLifecycleOwner,
            { (title, showSpinner, enabled) ->
                setButton(title, showSpinner, enabled)
            })

        marketAdvancedSearchViewModel.errorLiveEvent.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireView(), it)
        }

        // Dispose the Composition when viewLifecycleOwner is destroyed
        binding.submitButtonCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        setButton()
    }

    private fun setButton(
        title: String = getString(R.string.Market_Filter_ShowResults),
        showSpinner: Boolean = false,
        enabled: Boolean = false
    ) {
        binding.submitButtonCompose.setContent {
            ComposeAppTheme {
                ButtonPrimaryYellowWithSpinner(
                    modifier = Modifier.padding(start = 16.dp, bottom = 24.dp, end = 16.dp),
                    title = title,
                    onClick = {
                        findNavController().navigate(
                            R.id.marketAdvancedSearchFragment_to_marketAdvancedSearchFragmentResults,
                            null,
                            navOptions()
                        )
                    },
                    showSpinner = showSpinner,
                    enabled = enabled

                )
            }
        }
    }

    private fun <ItemClass> showSelectorDialog(
        title: Int,
        subtitleText: String = "---------",
        headerIcon: Int,
        items: List<ViewItemWrapper<ItemClass>>,
        selectedItem: ViewItemWrapper<ItemClass>,
        onSelectListener: (ViewItemWrapper<ItemClass>) -> Unit
    ) {
        val dialog = BottomSheetMarketSearchFilterSelectDialog<ItemClass>()
        dialog.titleText = getString(title)
        dialog.subtitleText = subtitleText
        dialog.headerIconResourceId = headerIcon
        dialog.items = items
        dialog.selectedItem = selectedItem
        dialog.onSelectListener = onSelectListener

        dialog.show(childFragmentManager, "selector_dialog")
    }
}
