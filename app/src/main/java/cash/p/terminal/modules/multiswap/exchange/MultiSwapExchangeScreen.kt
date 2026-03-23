package cash.p.terminal.modules.multiswap.exchange

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.multiswap.PriceField
import cash.p.terminal.modules.multiswap.PriceImpactField
import cash.p.terminal.ui.compose.components.HSRow
import cash.p.terminal.ui_compose.BottomSheetHeader
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui_compose.components.ButtonPrimaryRed
import cash.p.terminal.ui_compose.components.ButtonPrimaryTransparent
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.CellUniversal
import cash.p.terminal.ui_compose.components.HFillSpacer
import cash.p.terminal.ui_compose.components.HSCircularProgressIndicator
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.HsImageCircle
import cash.p.terminal.ui_compose.components.MenuItemTimeoutIndicator
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.caption_grey
import cash.p.terminal.ui_compose.components.subhead1_grey
import cash.p.terminal.ui_compose.components.subhead1_leah
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.entities.CurrencyValue
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MultiSwapExchangeScreen(
    uiState: MultiSwapExchangeUiState?,
    timeRemainingProgress: () -> Float?,
    onSwap: () -> Unit,
    onRefresh: () -> Unit,
    onContinueLater: () -> Unit,
    onDeleteAndClose: () -> Unit,
    onBack: () -> Unit,
    onClickProvider: () -> Unit,
    onClickLeg1: () -> Unit = {},
    swapButtonTitle: String = stringResource(R.string.Swap),
) {
    var showCancelConfirmation by remember { mutableStateOf(false) }

    if (showCancelConfirmation) {
        CancelSwapBottomSheet(
            onConfirm = {
                showCancelConfirmation = false
                onDeleteAndClose()
            },
            onDismiss = { showCancelConfirmation = false },
        )
    }

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.Swap),
                navigationIcon = { HsBackButton(onClick = onBack) },
                menuItems = buildList {
                    timeRemainingProgress()?.let { progress ->
                        add(MenuItemTimeoutIndicator(progress))
                    }
                },
            )
        }
    ) { paddingValues ->
        if (uiState == null) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                VSpacer(height = 12.dp)
                // Leg1 header center = 24dp, Leg2 header center = 20dp
                val dotStartPadding = 8.dp
                val cardStartPadding = dotStartPadding + 8.dp + 8.dp // dot area + gap
                val leg1DotOffset = 24.dp - 4.dp // header center - half dot
                var leg1CardHeight by remember { mutableIntStateOf(0) }
                val density = LocalDensity.current
                val gapBetweenCards = 40.dp
                val leg2HeaderCenter = 20.dp
                val leg2DotOffset = with(density) {
                    leg1CardHeight.toDp() + gapBetweenCards + leg2HeaderCenter - 4.dp
                }

                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                ) {
                    // Cards column
                    Column(
                        modifier = Modifier.padding(start = cardStartPadding)
                    ) {
                        LegCard(
                            leg = uiState.leg1,
                            borderColor = ComposeAppTheme.colors.grey,
                            modifier = Modifier.onSizeChanged { leg1CardHeight = it.height },
                            content = {
                                Leg1Header(
                                    providerName = uiState.leg1.providerName,
                                    status = uiState.leg1.status,
                                    coinIconUrlIn = uiState.leg1.coinIconUrlIn,
                                    coinIconUrlOut = uiState.leg1.coinIconUrlOut,
                                    onClick = if (uiState.leg1Clickable) onClickLeg1 else null,
                                )
                                LegContent(uiState.leg1)
                            }
                        )
                        VSpacer(height = gapBetweenCards)
                        LegCard(
                            leg = uiState.leg2,
                            borderColor = ComposeAppTheme.colors.steel20,
                            content = {
                                Leg2Header(
                                    providerName = uiState.leg2.providerName,
                                    providerIcon = uiState.leg2.providerIcon,
                                    clickable = uiState.leg2ProviderClickable,
                                    quoting = uiState.leg2Quoting,
                                    onClickProvider = onClickProvider,
                                )
                                LegContent(uiState.leg2)
                            }
                        )
                    }
                    // Dots + line overlay
                    StatusDot(
                        status = uiState.leg1.status,
                        modifier = Modifier
                            .padding(start = dotStartPadding)
                            .offset(y = leg1DotOffset),
                    )
                    StatusDot(
                        status = uiState.leg2.status,
                        modifier = Modifier
                            .padding(start = dotStartPadding)
                            .offset(y = leg2DotOffset),
                    )
                    VerticalLine(
                        isDotted = uiState.leg1.status != LegStatus.Completed,
                        modifier = Modifier
                            .padding(start = dotStartPadding + 4.5.dp)
                            .offset(y = leg1DotOffset + 15.dp)
                            .height(leg2DotOffset - leg1DotOffset - 20.dp),
                    )
                }
                VSpacer(height = 24.dp)
            }

            BottomButtons(
                buttonState = uiState.buttonState,
                showContinueLater = uiState.showContinueLater,
                swapButtonTitle = swapButtonTitle,
                onSwap = onSwap,
                onRefresh = onRefresh,
                onClose = onContinueLater,
                onDeleteAndClose = { showCancelConfirmation = true },
            )
        }
    }
}

@Composable
private fun StatusDot(status: LegStatus, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(statusDotColor(status))
    )
}

@Composable
private fun VerticalLine(isDotted: Boolean, modifier: Modifier = Modifier) {
    val color = ComposeAppTheme.colors.grey
    if (isDotted) {
        Canvas(modifier = modifier.width(1.dp)) {
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
            drawLine(
                color = color,
                start = Offset(size.width / 2, 0f),
                end = Offset(size.width / 2, size.height),
                strokeWidth = 1.dp.toPx(),
                pathEffect = pathEffect,
            )
        }
    } else {
        Box(
            modifier = modifier
                .width(1.dp)
                .background(color)
        )
    }
}

@Composable
private fun LegCard(
    leg: LegUiState,
    borderColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        content()
    }
}

@Composable
private fun LegContent(leg: LegUiState) {
    // You Send
    AmountRow(
        title = stringResource(R.string.swap_you_send),
        badge = leg.badgeIn,
        amountFormatted = leg.amountInFormatted?.let { "$it ${leg.coinIn}" },
        fiatAmount = leg.fiatAmountIn,
        currency = leg.currency,
        amountColor = ComposeAppTheme.colors.leah,
    )
    // You Get
    AmountRow(
        title = stringResource(R.string.swap_you_receive),
        badge = leg.badgeOut,
        amountFormatted = leg.amountOutFormatted?.let { "$it ${leg.coinOut}" },
        fiatAmount = leg.fiatAmountOut,
        currency = leg.currency,
        amountColor = ComposeAppTheme.colors.remus,
    )
    // Price
    val tokenIn = leg.tokenIn
    val tokenOut = leg.tokenOut
    val amountIn = leg.amountIn
    val amountOut = leg.amountOut
    if (tokenIn != null && tokenOut != null && amountIn != null && amountOut != null) {
        PriceField(tokenIn, tokenOut, amountIn, amountOut, borderTop = true)
        PriceImpactField(leg.priceImpact, leg.priceImpactLevel)
    }
}

@Composable
private fun AmountRow(
    title: String,
    badge: String?,
    amountFormatted: String?,
    fiatAmount: BigDecimal?,
    currency: Currency?,
    amountColor: Color,
) {
    CellUniversal(borderTop = true) {
        Column {
            subhead2_leah(text = title)
            VSpacer(height = 1.dp)
            caption_grey(text = badge ?: stringResource(R.string.CoinPlatforms_Native))
        }
        HFillSpacer(minWidth = 16.dp)
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = amountFormatted ?: "---",
                style = ComposeAppTheme.typography.subhead1,
                color = amountColor,
            )
            if (fiatAmount != null && currency != null) {
                VSpacer(height = 1.dp)
                caption_grey(text = CurrencyValue(currency, fiatAmount).getFormattedFull())
            }
        }
    }
}

@Composable
private fun Leg1Header(
    providerName: String?,
    status: LegStatus,
    coinIconUrlIn: String?,
    coinIconUrlOut: String?,
    onClick: (() -> Unit)? = null,
) {
    CellUniversal(
        borderTop = false,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.size(42.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (status == LegStatus.Executing) {
                HSCircularProgressIndicator(progress = 0.15f)
            }
            HsImageCircle(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 4.dp, start = 6.dp)
                    .size(24.dp),
                url = coinIconUrlIn,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 4.5.dp, end = 6.5.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(ComposeAppTheme.colors.tyler)
            )
            HsImageCircle(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 4.dp, end = 6.dp)
                    .size(24.dp),
                url = coinIconUrlOut,
            )
        }
        HSpacer(width = 16.dp)
        Column {
            val titleText = when (status) {
                LegStatus.Completed -> stringResource(R.string.multi_swap_completed)
                else -> stringResource(R.string.Swap)
            }
            subhead1_leah(text = titleText)
            if (providerName != null) {
                subhead2_grey(text = providerName)
            }
        }
    }
}

@Composable
private fun Leg2Header(
    providerName: String?,
    providerIcon: Int?,
    clickable: Boolean,
    quoting: Boolean,
    onClickProvider: () -> Unit,
) {
    HSRow(
        modifier = Modifier
            .height(40.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        borderBottom = true,
    ) {
        if (clickable) {
            Selector(
                icon = {
                    if (providerIcon != null) {
                        Image(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(providerIcon),
                            contentDescription = null
                        )
                    }
                },
                text = {
                    subhead1_leah(text = providerName ?: "")
                },
                onClickSelect = onClickProvider
            )
        } else {
            if (providerIcon != null) {
                Image(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(providerIcon),
                    contentDescription = null
                )
                HSpacer(width = 8.dp)
            }
            when {
                providerName != null -> subhead1_leah(text = providerName)
                quoting -> subhead1_grey(text = stringResource(R.string.multi_swap_finding_best_provider))
                else -> subhead1_grey(text = stringResource(R.string.multi_swap_no_providers))
            }
        }
    }
}

@Composable
private fun Selector(
    icon: @Composable (RowScope.() -> Unit),
    text: @Composable (RowScope.() -> Unit),
    onClickSelect: () -> Unit,
) {
    Row(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClickSelect,
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon.invoke(this)
        HSpacer(width = 8.dp)
        text.invoke(this)
        HSpacer(width = 8.dp)
        Icon(
            painter = painterResource(R.drawable.ic_arrow_big_down_20),
            contentDescription = "",
            tint = ComposeAppTheme.colors.grey
        )
    }
}

@Composable
private fun BottomButtons(
    buttonState: ButtonState,
    showContinueLater: Boolean,
    swapButtonTitle: String,
    onSwap: () -> Unit,
    onRefresh: () -> Unit,
    onClose: () -> Unit,
    onDeleteAndClose: () -> Unit,
) {
    val buttonModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)

    ButtonsGroupWithShade {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when (buttonState) {
                ButtonState.Close -> ButtonPrimaryYellow(
                    modifier = buttonModifier,
                    title = stringResource(R.string.Button_Close),
                    onClick = onClose,
                )

                ButtonState.Enabled -> {
                    ButtonPrimaryYellow(
                        modifier = buttonModifier,
                        title = swapButtonTitle,
                        onClick = onSwap,
                    )
                }

                ButtonState.Refresh -> {
                    ButtonPrimaryDefault(
                        modifier = buttonModifier,
                        title = stringResource(R.string.Button_Refresh),
                        onClick = onRefresh,
                    )
                }

                ButtonState.Quoting -> ButtonPrimaryYellow(
                    modifier = buttonModifier,
                    title = stringResource(R.string.Swap_Quoting),
                    enabled = false,
                    loadingIndicator = true,
                    onClick = {},
                )

                ButtonState.Hidden -> { /* no primary button */
                }

                ButtonState.Disabled -> ButtonPrimaryYellow(
                    modifier = buttonModifier,
                    title = swapButtonTitle,
                    onClick = {},
                    enabled = false,
                )
            }

            if (showContinueLater) {
                VSpacer(height = 8.dp)
                ButtonPrimaryTransparent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    title = stringResource(R.string.swap_continue_later),
                    onClick = onClose,
                )
                VSpacer(height = 8.dp)
                ButtonPrimaryTransparent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    title = stringResource(R.string.Button_Cancel),
                    textColor = ComposeAppTheme.colors.lucian,
                    onClick = onDeleteAndClose,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CancelSwapBottomSheet(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        sheetState = sheetState,
        containerColor = ComposeAppTheme.colors.transparent,
    ) {
        val buttonModifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)

        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_attention_24),
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.lucian),
            title = stringResource(R.string.multi_swap_cancel_swap),
            onCloseClick = {
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            },
        ) {
            VSpacer(12.dp)
            TextImportantWarning(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.multi_swap_cancel_swap_warning),
            )
            VSpacer(32.dp)
            ButtonPrimaryRed(
                modifier = buttonModifier,
                title = stringResource(R.string.Button_Delete),
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onConfirm() }
                },
            )
            VSpacer(12.dp)
            ButtonPrimaryTransparent(
                modifier = buttonModifier,
                title = stringResource(R.string.Button_Cancel),
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
            )
            VSpacer(32.dp)
        }
    }
}

@Composable
private fun statusDotColor(status: LegStatus) = when (status) {
    LegStatus.Pending -> ComposeAppTheme.colors.grey
    LegStatus.Executing -> ComposeAppTheme.colors.jacob
    LegStatus.Completed -> ComposeAppTheme.colors.remus
    LegStatus.Failed -> ComposeAppTheme.colors.lucian
}

@Preview
@Composable
private fun Leg1HeaderPreview() {
    ComposeAppTheme {
        Column {
            Leg1Header(
                providerName = "STON.fi",
                status = LegStatus.Executing,
                coinIconUrlIn = null,
                coinIconUrlOut = null,
            )
            Leg1Header(
                providerName = "STON.fi",
                status = LegStatus.Completed,
                coinIconUrlIn = null,
                coinIconUrlOut = null,
            )
        }
    }
}

@Preview
@Composable
private fun MultiSwapExchangeScreenPreview() {
    ComposeAppTheme {
        MultiSwapExchangeScreen(
            uiState = MultiSwapExchangeUiState(
                leg1 = LegUiState(
                    status = LegStatus.Executing,
                    providerName = "STON.fi",
                    coinIn = "PIRATE",
                    coinOut = "TONCOIN",
                    amountInFormatted = "50.8762",
                    amountOutFormatted = "7.2235",
                    fiatAmountIn = BigDecimal("0.99"),
                    fiatAmountOut = BigDecimal("0.95"),
                ),
                leg2 = LegUiState(
                    status = LegStatus.Pending,
                    providerName = "ChangeNow",
                    coinIn = "TONCOIN",
                    coinOut = "BNB",
                    amountInFormatted = "0.7576",
                    amountOutFormatted = "0.001476",
                    fiatAmountIn = BigDecimal("0.99"),
                    fiatAmountOut = BigDecimal("0.95"),
                ),
                buttonState = ButtonState.Disabled,
                showContinueLater = true,
            ),
            timeRemainingProgress = { null },
            onSwap = {},
            onRefresh = {},
            onContinueLater = {},
            onDeleteAndClose = {},
            onBack = {},
            onClickProvider = {},
        )
    }
}

@Preview
@Composable
private fun MultiSwapExchangeScreenCompletedPreview() {
    ComposeAppTheme {
        MultiSwapExchangeScreen(
            uiState = MultiSwapExchangeUiState(
                leg1 = LegUiState(
                    status = LegStatus.Completed,
                    providerName = "STON.fi",
                    coinIn = "PIRATE",
                    coinOut = "TONCOIN",
                    amountInFormatted = "50.8762",
                    amountOutFormatted = "7.2235",
                    fiatAmountIn = BigDecimal("0.99"),
                    fiatAmountOut = BigDecimal("0.95"),
                ),
                leg2 = LegUiState(
                    status = LegStatus.Pending,
                    providerName = "ChangeNow",
                    coinIn = "TONCOIN",
                    coinOut = "BNB",
                    amountInFormatted = "0.7576",
                    amountOutFormatted = "0.001476",
                    fiatAmountIn = BigDecimal("0.99"),
                    fiatAmountOut = BigDecimal("0.95"),
                ),
                buttonState = ButtonState.Enabled,
                showContinueLater = true,
            ),
            timeRemainingProgress = { 0.7f },
            onSwap = {},
            onRefresh = {},
            onContinueLater = {},
            onDeleteAndClose = {},
            onBack = {},
            onClickProvider = {},
        )
    }
}
