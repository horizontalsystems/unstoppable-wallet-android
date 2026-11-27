package io.horizontalsystems.walletkit.modules.market.filters

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.designkit.components.ButtonStyle
import io.horizontalsystems.designkit.components.ButtonVariant
import io.horizontalsystems.designkit.components.HSButton
import io.horizontalsystems.designkit.components.HSDropdownButton
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.StatPremiumTrigger
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.walletkit.modules.market.filters.MarketFiltersModule.FilterDropdown.CoinSet
import io.horizontalsystems.walletkit.modules.market.filters.MarketFiltersModule.FilterDropdown.MarketCap
import io.horizontalsystems.walletkit.modules.market.filters.MarketFiltersModule.FilterDropdown.PriceChange
import io.horizontalsystems.walletkit.modules.market.filters.MarketFiltersModule.FilterDropdown.PriceCloseTo
import io.horizontalsystems.walletkit.modules.market.filters.MarketFiltersModule.FilterDropdown.PricePeriod
import io.horizontalsystems.walletkit.modules.market.filters.MarketFiltersModule.FilterDropdown.TradingSignals
import io.horizontalsystems.walletkit.modules.market.filters.MarketFiltersModule.FilterDropdown.TradingVolume
import io.horizontalsystems.walletkit.modules.market.filtersresult.MarketFiltersResultsPage
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.HsSwitch
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.ui.compose.components.PremiumHeader
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.body_leah
import io.horizontalsystems.walletkit.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.walletkit.ui.compose.components.cell.SectionPremiumUniversalLawrence
import io.horizontalsystems.walletkit.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_grey
import io.horizontalsystems.walletkit.uiv3.TestTags
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.subscriptions.core.AdvancedSearch
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object MarketFiltersPage : HSPage(accessibleWhileLocked = true) {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        AdvancedSearchScreen(
            viewModel(factory = MarketFiltersModule.Factory()),
            navigation,
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedSearchScreen(
    viewModel: MarketFiltersViewModel,
    navigation: HSNavigation,
) {
    val uiState = viewModel.uiState
    val errorMessage = uiState.errorMessage
    val coroutineScope = rememberCoroutineScope()

    var bottomSheetType by remember { mutableStateOf(CoinSet) }
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isBottomSheetVisible by remember { mutableStateOf(false) }

    HSScaffold(
        title = stringResource(R.string.Market_Filters),
        onBack = navigation::removeLastOrNull,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Reset),
                onClick = { viewModel.reset() },
                enabled = uiState.resetEnabled,
                tint = ComposeAppTheme.colors.orange
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
                    navigation = navigation,
                    viewModel = viewModel,
                    onFilterByBlockchainsClick = {
                        navigation.slideFromBottom(BlockchainsSelectorPage)
                    },
                    showBottomSheet = { type ->
                        bottomSheetType = type
                        isBottomSheetVisible = true
                    }
                )
            }

            ButtonsGroupWithShade {
                HSButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    title = uiState.buttonTitle,
                    onClick = {
                        navigation.slideFromRight(MarketFiltersResultsPage)
                    },
                    loadingIndicator = uiState.showSpinner,
                    enabled = uiState.buttonEnabled,
                )
            }
        }
    }

    if (isBottomSheetVisible) {
        BottomSheetContent(
            onDismissRequest = {
                isBottomSheetVisible = false
            },
            sheetState = modalBottomSheetState
        ) {
            BSContent(
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
private fun BSContent(
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
                headerIcon = R.drawable.clock_24,
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
    navigation: HSNavigation,
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
            testTag = TestTags.MARKET_FILTERS_SET,
            onDropdownClick = { showBottomSheet(CoinSet) }
        )
        AdvancedSearchDropdown(
            title = R.string.Market_Filter_Volume24h,
            value = uiState.volume.title,
            testTag = TestTags.MARKET_FILTERS_VOLUME,
            onDropdownClick = { showBottomSheet(TradingVolume) }
        )
        AdvancedSearchDropdown(
            title = R.string.Market_Filter_Blockchains,
            value = uiState.selectedBlockchainsValue,
            testTag = TestTags.MARKET_FILTERS_BLOCKCHAINS,
            onDropdownClick = onFilterByBlockchainsClick
        )
    }

    VSpacer(24.dp)

    PremiumHeader()

    SectionPremiumUniversalLawrence {
        AdvancedSearchDropdown(
            title = R.string.Market_Filter_Sectors,
            value = if (uiState.sectors.size == 1 && uiState.sectors[0].item == null) null else uiState.sectors.size.toString(),
            testTag = TestTags.MARKET_FILTERS_SECTORS,
            onDropdownClick = {
                navigation.paidAction(AdvancedSearch) {
                    navigation.slideFromBottom(SectorsSelectorPage)
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
            testTag = TestTags.MARKET_FILTERS_PRICE_CHANGE,
            onDropdownClick = {
                navigation.paidAction(AdvancedSearch) {
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
            testTag = TestTags.MARKET_FILTERS_PRICE_PERIOD,
            onDropdownClick = {
                navigation.paidAction(AdvancedSearch) {
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
            testTag = TestTags.MARKET_FILTERS_TRADING_SIGNALS,
            onDropdownClick = {
                navigation.paidAction(AdvancedSearch) {
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
            testTag = TestTags.MARKET_FILTERS_PRICE_CLOSE_TO,
            onDropdownClick = {
                navigation.paidAction(AdvancedSearch) {
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
                navigation.paidAction(AdvancedSearch) {
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
                navigation.paidAction(AdvancedSearch) {
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
                navigation.paidAction(AdvancedSearch) {
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
                navigation.paidAction(AdvancedSearch) {
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
                navigation.paidAction(AdvancedSearch) {
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
                navigation.paidAction(AdvancedSearch) {
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
                navigation.paidAction(AdvancedSearch) {
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
                navigation.paidAction(AdvancedSearch) {
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
                navigation.paidAction(AdvancedSearch) {
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
    testTag: String,
    borderTop: Boolean = true,
    onDropdownClick: () -> Unit,
) {
    CellUniversal(
        modifier = Modifier.testTag(testTag),
        borderTop = borderTop,
        onClick = onDropdownClick
    ) {
        body_leah(
            modifier = Modifier.weight(1f),
            text = stringResource(title),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        HSDropdownButton(
            modifier = Modifier.testTag(TestTags.dropdownValue(testTag)),
            variant = ButtonVariant.Secondary,
            style = ButtonStyle.Transparent,
            title = value ?: stringResource(R.string.Any),
            onClick = onDropdownClick,
        )
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
