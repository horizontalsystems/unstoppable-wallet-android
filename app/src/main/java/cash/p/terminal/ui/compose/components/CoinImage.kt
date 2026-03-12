package cash.p.terminal.ui.compose.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cash.p.terminal.R
import cash.p.terminal.core.iconPlaceholder
import cash.p.terminal.modules.balance.SyncingProgress
import cash.p.terminal.modules.balance.SyncingProgressType
import cash.p.terminal.ui_compose.components.HsImage
import cash.p.terminal.ui_compose.components.HsImageCircle
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.alternativeImageUrl
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.imagePlaceholder
import cash.p.terminal.wallet.imageUrl

@Composable
fun CoinImage(
    coin: Coin?,
    modifier: Modifier,
    colorFilter: ColorFilter? = null
) = HsImage(
    url = coin?.imageUrl,
    alternativeUrl = coin?.alternativeImageUrl,
    placeholder = coin?.imagePlaceholder,
    modifier = modifier.clip(CircleShape),
    colorFilter = colorFilter
)

@Composable
fun CoinImage(
    token: Token?,
    modifier: Modifier,
    colorFilter: ColorFilter? = null
) = HsImageCircle(
    modifier,
    token?.coin?.imageUrl,
    token?.coin?.alternativeImageUrl,
    token?.iconPlaceholder,
    colorFilter
)

@Composable
fun CoinIconWithSyncProgress(
    token: Token,
    syncingProgress: SyncingProgress,
    failedIconVisible: Boolean,
    onClickSyncError: (() -> Unit)?,
    boxSize: Dp = 40.dp,
    iconSize: Dp = 32.dp
) {
    val transition = rememberInfiniteTransition(label = "sync_rotation")
    val rotate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1100,
                easing = LinearEasing
            )
        ),
        label = "rotation"
    )

    val progress = syncingProgress.progress
    val iconAlpha = if (syncingProgress.type == null) 1f else 0.3f
    val leah = ComposeAppTheme.colors.leah
    val circleColor = ComposeAppTheme.colors.steel10

    Box(
        modifier = Modifier
            .size(boxSize)
            .drawBehind {
                when (syncingProgress.type) {
                    SyncingProgressType.ProgressWithRing -> {
                        val progressF = (progress ?: 0).coerceAtLeast(10) / 100f
                        val angle = 360f * progressF

                        inset(-1.dp.toPx()) {
                            drawArc(
                                color = circleColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                            rotate(degrees = -90f) {
                                drawArc(
                                    color = leah,
                                    startAngle = 0f,
                                    sweepAngle = angle,
                                    useCenter = false,
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }
                    }

                    SyncingProgressType.Spinner -> {
                        inset(-1.dp.toPx()) {
                            drawArc(
                                color = circleColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                            rotate(degrees = rotate) {
                                drawArc(
                                    color = leah,
                                    startAngle = 0f,
                                    sweepAngle = -120f,
                                    useCenter = false,
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }
                    }

                    null -> {
                        // No progress indicator
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (failedIconVisible) {
            val clickableModifier = if (onClickSyncError != null) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(
                        bounded = false,
                        radius = 20.dp
                    ),
                    onClick = onClickSyncError
                )
            } else {
                Modifier
            }

            Image(
                modifier = Modifier
                    .size(iconSize)
                    .then(clickableModifier),
                painter = painterResource(id = R.drawable.ic_attention_24),
                contentDescription = "coin icon",
                colorFilter = ColorFilter.tint(ComposeAppTheme.colors.lucian)
            )
        } else {
            CoinImage(
                token = token,
                modifier = Modifier
                    .size(iconSize)
                    .alpha(iconAlpha)
            )
        }

        if (syncingProgress.type == SyncingProgressType.ProgressWithRing) {
            syncingProgress.progress?.let {
                SyncProgressText("${it}%")
            }
        }
    }
}

@Composable
private fun SyncProgressText(text: String) {
    BasicText(
        text = text,
        style = ComposeAppTheme.typography.subhead2.copy(
            color = ComposeAppTheme.colors.leah
        ),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(
            minFontSize = 8.sp,
            maxFontSize = 14.sp,
            stepSize = 1.sp
        )
    )
}

@Preview
@Composable
private fun SyncProgressTextPreview() {
    ComposeAppTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 10, 50, 99, 100, 100000).forEach { progress ->
                val syncingProgress = SyncingProgress(SyncingProgressType.ProgressWithRing, progress)
                val progressF = progress.coerceAtLeast(10) / 100f
                val angle = 360f * progressF
                val leah = ComposeAppTheme.colors.leah
                val circleColor = ComposeAppTheme.colors.steel10

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .drawBehind {
                            inset(-1.dp.toPx()) {
                                drawArc(
                                    color = circleColor,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                )
                                rotate(degrees = -90f) {
                                    drawArc(
                                        color = leah,
                                        startAngle = 0f,
                                        sweepAngle = angle,
                                        useCenter = false,
                                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    SyncProgressText("${progress}%")
                }
            }
        }
    }
}
