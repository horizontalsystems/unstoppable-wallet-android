package io.horizontalsystems.walletkit.modules.multiswap.history

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.HeaderStick
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
import io.horizontalsystems.walletkit.ui.compose.components.HsImageCircle
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.body_grey
import io.horizontalsystems.walletkit.ui.compose.components.captionSB_grey
import io.horizontalsystems.walletkit.ui.compose.components.subheadSB_leah
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import kotlinx.serialization.Serializable

@Serializable
data object SwapHistoryPage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        SwapHistoryScreen(navigation)
    }
}

@Composable
fun SwapHistoryScreen(navigation: HSNavigation) {
    val viewModel = viewModel<SwapHistoryViewModel>(factory = SwapHistoryViewModel.Factory())
    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.SwapHistory_Title),
        onBack = navigation::removeLastOrNull,
    ) {
        if (uiState.items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                body_grey(
                    text = stringResource(R.string.SwapHistory_EmptyList),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ComposeAppTheme.colors.lawrence),
            ) {
                uiState.items.forEach { (dateHeader, swaps) ->
                    stickyHeader {
                        HeaderStick(
                            borderBottom = true,
                            text = dateHeader.uppercase(),
                            color = ComposeAppTheme.colors.lawrence,
                        )
                    }
                    items(swaps, key = { it.id }) { item ->
                        val onClick = {
                            navigation.slideFromRight(
                                SwapInfoPage(SwapInfoPage.Input(item.id)),
                            )
                        }
                        when (item.operation) {
                            SwapOperation.Swap -> SwapHistoryCell(item = item, onClick = onClick)
                            SwapOperation.PrivateSend,
                            SwapOperation.CrossPay -> OperationHistoryCell(item = item, onClick = onClick)
                        }
                    }
                }
                item { VSpacer(32.dp) }
            }
        }
    }
}

@Composable
private fun SwapHistoryCell(item: SwapHistoryViewItem, onClick: () -> Unit) {
    Column {
        CellPrimary(
            left = {
                SwapCoinIcon(
                    imageUrl = item.tokenInImageUrl,
                    alternativeImageUrl = item.tokenInAlternativeImageUrl,
                    showSpinner = item.status == SwapStatus.Depositing,
                )
            },
            middle = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CellMiddleInfo(
                            subtitle = item.amountIn.hs(ComposeAppTheme.colors.leah),
                            description = item.fiatAmountIn?.hs,
                        )
                    }
                    val (statusIcon, statusTint) = statusIconAndTint(item.status)
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(statusIcon),
                        tint = statusTint,
                        contentDescription = null,
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End,
                    ) {
                        subheadSB_leah(
                            text = item.amountOut ?: "---",
                            textAlign = TextAlign.End,
                        )

                        item.fiatAmountOut?.let {
                            captionSB_grey(
                                text = it,
                                textAlign = TextAlign.End,
                            )
                        }
                    }
                }
            },
            right = {
                SwapCoinIcon(
                    imageUrl = item.tokenOutImageUrl,
                    alternativeImageUrl = item.tokenOutAlternativeImageUrl,
                    showSpinner = item.status == SwapStatus.Swapping || item.status == SwapStatus.Sending,
                )
            },
            onClick = onClick,
        )
        HsDivider()
    }
}

/**
 * A private send or CrossPay row: the product name and its current stage instead of a
 * token pair. Only the spent side is known to the user as "theirs", so the amount reads as
 * an outgoing value.
 */
@Composable
private fun OperationHistoryCell(item: SwapHistoryViewItem, onClick: () -> Unit) {
    val inProgress = item.status in listOf(SwapStatus.Depositing, SwapStatus.Swapping, SwapStatus.Sending)
    Column {
        CellPrimary(
            left = {
                SwapCoinIcon(
                    imageUrl = item.tokenInImageUrl,
                    alternativeImageUrl = item.tokenInAlternativeImageUrl,
                    showSpinner = inProgress,
                )
            },
            middle = {
                CellMiddleInfo(
                    subtitle = operationTitle(item.operation).hs(ComposeAppTheme.colors.leah),
                    description = statusText(item.status).hs(color = statusTextColor(item.status)),
                )
            },
            right = {
                Column(horizontalAlignment = Alignment.End) {
                    subheadSB_leah(
                        text = "-${item.amountIn}",
                        textAlign = TextAlign.End,
                    )
                    item.fiatAmountIn?.let {
                        captionSB_grey(
                            text = "-$it",
                            textAlign = TextAlign.End,
                        )
                    }
                }
            },
            onClick = onClick,
        )
        HsDivider()
    }
}

@Composable
fun operationTitle(operation: SwapOperation): String = when (operation) {
    SwapOperation.Swap -> stringResource(R.string.SwapInfo_Title)
    SwapOperation.PrivateSend -> stringResource(R.string.PrivateSend_Toggle_Title)
    SwapOperation.CrossPay -> stringResource(R.string.CrossPay_Info_Title)
}

@Composable
private fun statusText(status: SwapStatus): String = when (status) {
    SwapStatus.Depositing -> stringResource(R.string.SwapHistory_StatusDepositing)
    SwapStatus.Swapping -> stringResource(R.string.SwapHistory_StatusSwapping)
    SwapStatus.Sending -> stringResource(R.string.SwapHistory_StatusSending)
    SwapStatus.Completed -> stringResource(R.string.SwapInfo_StatusCompleted)
    SwapStatus.Refunded -> stringResource(R.string.SwapInfo_StatusRefunded)
    SwapStatus.Failed -> stringResource(R.string.SwapInfo_StatusFailed)
    SwapStatus.ActionRequired -> stringResource(R.string.SwapHistory_StatusActionRequired)
}

@Composable
private fun statusTextColor(status: SwapStatus): Color = when (status) {
    SwapStatus.Completed -> ComposeAppTheme.colors.remus
    SwapStatus.Failed,
    SwapStatus.ActionRequired -> ComposeAppTheme.colors.lucian
    else -> ComposeAppTheme.colors.grey
}

@Composable
private fun SwapCoinIcon(imageUrl: String, alternativeImageUrl: String?, showSpinner: Boolean) {
    val leah = ComposeAppTheme.colors.leah
    val andy = ComposeAppTheme.colors.andy

    val rotate by if (showSpinner) {
        rememberInfiniteTransition(label = "spinner").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing)
            ),
            label = "rotate",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .drawBehind {
                if (showSpinner) {
                    inset(-2.dp.toPx()) {
                        drawArc(
                            color = andy,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                        )
                        rotate(degrees = rotate) {
                            drawArc(
                                color = leah,
                                startAngle = 0f,
                                sweepAngle = -120f,
                                useCenter = false,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                            )
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        HsImageCircle(
            modifier = Modifier.size(32.dp),
            url = imageUrl,
            alternativeUrl = alternativeImageUrl,
            placeholder = R.drawable.coin_placeholder,
        )
    }
}

@Composable
private fun statusIconAndTint(status: SwapStatus): Pair<Int, Color> = when (status) {
    SwapStatus.Depositing,
    SwapStatus.Swapping,
    SwapStatus.Sending -> Pair(R.drawable.arrow_m_right_24, ComposeAppTheme.colors.grey)

    SwapStatus.Completed -> Pair(R.drawable.ic_done_filled_20, ComposeAppTheme.colors.remus)
    SwapStatus.Refunded -> Pair(R.drawable.ic_arrow_return_20, ComposeAppTheme.colors.grey)
    SwapStatus.Failed,
    SwapStatus.ActionRequired -> Pair(R.drawable.ic_warning_filled_20, ComposeAppTheme.colors.redL)
}

@Preview
@Composable
private fun SwapHistoryCellPreview() {
    val ethUrl = ""
    val btcUrl = ""
    val usdtUrl = ""

    val items = listOf(
        SwapHistoryViewItem(
            id = 1,
            tokenInImageUrl = ethUrl,
            tokenOutImageUrl = btcUrl,
            tokenInAlternativeImageUrl = null,
            tokenOutAlternativeImageUrl = null,
            amountIn = "1.5 ETH",
            amountOut = "0.0004203 BTC",
            fiatAmountIn = "$2,850.00",
            fiatAmountOut = "$2,831.40",
            status = SwapStatus.Completed,
            operation = SwapOperation.Swap,
            formattedDate = "March 17",
        ),
        SwapHistoryViewItem(
            id = 2,
            tokenInImageUrl = usdtUrl,
            tokenOutImageUrl = ethUrl,
            tokenInAlternativeImageUrl = null,
            tokenOutAlternativeImageUrl = null,
            amountIn = "500 USDT",
            amountOut = null,
            fiatAmountIn = "$500.00",
            fiatAmountOut = null,
            status = SwapStatus.Swapping,
            operation = SwapOperation.Swap,
            formattedDate = "March 17",
        ),
        SwapHistoryViewItem(
            id = 3,
            tokenInImageUrl = btcUrl,
            tokenOutImageUrl = usdtUrl,
            tokenInAlternativeImageUrl = null,
            tokenOutAlternativeImageUrl = null,
            amountIn = "0.1 BTC",
            amountOut = "6,720 USDT",
            fiatAmountIn = "$6,720.00",
            fiatAmountOut = "$6,720.00",
            status = SwapStatus.Refunded,
            operation = SwapOperation.Swap,
            formattedDate = "March 16",
        ),
        SwapHistoryViewItem(
            id = 4,
            tokenInImageUrl = ethUrl,
            tokenOutImageUrl = usdtUrl,
            tokenInAlternativeImageUrl = null,
            tokenOutAlternativeImageUrl = null,
            amountIn = "0.5 ETH",
            amountOut = null,
            fiatAmountIn = "$950.00",
            fiatAmountOut = null,
            status = SwapStatus.Failed,
            operation = SwapOperation.Swap,
            formattedDate = "March 16",
        ),
        SwapHistoryViewItem(
            id = 5,
            tokenInImageUrl = usdtUrl,
            tokenOutImageUrl = usdtUrl,
            tokenInAlternativeImageUrl = null,
            tokenOutAlternativeImageUrl = null,
            amountIn = "10,536.84 USDT",
            amountOut = "10,500 USDT",
            fiatAmountIn = "$10,532.62",
            fiatAmountOut = "$10,498.12",
            status = SwapStatus.Depositing,
            operation = SwapOperation.PrivateSend,
            formattedDate = "March 16",
        ),
        SwapHistoryViewItem(
            id = 6,
            tokenInImageUrl = btcUrl,
            tokenOutImageUrl = usdtUrl,
            tokenInAlternativeImageUrl = null,
            tokenOutAlternativeImageUrl = null,
            amountIn = "0.1137 BTC",
            amountOut = "10,536.8 USDT",
            fiatAmountIn = "$10,498.12",
            fiatAmountOut = "$10,498.12",
            status = SwapStatus.Completed,
            operation = SwapOperation.CrossPay,
            formattedDate = "March 16",
        ),
    )
    ComposeAppTheme(darkTheme = false) {
        Column {
            items.forEach { item ->
                when (item.operation) {
                    SwapOperation.Swap -> SwapHistoryCell(item = item, onClick = {})
                    else -> OperationHistoryCell(item = item, onClick = {})
                }
            }
        }
    }
}
