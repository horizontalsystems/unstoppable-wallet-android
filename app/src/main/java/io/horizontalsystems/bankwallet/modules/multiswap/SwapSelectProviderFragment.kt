package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.multiswap.providers.RiskLevel
import io.horizontalsystems.bankwallet.modules.nav3.NavController
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.captionSB_grey
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subheadSB_leah
import io.horizontalsystems.bankwallet.uiv3.components.BoxBordered
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellLeftImage
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
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
    if (previousBackStackEntry == null) {
        navController.removeLastOrNull()
        return
    }
    val swapViewModel = viewModel<SwapViewModel>(
        viewModelStoreOwner = previousBackStackEntry,
    )
    val viewModel = viewModel<SwapSelectProviderViewModel>(
        factory = SwapSelectProviderViewModel.Factory(
            swapViewModel.uiState.quotes,
            swapViewModel.uiState.quote
        )
    )

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
            navController.slideFromBottom(R.id.riskLevelInfoBottomSheet)
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
                                CellLeftImage(
                                    painter = painterResource(provider.icon),
                                    type = ImageType.Rectangle,
                                    size = 32
                                )
                            },
                            middle = {
                                Row(
                                    horizontalArrangement = spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row {
                                            headline2_leah(
                                                text = provider.title,
                                                modifier = Modifier.weight(1f)
                                            )
                                            subheadSB_leah(viewItem.tokenAmount)
                                        }

                                        Row {
                                            Row(
                                                horizontalArrangement = spacedBy(4.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(end = 4.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.clock_filled_24),
                                                    modifier = Modifier.size(16.dp),
                                                    tint = ComposeAppTheme.colors.grey,
                                                    contentDescription = null
                                                )
                                                captionSB_grey(
                                                    text = (viewItem.estimationTime?.let {
                                                        formatDurationShort(
                                                            it
                                                        )
                                                    } ?: stringResource(R.string.NotAvailable))
                                                )

                                                RiskCell(
                                                    modifier = Modifier.clickable {
                                                        onBadgeClick.invoke()
                                                    },
                                                    provider.riskLevel
                                                )
                                            }
                                            Row(
                                                horizontalArrangement = spacedBy(4.dp)
                                            ) {
                                                viewItem.fiatAmount?.let {
                                                    Text(
                                                        text = it,
                                                        style = ComposeAppTheme.typography.captionSB,
                                                        color = ComposeAppTheme.colors.grey,
                                                        textAlign = TextAlign.End,
                                                    )
                                                }
                                                getPriceImpact(viewItem.priceImpactData)?.let {
                                                    Text(
                                                        text = it.text,
                                                        style = ComposeAppTheme.typography.captionSB,
                                                        color = it.color ?: ComposeAppTheme.colors.grey,
                                                        textAlign = TextAlign.End,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Icon(
                                        modifier = Modifier
                                            .size(20.dp),
                                        painter = painterResource(icon),
                                        contentDescription = null,
                                        tint = iconTint
                                    )
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
fun RiskCell(
    modifier: Modifier = Modifier,
    riskLevel: RiskLevel,
) {
    val color = when (riskLevel) {
        RiskLevel.AUTO -> ComposeAppTheme.colors.remus
        RiskLevel.CONTROLLED -> ComposeAppTheme.colors.jacob
        RiskLevel.LIMITED -> ComposeAppTheme.colors.ocean
        RiskLevel.PRECHECK -> ComposeAppTheme.colors.leah
    }
    val icon = riskLevel.icon
    Row(modifier = modifier) {
        Icon(
            painter = painterResource(icon),
            modifier = Modifier.size(16.dp),
            tint = color,
            contentDescription = null
        )
        HSpacer(4.dp)
        Text(
            text = stringResource(riskLevel.title),
            style = ComposeAppTheme.typography.captionSB,
            color = color,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
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
