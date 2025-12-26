package io.horizontalsystems.bankwallet.modules.market.filters

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.paidAction
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatPremiumTrigger
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.market.filters.MarketFiltersModule.FilterDropdown.CoinSet
import io.horizontalsystems.bankwallet.modules.market.filters.MarketFiltersModule.FilterDropdown.MarketCap
import io.horizontalsystems.bankwallet.modules.market.filters.MarketFiltersModule.FilterDropdown.PriceChange
import io.horizontalsystems.bankwallet.modules.market.filters.MarketFiltersModule.FilterDropdown.PriceCloseTo
import io.horizontalsystems.bankwallet.modules.market.filters.MarketFiltersModule.FilterDropdown.PricePeriod
import io.horizontalsystems.bankwallet.modules.market.filters.MarketFiltersModule.FilterDropdown.TradingSignals
import io.horizontalsystems.bankwallet.modules.market.filters.MarketFiltersModule.FilterDropdown.TradingVolume
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellowWithSpinner
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.PremiumHeader
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.body_lucian
import io.horizontalsystems.bankwallet.ui.compose.components.body_remus
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionPremiumUniversalLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.subscriptions.core.AdvancedSearch
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedSearchScreen(
    viewModel: MarketFiltersViewModel,
    navController: NavController,
) {
    val uiState = viewModel.uiState
    val errorMessage = uiState.errorMessage
    val coroutineScope = rememberCoroutineScope()

    var bottomSheetType by remember { mutableStateOf(CoinSet) }
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isBottomSheetVisible by remember { mutableStateOf(false) }

    HSScaffold(
        title = stringResource(R.string.Market_Filters),
        onBack = navController::popBackStack,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Reset),
                onClick = { viewModel.reset() },
                enabled = uiState.resetEnabled,
                tint = ComposeAppTheme.colors.jacob
            )
        ),
    ) {
        Column {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                AdvancedSearchContent(
                    navController = navController,
                    viewModel = viewModel,
                    onFilterByBlockchainsClick = {
                        navController.slideFromBottom(R.id.blockchainsSelectorFragment)
                    },
                    showBottomSheet = { type ->
                        bottomSheetType = type
                        coroutineScope.launch {
                            modalBottomSheetState.show()
                            isBottomSheetVisible = true
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

    if (isBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                isBottomSheetVisible = false
            },
            sheetState = modalBottomSheetState,
            containerColor = ComposeAppTheme.colors.transparent
        ) {
            BottomSheetContent(
                bottomSheetType = bottomSheetType,
                viewModel = viewModel,
                onClose = {
                    coroutineScope.launch {
                        modalBottomSheetState.hide()
                        isBottomSheetVisible = false
                    }
                }
            )
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
        PriceCloseTo -> {
            PriceCloseToBottomSheetContent(
                items = viewModel.priceCloseToOptions,
                selectedItem = uiState.priceCloseTo,
                onSelect = {
                    viewModel.updatePriceCloseTo(it)
                },
                onClose = onClose
            )
        }

        CoinSet -> {
            CoinSetBottomSheetContent(
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
    navController: NavController,
    viewModel: MarketFiltersViewModel,
    onFilterByBlockchainsClick: () -> Unit,
    showBottomSheet: (MarketFiltersModule.FilterDropdown) -> Unit,
) {
    val uiState = viewModel.uiState

    VSpacer(12.dp)

    SectionUniversalLawrence {
        AdvancedSearchDropdown(
            title = R.string.Market_Filter_ChooseSet,
            value = stringResource(uiState.coinListSet.titleResId),
            borderTop = false,
            onDropdownClick = { showBottomSheet(CoinSet) }
        )
        AdvancedSearchDropdown(
            title = R.string.Market_Filter_Volume24h,
            value = uiState.volume.title,
            onDropdownClick = { showBottomSheet(TradingVolume) }
        )
        AdvancedSearchDropdown(
            title = R.string.Market_Filter_Blockchains,
            value = uiState.selectedBlockchainsValue,
            onDropdownClick = onFilterByBlockchainsClick
        )
    }

    VSpacer(24.dp)

    PremiumHeader()

    SectionPremiumUniversalLawrence {
        AdvancedSearchDropdown(
            title = R.string.Market_Filter_Sectors,
            value = if (uiState.sectors.size == 1 && uiState.sectors[0].item == null) null else uiState.sectors.size.toString(),
            onDropdownClick = {
                navController.paidAction(AdvancedSearch) {
                    navController.slideFromBottom(R.id.sectorsSelectorFragment)
                }
                stat(
                    page = StatPage.AdvancedSearch,
                    event = StatEvent.OpenPremium(StatPremiumTrigger.Sectors)
                )
            }
        )
    }

    VSpacer(24.dp)

    SectionPremiumUniversalLawrence {
        AdvancedSearchDropdown(
            title = R.string.Market_Filter_PriceChange,
            value = uiState.priceChange.title,
            valueColor = uiState.priceChange.item?.color ?: TextColor.Grey,
            onDropdownClick = {
                navController.paidAction(AdvancedSearch) {
                    showBottomSheet(PriceChange)
                }
                stat(
                    page = StatPage.AdvancedSearch,
                    event = StatEvent.OpenPremium(StatPremiumTrigger.PriceChange)
                )
            }
        )
        AdvancedSearchDropdown(
            title = R.string.Market_Filter_PricePeriod,
            value = uiState.period.title,
            onDropdownClick = {
                navController.paidAction(AdvancedSearch) {
                    showBottomSheet(PricePeriod)
                }
                stat(
                    page = StatPage.AdvancedSearch,
                    event = StatEvent.OpenPremium(StatPremiumTrigger.PricePeriod)
                )
            }

        )
        AdvancedSearchDropdown(
            title = R.string.Market_Filter_TradingSignals,
            value = uiState.filterTradingSignal.title,
            onDropdownClick = {
                navController.paidAction(AdvancedSearch) {
                    showBottomSheet(TradingSignals)
                }
                stat(
                    page = StatPage.AdvancedSearch,
                    event = StatEvent.OpenPremium(StatPremiumTrigger.TradingSignal)
                )
            }
        )
        AdvancedSearchDropdown(
            title = R.string.Market_Filter_PriceCloseTo,
            value = uiState.priceCloseTo?.titleResId?.let { stringResource(it) },
            onDropdownClick = {
                navController.paidAction(AdvancedSearch) {
                    showBottomSheet(PriceCloseTo)
                }
                stat(
                    page = StatPage.AdvancedSearch,
                    event = StatEvent.OpenPremium(StatPremiumTrigger.PriceCloseTo)
                )
            }
        )
    }

    VSpacer(24.dp)

    SectionPremiumUniversalLawrence {
        AdvancedSearchSwitch(
            title = R.string.Market_Filter_OutperformedBtc,
            enabled = uiState.outperformedBtcOn,
            onChecked = {
                navController.paidAction(AdvancedSearch) {
                    viewModel.updateOutperformedBtcOn(it)
                }
                stat(
                    page = StatPage.AdvancedSearch,
                    event = StatEvent.OpenPremium(StatPremiumTrigger.OutperformedBtc)
                )
            }
        )
        AdvancedSearchSwitch(
            title = R.string.Market_Filter_OutperformedEth,
            enabled = uiState.outperformedEthOn,
            onChecked = {
                navController.paidAction(AdvancedSearch) {
                    viewModel.updateOutperformedEthOn(it)
                }
                stat(
                    page = StatPage.AdvancedSearch,
                    event = StatEvent.OpenPremium(StatPremiumTrigger.OutperformedEth)
                )
            }
        )
        AdvancedSearchSwitch(
            title = R.string.Market_Filter_OutperformedBnb,
            enabled = uiState.outperformedBnbOn,
            onChecked = {
                navController.paidAction(AdvancedSearch) {
                    viewModel.updateOutperformedBnbOn(it)
                }
                stat(
                    page = StatPage.AdvancedSearch,
                    event = StatEvent.OpenPremium(StatPremiumTrigger.OutperformedBnb)
                )
            }
        )
        AdvancedSearchSwitch(
            title = R.string.Market_Filter_OutperformedSnp,
            enabled = uiState.outperformedSnpOn,
            onChecked = {
                navController.paidAction(AdvancedSearch) {
                    viewModel.updateOutperformedSnpOn(it)
                }
                stat(
                    page = StatPage.AdvancedSearch,
                    event = StatEvent.OpenPremium(StatPremiumTrigger.OutperformedSp500)
                )
            }
        )
        AdvancedSearchSwitch(
            title = R.string.Market_Filter_OutperformedGold,
            enabled = uiState.outperformedGoldOn,
            onChecked = {
                navController.paidAction(AdvancedSearch) {
                    viewModel.updateOutperformedGoldOn(it)
                }
                stat(
                    page = StatPage.AdvancedSearch,
                    event = StatEvent.OpenPremium(StatPremiumTrigger.OutperformedGold)
                )
            }
        )
    }

    VSpacer(24.dp)

    SectionPremiumUniversalLawrence {
        AdvancedSearchSwitch(
            title = R.string.Market_Filter_SolidCex,
            subtitle = R.string.Market_Filter_SolidCex_Description,
            enabled = uiState.solidCexOn,
            onChecked = {
                navController.paidAction(AdvancedSearch) {
                    viewModel.updateSolidCexOn(it)
                }
                stat(
                    page = StatPage.AdvancedSearch,
                    event = StatEvent.OpenPremium(StatPremiumTrigger.GoodCexVolume)
                )
            }
        )
        AdvancedSearchSwitch(
            title = R.string.Market_Filter_SolidDex,
            subtitle = R.string.Market_Filter_SolidDex_Description,
            enabled = uiState.solidDexOn,
            onChecked = {
                navController.paidAction(AdvancedSearch) {
                    viewModel.updateSolidDexOn(it)
                }
                stat(
                    page = StatPage.AdvancedSearch,
                    event = StatEvent.OpenPremium(StatPremiumTrigger.GoodDexVolume)
                )
            }
        )
        AdvancedSearchSwitch(
            title = R.string.Market_Filter_GoodDistribution,
            subtitle = R.string.Market_Filter_GoodDistribution_Description,
            enabled = uiState.goodDistributionOn,
            onChecked = {
                navController.paidAction(AdvancedSearch) {
                    viewModel.updateGoodDistributionOn(it)
                }
                stat(
                    page = StatPage.AdvancedSearch,
                    event = StatEvent.OpenPremium(StatPremiumTrigger.GoodDistribution)
                )
            }
        )
        AdvancedSearchSwitch(
            title = R.string.Market_Filter_ListedOnTopExchanges,
            enabled = uiState.listedOnTopExchangesOn,
            onChecked = {
                navController.paidAction(AdvancedSearch) {
                    viewModel.updateListedOnTopExchangesOn(it)
                }
                stat(
                    page = StatPage.AdvancedSearch,
                    event = StatEvent.OpenPremium(StatPremiumTrigger.ListedOnTopExchanges)
                )
            }
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
