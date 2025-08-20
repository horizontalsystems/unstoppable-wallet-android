package io.horizontalsystems.bankwallet.modules.balance.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem2
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.diffText
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo

@Composable
fun BalanceCardInner2(
    viewItem: BalanceViewItem2,
    type: BalanceCardSubtitleType,
    onClickSyncError: (() -> Unit)? = null,
    onClick: (() -> Unit)?
) {
    CellPrimary(
        left = {
            WalletIcon2(viewItem, onClickSyncError)
        },
        middle = {
            val subtitle: String
            var subtitle2: String? = null

            if (viewItem.failedIconVisible) {
                subtitle = stringResource(R.string.BalanceSyncError_Text)
            } else if (viewItem.syncingTextValue != null) {
                subtitle = viewItem.syncingTextValue
            } else {
                when (type) {
                    BalanceCardSubtitleType.Rate -> {
                        // TODO("viewItem.exchangeValue.dimmed")
                        subtitle = viewItem.exchangeValue.value
                        // TODO("diffColor(viewItem.diff)")
                        subtitle2 = diffText(viewItem.diff)
                    }

                    BalanceCardSubtitleType.CoinName -> {
                        subtitle = viewItem.wallet.coin.name
                    }
                }
            }

            CellMiddleInfo(
                title = viewItem.wallet.coin.code,
                badge = viewItem.badge,
                subtitle = subtitle,
                subtitle2 = subtitle2
            )
        },
        right = {
            val subtitle = when {
                viewItem.syncedUntilTextValue != null -> viewItem.syncedUntilTextValue
                viewItem.secondaryValue.visible -> {
                    // TODO("viewItem.secondaryValue.dimmed")
                    viewItem.secondaryValue.value
                }
                else -> ""
            }

            // TODO("viewItem.primaryValue.dimmed")
            CellRightInfo(
                title = if (viewItem.primaryValue.visible) viewItem.primaryValue.value else "------",
                subtitle = subtitle
            )
        },
        onClick = onClick,
    )
}


@Composable
private fun WalletIcon2(
    viewItem: BalanceViewItem2,
    onClickSyncError: (() -> Unit)?
) {
    val transition = rememberInfiniteTransition()
    val rotate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1100,
                easing = LinearEasing
            )
        )
    )

    val progress = viewItem.syncingProgress.progress
    val iconAlpha = if (progress == null) 1f else 0.5f
    val leah = ComposeAppTheme.colors.leah
    val andy = ComposeAppTheme.colors.andy

    Box(
        modifier = Modifier
            .drawBehind {
                if (progress != null) {
                    val progressF = progress.coerceAtLeast(10) / 100f
                    val angle = 360f * progressF

                    inset(-1.dp.toPx()) {
                        drawArc(
                            color = andy,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )

                        rotate(degrees = rotate) {
                            drawArc(
                                color = leah,
                                startAngle = 0f,
                                sweepAngle = -angle,
                                useCenter = false,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }
    ) {
        IconCell(
            viewItem.failedIconVisible,
            viewItem.wallet.token,
            iconAlpha,
            onClickSyncError
        )
    }
}
