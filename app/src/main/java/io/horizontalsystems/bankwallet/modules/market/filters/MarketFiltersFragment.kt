package io.horizontalsystems.bankwallet.modules.market.filters

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
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
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch
import io.horizontalsystems.bankwallet.modules.market.filters.PriceChange as FilterPriceChange

class MarketFiltersFragment : BaseComposeFragment() {

    private val viewModel by navGraphViewModels<MarketFiltersViewModel>(R.id.marketAdvancedSearchFragment) {
        MarketFiltersModule.Factory()
    }

    @Composable
    override fun GetContent() {
        AdvancedSearchScreen(
            viewModel,
            findNavController(),
        )
    }

}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun AdvancedSearchScreen(
    viewModel: MarketFiltersViewModel,
    navController: NavController,
) {
    val errorMessage = viewModel.errorMessage
    val coroutineScope = rememberCoroutineScope()

    var bottomSheetType by remember { mutableStateOf(CoinSet) }
    val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)

    ComposeAppTheme {
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
                        TranslatableString.ResString(R.string.Market_Filters),
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
    when (bottomSheetType) {
        CoinSet -> {
            SingleSelectBottomSheetContent(
                title = R.string.Market_Filter_ChooseSet,
                headerIcon = R.drawable.ic_circle_coin_24,
                items = viewModel.coinListsViewItemOptions,
                selectedItem = viewModel.coinListSet,
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
                selectedItem = viewModel.marketCap,
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
                selectedItem = viewModel.volume,
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
                selectedItem = viewModel.priceChange,
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
                selectedItem = viewModel.period,
                onSelect = {
                    viewModel.updatePeriod(it)
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

    Spacer(Modifier.height(12.dp))

    CellSingleLineLawrenceSection(
        listOf {
            AdvancedSearchDropdown(
                title = R.string.Market_Filter_ChooseSet,
                value = viewModel.coinListSet.title,
                onDropdownClick = { showBottomSheet(CoinSet) }
            )
        }
    )

    Spacer(Modifier.height(24.dp))
    HeaderText(stringResource(R.string.Market_FilterSection_MarketParameters))

    CellSingleLineLawrenceSection(
        listOf({
            AdvancedSearchDropdown(
                title = R.string.Market_Filter_MarketCap,
                value = viewModel.marketCap.title,
                onDropdownClick = { showBottomSheet(MarketCap) }
            )
        }, {
            AdvancedSearchDropdown(
                title = R.string.Market_Filter_Volume,
                value = viewModel.volume.title,
                onDropdownClick = { showBottomSheet(TradingVolume) }
            )
        })
    )

    Spacer(Modifier.height(24.dp))
    HeaderText(stringResource(R.string.Market_FilterSection_NetworkParameters))

    CellSingleLineLawrenceSection(
        listOf {
            AdvancedSearchDropdown(
                title = R.string.Market_Filter_Blockchains,
                value = viewModel.selectedBlockchainsValue,
                onDropdownClick = onFilterByBlockchainsClick
            )
        }
    )

    Spacer(Modifier.height(24.dp))
    HeaderText(stringResource(R.string.Market_FilterSection_PriceParameters))

    CellSingleLineLawrenceSection(
        listOf({
            AdvancedSearchDropdown(
                title = R.string.Market_Filter_PriceChange,
                value = viewModel.priceChange.title,
                valueColor = viewModel.priceChange.item?.color ?: TextColor.Grey,
                onDropdownClick = { showBottomSheet(PriceChange) }
            )
        }, {
            AdvancedSearchDropdown(
                title = R.string.Market_Filter_PricePeriod,
                value = viewModel.period.title,
                onDropdownClick = { showBottomSheet(PricePeriod) }
            )
        }, {
            AdvancedSearchSwitch(
                title = R.string.Market_Filter_OutperformedBtc,
                enabled = viewModel.outperformedBtcOn,
                onChecked = { viewModel.updateOutperformedBtcOn(it) }
            )
        }, {
            AdvancedSearchSwitch(
                title = R.string.Market_Filter_OutperformedEth,
                enabled = viewModel.outperformedEthOn,
                onChecked = { viewModel.updateOutperformedEthOn(it) }
            )
        }, {
            AdvancedSearchSwitch(
                title = R.string.Market_Filter_OutperformedBnb,
                enabled = viewModel.outperformedBnbOn,
                onChecked = { viewModel.updateOutperformedBnbOn(it) }
            )
        }, {
            AdvancedSearchSwitch(
                title = R.string.Market_Filter_PriceCloseToAth,
                enabled = viewModel.priceCloseToAth,
                onChecked = { viewModel.updateOutperformedAthOn(it) }
            )
        }, {
            AdvancedSearchSwitch(
                title = R.string.Market_Filter_PriceCloseToAtl,
                enabled = viewModel.priceCloseToAtl,
                onChecked = { viewModel.updateOutperformedAtlOn(it) }
            )
        })
    )

    Spacer(modifier = Modifier.height(32.dp))
}

@Composable
private fun AdvancedSearchDropdown(
    @StringRes title: Int,
    value: String?,
    valueColor: TextColor = TextColor.Leah,
    onDropdownClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .clickable {
                onDropdownClick()
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
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
        body_leah(
            text = stringResource(title),
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
