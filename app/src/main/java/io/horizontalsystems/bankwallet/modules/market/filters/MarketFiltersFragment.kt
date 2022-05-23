package io.horizontalsystems.bankwallet.modules.market.filters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.market.filters.MarketFiltersModule.FilterDropdown.*
import io.horizontalsystems.bankwallet.modules.market.filters.MarketFiltersModule.FilterDropdown.PriceChange
import io.horizontalsystems.bankwallet.modules.market.filters.MarketFiltersModule.Item
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetMarketSearchFilterSelectDialog
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetMarketSearchFilterSelectMultipleDialog
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class MarketFiltersFragment : BaseFragment() {

    private val viewModel by navGraphViewModels<MarketFiltersViewModel>(R.id.marketAdvancedSearchFragment) {
        MarketFiltersModule.Factory()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    AdvancedSearchScreen(
                        viewModel,
                        findNavController(),
                    ) { filterType -> openSelectionDialog(filterType) }
                }
            }
        }
    }

    private fun openSelectionDialog(filterType: MarketFiltersModule.FilterDropdown) {
        when (filterType) {
            CoinSet -> showFilterCoinListDialog()
            MarketCap -> showFilterMarketCapDialog()
            TradingVolume -> showFilterVolumeDialog()
            Blockchain -> showFilterBlockchainsDialog()
            PriceChange -> showPriceChangeDialog()
            PricePeriod -> showPeriodDialog()
        }
    }

    private fun showFilterCoinListDialog() {
        showSelectorDialog(
            title = R.string.Market_Filter_ChooseSet,
            headerIcon = R.drawable.ic_circle_coin_24,
            items = viewModel.coinListsViewItemOptions,
            selectedItem = viewModel.coinListViewItem,
        ) {
            viewModel.coinListViewItem = it
        }
    }

    private fun showFilterMarketCapDialog() {
        showSelectorDialog(
            title = R.string.Market_Filter_MarketCap,
            headerIcon = R.drawable.ic_usd_24,
            items = viewModel.marketCapViewItemOptions,
            selectedItem = viewModel.marketCapViewItem,
        ) {
            viewModel.marketCapViewItem = it
        }
    }

    private fun showFilterVolumeDialog() {
        showSelectorDialog(
            title = R.string.Market_Filter_Volume,
            subtitleText = getString(R.string.TimePeriod_24h),
            headerIcon = R.drawable.ic_chart_24,
            items = viewModel.volumeViewItemOptions,
            selectedItem = viewModel.volumeViewItem,
        ) {
            viewModel.volumeViewItem = it
        }
    }

    private fun showFilterBlockchainsDialog() {
        showMultipleSelectorDialog(
            title = R.string.Market_Filter_Blockchains,
            headerIcon = R.drawable.ic_blocks_24,
            items = viewModel.blockchainOptions,
            selectedIndexes = viewModel.selectedBlockchainIndexes,
        ) { selectedIndexes ->
            viewModel.selectedBlockchainIndexes = selectedIndexes
        }
    }

    private fun showPriceChangeDialog() {
        showSelectorDialog(
            title = R.string.Market_Filter_PriceChange,
            headerIcon = R.drawable.ic_market_24,
            items = viewModel.priceChangeViewItemOptions,
            selectedItem = viewModel.priceChangeViewItem,
        ) {
            viewModel.priceChangeViewItem = it
        }
    }

    private fun showPeriodDialog() {
        showSelectorDialog(
            title = R.string.Market_Filter_PricePeriod,
            headerIcon = R.drawable.ic_circle_clock_24,
            items = viewModel.periodViewItemOptions,
            selectedItem = viewModel.periodViewItem,
        ) {
            viewModel.periodViewItem = it
        }
    }

    private fun <ItemClass> showSelectorDialog(
        title: Int,
        subtitleText: String = "---------",
        headerIcon: Int,
        items: List<FilterViewItemWrapper<ItemClass>>,
        selectedItem: FilterViewItemWrapper<ItemClass>?,
        onSelectListener: (FilterViewItemWrapper<ItemClass>) -> Unit
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

    private fun <ItemClass> showMultipleSelectorDialog(
        title: Int,
        subtitleText: String = "---------",
        headerIcon: Int,
        items: List<FilterViewItemWrapper<ItemClass>>,
        selectedIndexes: List<Int>,
        onSelectListener: (List<Int>) -> Unit
    ) {
        val dialog = BottomSheetMarketSearchFilterSelectMultipleDialog(
            titleText = getString(title),
            subtitleText = subtitleText,
            headerIcon = headerIcon,
            items = items,
            selectedIndexes = selectedIndexes,
            onCloseListener = onSelectListener
        )

        dialog.show(childFragmentManager, "selector_dialog")
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AdvancedSearchScreen(
    viewModel: MarketFiltersViewModel,
    navController: NavController,
    onClick: (MarketFiltersModule.FilterDropdown) -> Unit,
) {
    val errorMessage = viewModel.errorMessage

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                TranslatableString.ResString(R.string.Market_Filters),
                navigationIcon = {
                    HsIconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Reset),
                        onClick = { viewModel.reset() }
                    )
                ),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                AdvancedSearchContent(viewModel, onClick)
            }

            ButtonsGroupWithShade {
                ButtonPrimaryYellowWithSpinner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    title = viewModel.buttonTitle,
                    onClick = {
                        navController.slideFromRight(
                            R.id.marketAdvancedSearchResultsFragment
                        )
                    },
                    showSpinner = viewModel.showSpinner,
                    enabled = viewModel.buttonEnabled,
                )
            }
        }
    }

    errorMessage?.let {
        HudHelper.showErrorMessage(LocalView.current, it.getString())
    }
}

@Composable
fun AdvancedSearchContent(
    viewModel: MarketFiltersViewModel,
    onDropdownClick: (MarketFiltersModule.FilterDropdown) -> Unit,
) {

    viewModel.sectionsState.forEach { section ->
        section.header?.let {
            SectionHeader(it)
        }
        val composables = mutableListOf<@Composable () -> Unit>()
        section.items.forEach { item ->
            when (item) {
                is Item.Switch -> {
                    composables.add {
                        AdvancedSearchSwitch(item.type.titleResId, item.selected) { checked ->
                            viewModel.onSwitchChanged(item.type, checked)
                        }
                    }
                }
                is Item.DropDown -> {
                    composables.add {
                        AdvancedSearchDropdown(item.type, item.value, onDropdownClick)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        CellSingleLineLawrenceSection(composables)
    }
    Spacer(modifier = Modifier.height(32.dp))
}

@Composable
private fun SectionHeader(header: Int) {
    Text(
        text = stringResource(header),
        style = ComposeAppTheme.typography.subhead1,
        color = ComposeAppTheme.colors.grey,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 16.dp, top = 44.dp, end = 16.dp, bottom = 1.dp)
    )
}

@Composable
private fun AdvancedSearchDropdown(
    type: MarketFiltersModule.FilterDropdown,
    value: String?,
    onDropdownClick: (MarketFiltersModule.FilterDropdown) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .clickable { onDropdownClick(type) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(type.titleResId),
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        FilterMenu(value) { onDropdownClick(type) }
    }
}

@Composable
private fun AdvancedSearchSwitch(
    titleResId: Int,
    enabled: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .clickable { onChecked(!enabled) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(titleResId),
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        HsSwitch(
            checked = enabled,
            onCheckedChange = onChecked,
        )
    }
}

@Composable
fun FilterMenu(title: String?, onClick: () -> Unit) {
    val valueText = title ?: stringResource(R.string.Any)
    Row(
        Modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            valueText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (title != null) ComposeAppTheme.colors.oz else ComposeAppTheme.colors.grey,
            modifier = Modifier.weight(1f, fill = false)
        )
        Icon(
            modifier = Modifier.padding(start = 4.dp),
            painter = painterResource(id = R.drawable.ic_down_arrow_20),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey
        )
    }
}
