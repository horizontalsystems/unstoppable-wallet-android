package cash.p.terminal.modules.multiswap.exchanges

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.multiswap.exchange.CancelSwapBottomSheet
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.DraggableCardSimple
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.HsIconButton
import cash.p.terminal.ui_compose.components.HsImageCircle
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_grey
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
internal fun MultiSwapExchangesScreen(
    uiState: MultiSwapExchangesUiState,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
) {
    var revealedCardId by remember { mutableStateOf<String?>(null) }
    var deleteConfirmationId by remember { mutableStateOf<String?>(null) }

    deleteConfirmationId?.let { id ->
        CancelSwapBottomSheet(
            onConfirm = {
                deleteConfirmationId = null
                onDelete(id)
            },
            onDismiss = { deleteConfirmationId = null },
        )
    }

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.Transactions_Swaps),
                navigationIcon = { HsBackButton(onClick = onBack) },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item { VSpacer(height = 12.dp) }
            items(uiState.items, key = { it.id }) { item ->
                ExchangeCardSwipable(
                    item = item,
                    revealed = revealedCardId == item.id,
                    onReveal = { revealedCardId = item.id },
                    onConceal = { revealedCardId = null },
                    onClick = { onSelect(item.id) },
                    onDelete = { deleteConfirmationId = item.id },
                )
                VSpacer(height = 8.dp)
            }
            item { VSpacer(height = 12.dp) }
        }
    }
}

@Composable
private fun ExchangeCardSwipable(
    item: ExchangeCardItem,
    revealed: Boolean,
    onReveal: () -> Unit,
    onConceal: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        HsIconButton(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .width(88.dp),
            onClick = onDelete,
            content = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_circle_minus_24),
                    tint = ComposeAppTheme.colors.grey,
                    contentDescription = "delete",
                )
            }
        )
        DraggableCardSimple(
            key = item.id,
            isRevealed = revealed,
            cardOffset = 72f,
            onReveal = onReveal,
            onConceal = onConceal,
            content = {
                ExchangeCardContent(item = item, onClick = onClick)
            }
        )
    }
}

@Composable
private fun ExchangeCardContent(
    item: ExchangeCardItem,
    onClick: () -> Unit,
) {
    val cardShape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, ComposeAppTheme.colors.grey, cardShape)
            .clip(cardShape)
            .background(ComposeAppTheme.colors.lawrence)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                CoinIconChain(
                    coinIconUrlIn = item.coinIconUrlIn,
                    coinIconUrlIntermediate = item.coinIconUrlIntermediate,
                    coinIconUrlOut = item.coinIconUrlOut,
                )
                VSpacer(height = 8.dp)
                subhead2_leah(text = item.statusText)
            }
            AmountColumn(item = item)
        }
    }
}

@Composable
private fun CoinIconChain(
    coinIconUrlIn: String?,
    coinIconUrlIntermediate: String?,
    coinIconUrlOut: String?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HsImageCircle(
            modifier = Modifier.size(24.dp),
            url = coinIconUrlIn,
        )
        DashSeparator()
        HsImageCircle(
            modifier = Modifier.size(24.dp),
            url = coinIconUrlIntermediate,
        )
        DashSeparator()
        HsImageCircle(
            modifier = Modifier.size(24.dp),
            url = coinIconUrlOut,
        )
    }
}

@Composable
private fun DashSeparator() {
    HSpacer(width = 4.dp)
    Box(
        modifier = Modifier
            .width(12.dp)
            .height(1.dp)
            .background(ComposeAppTheme.colors.steel20)
    )
    HSpacer(width = 4.dp)
}

@Composable
private fun AmountColumn(item: ExchangeCardItem) {
    Column(horizontalAlignment = Alignment.End) {
        val amounts = listOf(
            item.amountInFormatted,
            item.amountIntermediateFormatted,
            item.amountOutFormatted,
        )
        amounts.forEachIndexed { index, text ->
            if (index > 0) VSpacer(height = 4.dp)
            if (index == item.activeLine) {
                body_leah(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                body_grey(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Preview
@Composable
private fun MultiSwapExchangesScreenPreview() {
    ComposeAppTheme {
        MultiSwapExchangesScreen(
            uiState = MultiSwapExchangesUiState(
                items = listOf(
                    ExchangeCardItem(
                        id = "1",
                        coinIconUrlIn = null,
                        coinIconUrlIntermediate = null,
                        coinIconUrlOut = null,
                        amountInFormatted = "1.5 ETH",
                        amountIntermediateFormatted = "3000 USDT",
                        amountOutFormatted = "--- BTC",
                        activeLine = 1,
                        statusText = "Continue exchange",
                    ),
                    ExchangeCardItem(
                        id = "2",
                        coinIconUrlIn = null,
                        coinIconUrlIntermediate = null,
                        coinIconUrlOut = null,
                        amountInFormatted = "0.5 BTC",
                        amountIntermediateFormatted = "--- USDT",
                        amountOutFormatted = "--- SOL",
                        activeLine = 0,
                        statusText = "Exchanging",
                    ),
                )
            ),
            onSelect = {},
            onDelete = {},
            onBack = {},
        )
    }
}
