package io.horizontalsystems.bankwallet.modules.multiswap

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderStick
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.HsImageCircle
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs

class SwapHistoryFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SwapHistoryScreen(navController)
    }
}

@Composable
fun SwapHistoryScreen(navController: NavController) {
    val viewModel = viewModel<SwapHistoryViewModel>(factory = SwapHistoryViewModel.Factory())
    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.SwapHistory_Title),
        onBack = navController::popBackStack,
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
                        SwapHistoryCell(
                            item = item,
                            onClick = {
                                navController.slideFromRight(
                                    R.id.swapInfoFragment,
                                    SwapInfoFragment.Input(item.id),
                                )
                            },
                        )
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
                            title = item.amountIn.hs,
                            subtitle = item.fiatAmountIn?.hs,
                        )
                    }
                    val (statusIcon, statusTint) = statusIconAndTint(item.status)
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(statusIcon),
                        tint = statusTint,
                        contentDescription = null,
                    )
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        CellRightInfo(
                            title = (item.amountOut ?: "---").hs,
                            subtitle = item.fiatAmountOut?.hs,
                        )
                    }
                }
            },
            right = {
                SwapCoinIcon(
                    imageUrl = item.tokenOutImageUrl,
                    showSpinner = item.status == SwapStatus.Swapping || item.status == SwapStatus.Sending,
                )
            },
            onClick = onClick,
        )
        HsDivider()
    }
}

@Composable
private fun SwapCoinIcon(imageUrl: String, showSpinner: Boolean) {
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
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
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
    SwapStatus.Failed -> Pair(R.drawable.ic_warning_filled_20, ComposeAppTheme.colors.redL)
}
