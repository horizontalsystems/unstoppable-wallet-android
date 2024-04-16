package io.horizontalsystems.bankwallet.modules.market.filters

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Surface
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.market.filters.MarketFiltersModule.FilterDropdown.*
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellowWithSpinner
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.body_lucian
import io.horizontalsystems.bankwallet.ui.compose.components.body_remus
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch
import io.horizontalsystems.bankwallet.modules.market.filters.PriceChange as FilterPriceChange

class MarketFiltersFragment : BaseComposeFragment() {

    private val viewModel by navGraphViewModels<MarketFiltersViewModel>(R.id.marketAdvancedSearchFragment) {
        MarketFiltersModule.Factory()
    }

    @Composable
    override fun GetContent(navController: NavController) {
        AdvancedSearchScreen(
            viewModel,
            navController,
        )
    }

}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun AdvancedSearchScreen(
    viewModel: MarketFiltersViewModel,
    navController: NavController,
) {
    val uiState = viewModel.uiState
    val errorMessage = uiState.errorMessage
    val coroutineScope = rememberCoroutineScope()

    var bottomSheetType by remember { mutableStateOf(CoinSet) }
    val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)

    ModalBottomSheetLayout(
        sheetState = modalBottomSheetState,
        sheetBackgroundColor = ComposeAppTheme.colors.transparent,
        sheetContent = {
            BottomSheetContent(
                bottomSheetType = bottomSheetType,
                viewModel = viewModel,
                onClose = {
                    coroutineScope.launch {
                        modalBottomSheetState.hide()
                    }
                }
            )
        },
    ) {
        Surface(color = ComposeAppTheme.colors.tyler) {
            Column {
                AppBar(
                    title = stringResource(R.string.Market_Filters),
                    navigationIcon = {
                        HsBackButton(onClick = { navController.popBackStack() })
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
                    AdvancedSearchContent(
                        viewModel = viewModel,
                        onFilterByBlockchainsClick = {
                            navController.slideFromRight(R.id.blockchainsSelectorFragment)
                        },
                        showBottomSheet = { type ->
                            bottomSheetType = type
                            coroutineScope.launch {
                                modalBottomSheetState.show()
                            }
                        }
                    )
                }

                ButtonsGroupWithShade {
                    ButtonPrimaryYellowWithSpinner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        title = uiState.buttonTitle,
                        onClick = {
                            navController.slideFromRight(
                                R.id.marketAdvancedSearchResultsFragment
                            )
                        },
                        showSpinner = uiState.showSpinner,
                        enabled = uiState.buttonEnabled,
                    )
                }
            }
        }
    }

    errorMessage?.let {
        HudHelper.showErrorMessage(LocalView.current, it.getString())
    }
}

@Composable
private fun BottomSheetContent(
    bottomSheetType: MarketFiltersModule.FilterDropdown,
    viewModel: MarketFiltersViewModel,
    onClose: () -> Unit,
) {
    val uiState = viewModel.uiState
    when (bottomSheetType) {
        CoinSet -> {
            SingleSelectBottomSheetContent(
                title = R.string.Market_Filter_ChooseSet,
                headerIcon = R.drawable.ic_circle_coin_24,
                items = viewModel.coinListsViewItemOptions,
                selectedItem = uiState.coinListSet,
                onSelect = {
                    viewModel.updateCoinList(it)
                },
                onClose = onClose
            )
        }

        MarketCap -> {
            SingleSelectBottomSheetContent(
                title = R.string.Market_Filter_MarketCap,
                headerIcon = R.drawable.ic_usd_24,
                items = viewModel.marketCapViewItemOptions,
                selectedItem = uiState.marketCap,
                onSelect = {
                    viewModel.updateMarketCap(it)
                },
                onClose = onClose
            )
        }

        TradingVolume -> {
            SingleSelectBottomSheetContent(
                title = R.string.Market_Filter_Volume24h,
                headerIcon = R.drawable.ic_chart_24,
                items = viewModel.volumeViewItemOptions,
                selectedItem = uiState.volume,
                onSelect = {
                    viewModel.updateVolume(it)
                },
                onClose = onClose
            )
        }

        PriceChange -> {
            SingleSelectBottomSheetContent(
                title = R.string.Market_Filter_PriceChange,
                headerIcon = R.drawable.icon_24_markets,
                items = viewModel.priceChangeViewItemOptions,
                selectedItem = uiState.priceChange,
                onSelect = {
                    viewModel.updatePriceChange(it)
                },
                onClose = onClose
            )
        }

        PricePeriod -> {
            SingleSelectBottomSheetContent(
                title = R.string.Market_Filter_PricePeriod,
                headerIcon = R.drawable.ic_circle_clock_24,
                items = viewModel.periodViewItemOptions,
                selectedItem = uiState.period,
                onSelect = {
                    viewModel.updatePeriod(it)
                },
                onClose = onClose
            )
        }

        TradingSignals -> {
            SingleSelectBottomSheetContent(
                title = R.string.Market_Filter_TradingSignals,
                headerIcon = R.drawable.ic_ring_24,
                items = viewModel.tradingSignals,
                selectedItem = uiState.filterTradingSignal,
                onSelect = {
                    viewModel.updateTradingSignal(it)
                },
                onClose = onClose
            )
        }
    }
}

@Composable
fun AdvancedSearchContent(
    viewModel: MarketFiltersViewModel,
    onFilterByBlockchainsClick: () -> Unit,
    showBottomSheet: (MarketFiltersModule.FilterDropdown) -> Unit,
) {
    val uiState = viewModel.uiState

    VSpacer(height = 12.dp)

    SectionUniversalLawrence {
        AdvancedSearchDropdown(
            title = R.string.Market_Filter_ChooseSet,
            value = uiState.coinListSet.title,
            borderTop = false,
            onDropdownClick = { showBottomSheet(CoinSet) }
        )
    }
    VSpacer(height = 32.dp)

    HeaderText(stringResource(R.string.Market_FilterSection_MarketParameters))
    SectionUniversalLawrence {
        AdvancedSearchDropdown(
            title = R.string.Market_Filter_MarketCap,
            value = uiState.marketCap.title,
            borderTop = false,
            onDropdownClick = { showBottomSheet(MarketCap) }
        )
        AdvancedSearchDropdown(
            title = R.string.Market_Filter_Volume,
            value = uiState.volume.title,
            onDropdownClick = { showBottomSheet(TradingVolume) }
        )
        AdvancedSearchSwitch(
            title = R.string.Market_Filter_ListedOnTopExchanges,
            enabled = uiState.listedOnTopExchangesOn,
            onChecked = { viewModel.updateListedOnTopExchangesOn(it) }
        )
        AdvancedSearchSwitch(
            title = R.string.Market_Filter_SolidCex,
            subtitle = R.string.Market_Filter_SolidCex_Description,
            enabled = uiState.solidCexOn,
            onChecked = { viewModel.updateSolidCexOn(it) }
        )
        AdvancedSearchSwitch(
            title = R.string.Market_Filter_SolidDex,
            subtitle = R.string.Market_Filter_SolidDex_Description,
            enabled = uiState.solidDexOn,
            onChecked = { viewModel.updateSolidDexOn(it) }
        )
        AdvancedSearchSwitch(
            title = R.string.Market_Filter_GoodDistribution,
            subtitle = R.string.Market_Filter_GoodDistribution_Description,
            enabled = uiState.goodDistributionOn,
            onChecked = { viewModel.updateGoodDistributionOn(it) }
        )
    }
    VSpacer(height = 32.dp)

    HeaderText(stringResource(R.string.Market_FilterSection_PriceParameters))
    SectionUniversalLawrence {
        AdvancedSearchDropdown(
            title = R.string.Market_Filter_PriceChange,
            value = uiState.priceChange.title,
            valueColor = uiState.priceChange.item?.color ?: TextColor.Grey,
            onDropdownClick = { showBottomSheet(PriceChange) }
        )
        AdvancedSearchDropdown(
            title = R.string.Market_Filter_PricePeriod,
            value = uiState.period.title,
            onDropdownClick = { showBottomSheet(PricePeriod) }
        )
        AdvancedSearchSwitch(
            title = R.string.Market_Filter_OutperformedBtc,
            enabled = uiState.outperformedBtcOn,
            onChecked = { viewModel.updateOutperformedBtcOn(it) }
        )
        AdvancedSearchSwitch(
            title = R.string.Market_Filter_OutperformedEth,
            enabled = uiState.outperformedEthOn,
            onChecked = { viewModel.updateOutperformedEthOn(it) }
        )
        AdvancedSearchSwitch(
            title = R.string.Market_Filter_OutperformedBnb,
            enabled = uiState.outperformedBnbOn,
            onChecked = { viewModel.updateOutperformedBnbOn(it) }
        )
        AdvancedSearchSwitch(
            title = R.string.Market_Filter_PriceCloseToAth,
            enabled = uiState.priceCloseToAth,
            onChecked = { viewModel.updateOutperformedAthOn(it) }
        )
        AdvancedSearchSwitch(
            title = R.string.Market_Filter_PriceCloseToAtl,
            enabled = uiState.priceCloseToAtl,
            onChecked = { viewModel.updateOutperformedAtlOn(it) }
        )
    }
    VSpacer(height = 32.dp)

    HeaderText(stringResource(R.string.Market_FilterSection_NetworkParameters))
    SectionUniversalLawrence {
        AdvancedSearchDropdown(
            title = R.string.Market_Filter_Blockchains,
            value = uiState.selectedBlockchainsValue,
            onDropdownClick = onFilterByBlockchainsClick
        )
    }
    VSpacer(height = 32.dp)

    HeaderText(stringResource(R.string.Market_FilterSection_Indicators))
    SectionUniversalLawrence {
        AdvancedSearchDropdown(
            title = R.string.Market_Filter_TradingSignals,
            value = uiState.filterTradingSignal.title,
            onDropdownClick = { showBottomSheet(TradingSignals) }
        )
    }
    VSpacer(32.dp)
}

@Composable
private fun AdvancedSearchDropdown(
    @StringRes title: Int,
    value: String?,
    valueColor: TextColor = TextColor.Leah,
    borderTop: Boolean = true,
    onDropdownClick: () -> Unit,
) {
    CellUniversal(
        borderTop = borderTop,
        onClick = onDropdownClick
    ) {
        body_leah(
            text = stringResource(title),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        FilterMenu(value, valueColor) {
            onDropdownClick()
        }
    }
}

@Composable
private fun AdvancedSearchSwitch(
    title: Int,
    subtitle: Int? = null,
    enabled: Boolean,
    borderTop: Boolean = true,
    onChecked: (Boolean) -> Unit,
) {
    CellUniversal(
        borderTop = borderTop,
        onClick = { onChecked(!enabled) }
    ) {
        Column {
            body_leah(
                text = stringResource(title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                VSpacer(height = 1.dp)
                subhead2_grey(
                    text = stringResource(subtitle),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        HsSwitch(
            checked = enabled,
            onCheckedChange = onChecked,
        )
    }
}

@Composable
private fun FilterMenu(title: String?, valueColor: TextColor, onClick: () -> Unit) {
    val valueText = title ?: stringResource(R.string.Any)
    val textColor = if (title != null) valueColor else TextColor.Grey
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
        when (textColor) {
            TextColor.Grey -> body_grey(
                text = valueText,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )

            TextColor.Remus -> body_remus(
                text = valueText,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )

            TextColor.Lucian -> body_lucian(
                text = valueText,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )

            TextColor.Leah -> body_leah(
                text = valueText,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
        Icon(
            modifier = Modifier.padding(start = 4.dp),
            painter = painterResource(id = R.drawable.ic_down_arrow_20),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey
        )
    }
}

@Composable
private fun <ItemClass> SingleSelectBottomSheetContent(
    title: Int,
    headerIcon: Int,
    items: List<FilterViewItemWrapper<ItemClass>>,
    selectedItem: FilterViewItemWrapper<ItemClass>? = null,
    onSelect: (FilterViewItemWrapper<ItemClass>) -> Unit,
    onClose: (() -> Unit),
) {
    BottomSheetHeader(
        iconPainter = painterResource(headerIcon),
        title = stringResource(title),
        onCloseClick = onClose,
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob)
    ) {
        Spacer(Modifier.height(12.dp))
        CellUniversalLawrenceSection(
            items = items,
            showFrame = true
        ) { itemWrapper ->
            RowUniversal(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = {
                    onSelect(itemWrapper)
                    onClose()
                }
            ) {
                if (itemWrapper.title != null && itemWrapper.item is FilterPriceChange) {
                    when (itemWrapper.item.color) {
                        TextColor.Lucian -> body_lucian(text = itemWrapper.title)
                        TextColor.Remus -> body_remus(text = itemWrapper.title)
                        TextColor.Grey -> body_grey(text = itemWrapper.title)
                        TextColor.Leah -> body_leah(text = itemWrapper.title)
                    }
                } else {
                    if (itemWrapper.title != null) {
                        body_leah(text = itemWrapper.title)
                    } else {
                        body_grey(text = stringResource(R.string.Any))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (itemWrapper == selectedItem) {
                    Image(
                        modifier = Modifier.padding(start = 5.dp),
                        painter = painterResource(id = R.drawable.ic_checkmark_20),
                        colorFilter = ColorFilter.tint(ComposeAppTheme.colors.jacob),
                        contentDescription = null
                    )
                }
            }
        }
        Spacer(Modifier.height(44.dp))
    }
}
