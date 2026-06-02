package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.multiswap.providers.RiskLevel
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.nav3.viewModelForScreen
import io.horizontalsystems.bankwallet.modules.multiswap.ui.RiskScore
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead_leah
import io.horizontalsystems.bankwallet.uiv3.components.BoxBordered
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.HSString
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSDropdownButton
import io.horizontalsystems.bankwallet.uiv3.components.menu.MenuGroup
import io.horizontalsystems.bankwallet.uiv3.components.menu.MenuItemX
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabsSectionButtons
import kotlinx.serialization.Serializable

@Serializable
data class SwapSelectProviderPage(val parentScreenContentKey: String) : HSPage() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        SwapSelectProviderScreen(navController, parentScreenContentKey)
    }
}

@Composable
fun SwapSelectProviderScreen(
    navController: HSNavigation,
    parentScreenContentKey: String
) {
    val swapViewModel = viewModelForScreen<SwapViewModel>(parentScreenContentKey)
    val viewModel = hiltViewModel<SwapSelectProviderViewModel, SwapSelectProviderViewModel.Factory> { factory ->
        factory.create(swapViewModel.uiState.quotes, swapViewModel.uiState.quote)
    }

    val uiState = viewModel.uiState

    SwapSelectProviderScreenInner(
        onClickClose = navController::removeLastOrNull,
        quotes = uiState.quoteViewItems,
        currentQuote = uiState.selectedQuote,
        sortType = uiState.sortType,
        onSortTypeChange = {
            viewModel.setSortType(it)
        },
        onBadgeClick = {
            navController.slideFromBottom(RiskLevelInfoSheet)
        }
    ) {
        swapViewModel.onSelectQuote(it)
        navController.removeLastOrNull()

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
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    painter = painterResource(icon),
                                    contentDescription = null,
                                    tint = iconTint
                                )
                            },
                            middle = {
                                Row(
                                    horizontalArrangement = spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = spacedBy(3.dp)
                                    ) {
                                        subhead_leah(viewItem.tokenAmount)
                                        Row(
                                            horizontalArrangement = spacedBy(4.dp)
                                        ) {
                                            viewItem.fiatAmount?.let {
                                                Text(
                                                    text = it,
                                                    style = ComposeAppTheme.typography.subhead,
                                                    color = ComposeAppTheme.colors.grey,
                                                )
                                            }
                                            getPriceImpact(viewItem.priceImpactData)?.let {
                                                Text(
                                                    text = it.text,
                                                    style = ComposeAppTheme.typography.subhead,
                                                    color = it.color ?: ComposeAppTheme.colors.grey,
                                                )
                                            }
                                        }
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = spacedBy(3.dp)
                                    ) {
                                        RiskScore(
                                            riskLevel = provider.riskLevel,
                                            modifier = Modifier.clickable {
                                                onBadgeClick.invoke()
                                            },
                                        )
                                        Text(
                                            text = viewItem.estimationTime?.let {
                                                formatDurationShort(it)
                                            } ?: stringResource(R.string.NotAvailable),
                                            style = ComposeAppTheme.typography.subheadSB,
                                            color = if (viewItem.timeStatus == SwapTimeStatus.Attention) {
                                                ComposeAppTheme.colors.jacob
                                            } else {
                                                ComposeAppTheme.colors.grey
                                            },
                                        )
                                    }
                                }
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

fun formatDurationShort(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return buildString {
        if (hours > 0) append("${hours}h ")
        if (minutes > 0) append("${minutes}m ")
        if (seconds > 0 || (hours == 0L && minutes == 0L)) append("${seconds}s")
    }.trim()
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
