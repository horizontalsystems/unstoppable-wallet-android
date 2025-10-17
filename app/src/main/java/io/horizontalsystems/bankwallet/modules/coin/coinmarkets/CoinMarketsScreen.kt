package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.MarketTickerViewItem
import io.horizontalsystems.bankwallet.modules.coin.coinmarkets.CoinMarketsModule.ExchangeType
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AlertGroup
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefaults
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondary
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryWithIcon
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderSorting
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.SecondaryButtonDefaults
import io.horizontalsystems.bankwallet.ui.compose.components.marketDataValueComponent
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.bankwallet.uiv3.components.BoxBordered
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellLeftImage
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.ImageType
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.marketkit.models.FullCoin
import kotlinx.coroutines.launch

@Composable
fun CoinMarketsScreen(
    fullCoin: FullCoin
) {
    val viewModel = viewModel<CoinMarketsViewModel>(factory = CoinMarketsModule.Factory(fullCoin))

    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }
    var showExchangeTypeSelector by rememberSaveable { mutableStateOf(false) }
    val uiState = viewModel.uiState

    Crossfade(uiState.viewState, label = "") { viewItemState ->
        when (viewItemState) {
            ViewState.Loading -> {
                Loading()
            }

            is ViewState.Error -> {
                ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
            }

            ViewState.Success -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (uiState.items.isEmpty()) {
                        ListEmptyView(
                            text = stringResource(R.string.CoinPage_NoDataAvailable),
                            icon = R.drawable.ic_no_data
                        )
                    } else {
                        CoinMarketsMenu(
                            exchangeTypeMenu = uiState.exchangeTypeMenu,
                            verified = viewModel.verified,
                            showExchangeTypeSelector = { showExchangeTypeSelector = true },
                            onVerifiedEnabled = { verified ->
                                viewModel.setVerified(verified)
                                scrollToTopAfterUpdate = true
                            }
                        )
                        CoinMarketList(uiState.items, scrollToTopAfterUpdate)
                        if (scrollToTopAfterUpdate) {
                            scrollToTopAfterUpdate = false
                        }
                    }
                }
            }
        }
    }
    if (showExchangeTypeSelector) {
        AlertGroup(
            title = stringResource(R.string.CoinPage_MarketsVerifiedMenu_ExchangeType),
            select = uiState.exchangeTypeMenu,
            onSelect = {
                viewModel::setExchangeType.invoke(it)
                showExchangeTypeSelector = false
            },
            onDismiss = { showExchangeTypeSelector = false }
        )
    }
}

@Composable
fun CoinMarketsMenu(
    exchangeTypeMenu: Select<ExchangeType>,
    verified: Boolean,
    showExchangeTypeSelector: () -> Unit,
    onVerifiedEnabled: (Boolean) -> Unit,
) {

    HeaderSorting(borderTop = true, borderBottom = true) {
        ButtonSecondaryWithIcon(
            modifier = Modifier.padding(start = 16.dp),
            iconRight = painterResource(R.drawable.ic_down_arrow_20),
            title = exchangeTypeMenu.selected.title.getString(),
            onClick = showExchangeTypeSelector
        )
        Spacer(Modifier.weight(1f))
        TurnOnButton(
            modifier = Modifier.padding(end = 16.dp),
            title = stringResource(R.string.CoinPage_MarketsVerifiedMenu_Verified),
            turnedOn = verified,
            onToggle = { verified ->
                onVerifiedEnabled.invoke(verified)
            }
        )
    }
}

@Composable
fun CoinMarketList(
    items: List<MarketTickerViewItem>,
    scrollToTop: Boolean,
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LazyColumn(state = listState) {
        items(items) { item ->
            BoxBordered(bottom = true) {
                CoinMarketCell(
                    item.market,
                    item.pair,
                    item.marketImageUrl ?: "",
                    item.volumeToken,
                    MarketDataValue.Volume(item.volumeFiat),
                    item.tradeUrl,
                    item.badge
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
        if (scrollToTop) {
            coroutineScope.launch {
                listState.scrollToItem(0)
            }
        }
    }
}

@Composable
fun CoinMarketCell(
    name: String,
    subtitle: String,
    iconUrl: String,
    volumeToken: String,
    marketDataValue: MarketDataValue,
    tradeUrl: String?,
    badge: TranslatableString?
) {
    val context = LocalContext.current
    CellPrimary(
        left = {
            CellLeftImage(
                type = ImageType.Rectangle,
                size = 32,
                painter = rememberAsyncImagePainter(
                    model = iconUrl,
                    error = painterResource(R.drawable.ic_platform_placeholder_24)
                ),
            )
        },
        middle = {
            CellMiddleInfo(
                title = name.hs,
                badge = badge?.toString()?.hs,
                subtitle = subtitle.hs,
            )
        },
        right = {
            CellRightInfo(
                title = volumeToken.hs,
                subtitle = marketDataValueComponent(marketDataValue)
            )
        },
        onClick = tradeUrl?.let {
            { LinkHelper.openLinkInAppBrowser(context, it) }
        }
    )
}

@Composable
fun TurnOnButton(
    modifier: Modifier,
    title: String,
    turnedOn: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val onClick = { onToggle.invoke(!turnedOn) }
    val buttonColors = if (turnedOn) {
        ButtonPrimaryDefaults.textButtonColors(
            backgroundColor = ComposeAppTheme.colors.yellowD,
            contentColor = ComposeAppTheme.colors.dark,
            disabledBackgroundColor = ComposeAppTheme.colors.blade,
            disabledContentColor = ComposeAppTheme.colors.andy,
        )
    } else {
        SecondaryButtonDefaults.buttonColors()
    }
    ButtonSecondary(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(
            start = 10.dp,
            end = 16.dp,
        ),
        buttonColors = buttonColors,
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = ComposeAppTheme.typography.captionSB,
                    color = if (turnedOn) ComposeAppTheme.colors.dark else ComposeAppTheme.colors.leah,
                )
            }
        },
    )
}