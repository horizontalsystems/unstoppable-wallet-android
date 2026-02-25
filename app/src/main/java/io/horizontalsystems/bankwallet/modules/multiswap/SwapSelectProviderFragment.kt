package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.multiswap.providers.RiskLevel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.BoxBordered
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellLeftImage
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightSelectors
import io.horizontalsystems.bankwallet.uiv3.components.cell.HSString
import io.horizontalsystems.bankwallet.uiv3.components.cell.ImageType
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSDropdownButton
import io.horizontalsystems.bankwallet.uiv3.components.menu.MenuGroup
import io.horizontalsystems.bankwallet.uiv3.components.menu.MenuItemX
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabsSectionButtons

class SwapSelectProviderFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SwapSelectProviderScreen(navController)
    }
}

@Composable
fun SwapSelectProviderScreen(navController: NavController) {
    val previousBackStackEntry = remember { navController.previousBackStackEntry }
    val swapViewModel = viewModel<SwapViewModel>(
        viewModelStoreOwner = previousBackStackEntry!!,
    )
    val viewModel = viewModel<SwapSelectProviderViewModel>(
        factory = SwapSelectProviderViewModel.Factory(
            swapViewModel.uiState.quotes,
            swapViewModel.uiState.quote
        )
    )

    val uiState = viewModel.uiState

    SwapSelectProviderScreenInner(
        onClickClose = navController::popBackStack,
        quotes = uiState.quoteViewItems,
        currentQuote = uiState.selectedQuote,
        sortType = uiState.sortType,
        onSortTypeChange = {
            viewModel.setSortType(it)
        },
        onBadgeClick = {
            navController.slideFromBottom(R.id.riskLevelInfoBottomSheet)
        }
    ) {
        swapViewModel.onSelectQuote(it)
        navController.popBackStack()

        stat(page = StatPage.SwapProvider, event = StatEvent.SwapSelectProvider(it.provider.id))
    }
}

@Composable
private fun SwapSelectProviderScreenInner(
    onClickClose: () -> Unit,
    quotes: List<QuoteViewItem>,
    currentQuote: SwapProviderQuote?,
    sortType: ProviderSortType,
    onSortTypeChange: (ProviderSortType) -> Unit,
    onBadgeClick: () -> Unit,
    onSelectQuote: (SwapProviderQuote) -> Unit,
) {
    HSScaffold(
        title = stringResource(R.string.Swap_Providers),
        onBack = onClickClose,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(ComposeAppTheme.colors.lawrence)
        ) {
            TabsSectionButtons(
                left = {
                    ProviderSortingSelector(
                        sortType = sortType,
                        sortTypes = listOf(
                            ProviderSortType.BestPrice,
                            ProviderSortType.BestTime,
                            ProviderSortType.Recommended
                        ),
                        onSelectSortType = {
                            onSortTypeChange.invoke(it)
                        }
                    )
                }
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (quotes.isNotEmpty()) {
                    HsDivider()
                }

                quotes.forEach { viewItem ->
                    val provider = viewItem.quote.provider
                    val icon = if (viewItem.quote == currentQuote) {
                        R.drawable.selector_checked_20
                    } else {
                        R.drawable.selector_unchecked_20
                    }
                    val iconTint = if (viewItem.quote == currentQuote) {
                        ComposeAppTheme.colors.jacob
                    } else {
                        ComposeAppTheme.colors.andy
                    }
                    BoxBordered(bottom = true) {
                        CellPrimary(
                            left = {
                                CellLeftImage(
                                    painter = painterResource(provider.icon),
                                    type = ImageType.Rectangle,
                                    size = 32
                                )
                            },
                            middle = {
                                var description = provider.type.title

                                if (provider.aml) {
                                    description += ", "
                                    description += stringResource(R.string.Swap_AML)
                                }

                                CellMiddleInfo(
                                    eyebrow = provider.title.hs(ComposeAppTheme.colors.leah),
                                    eyebrowBadge = getRiskLevelHsString(provider.riskLevel),
                                    onEyebrowBadgeClick = onBadgeClick,
                                    subtitle = description.hs,
                                )
                            },
                            right = {
                                CellRightSelectors(
                                    subtitle = viewItem.tokenAmount.hs,
                                    description1 = viewItem.fiatAmount?.hs,
                                    description2 = getPriceImpact(viewItem.priceImpactData),
                                    icon = painterResource(icon),
                                    iconTint = iconTint
                                )
                            },
                            onClick = { onSelectQuote.invoke(viewItem.quote) }
                        )
                    }
                }
            }
            VSpacer(32.dp)
        }
    }
}

@Composable
fun ProviderSortingSelector(
    sortType: ProviderSortType,
    sortTypes: List<ProviderSortType>,
    onSelectSortType: (ProviderSortType) -> Unit
) {
    var showSortTypeSelectorDialog by remember { mutableStateOf(false) }

    HSDropdownButton(
        variant = ButtonVariant.Secondary,
        title = stringResource(sortType.title),
        onClick = {
            showSortTypeSelectorDialog = true
        }
    )

    if (showSortTypeSelectorDialog) {
        MenuGroup(
            title = stringResource(R.string.Balance_Sort_PopupTitle),
            items = sortTypes.map {
                MenuItemX(stringResource(it.title), it == sortType, it)
            },
            onDismissRequest = {
                showSortTypeSelectorDialog = false
            },
            onSelectItem = onSelectSortType
        )
    }
}

@Composable
fun getRiskLevelHsString(riskLevel: RiskLevel): HSString {
    val text = stringResource(riskLevel.title)
    val color = when (riskLevel) {
        RiskLevel.AUTO -> ComposeAppTheme.colors.remus
        RiskLevel.LIMITED -> ComposeAppTheme.colors.ocean
        RiskLevel.CONTROLLED -> ComposeAppTheme.colors.jacob
    }
    return HSString(text, color, false)
}

@Composable
private fun getPriceImpact(priceImpactData: PriceImpactData?): HSString? {
    if (priceImpactData == null) {
        return null
    }
    val color = when (priceImpactData.priceImpactLevel) {
        PriceImpactLevel.Normal -> null
        PriceImpactLevel.Warning -> ComposeAppTheme.colors.jacob
        else -> ComposeAppTheme.colors.lucian
    }
    val value =
        App.numberFormatter.format(priceImpactData.priceImpact, 0, 2, prefix = "-", suffix = "%")
    return "($value)".hs(color = color)
}

@Preview
@Composable
private fun SwapSelectProviderScreenPreview() {
    ComposeAppTheme(darkTheme = false) {
        SwapSelectProviderScreenInner(
            onClickClose = {},
            quotes = listOf(
//                SwapProviderQuote(
//                    SwapMainModule.OneInchProvider,
//                    quote.amountOut,
//                    quote.fee,
//                    quote.fields
//                ),
//                SwapProviderQuote(
//                    SwapMainModule.UniswapV3Provider,
//                    quote.amountOut,
//                    quote.fee,
//                    quote.fields
//                ),
            ),
            currentQuote = null,
            sortType = ProviderSortType.BestPrice,
            onSortTypeChange = {},
            onBadgeClick = {},
            onSelectQuote = {}
        )
    }
}
