package io.horizontalsystems.bankwallet.modules.balance2.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.balance.BalanceFragment
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem
import io.horizontalsystems.bankwallet.modules.balance2.BalanceViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.RotatingCircleProgressView

@Composable
fun BalanceCard(viewItem: BalanceViewItem, viewModel: BalanceViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .padding(vertical = 4.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                viewModel.onItem(viewItem)
            }
    ) {
        CellMultilineClear {
            Row {
                WalletIcon(viewItem)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewItem.coinCode,
                            color = ComposeAppTheme.colors.oz,
                            style = ComposeAppTheme.typography.headline2,
                            maxLines = 1,
                        )
                        if (!viewItem.badge.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp, end = 16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ComposeAppTheme.colors.jeremy)
                            ) {
                                Text(
                                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 1.dp),
                                    text = viewItem.badge,
                                    color = ComposeAppTheme.colors.bran,
                                    style = ComposeAppTheme.typography.microSB,
                                    maxLines = 1,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (viewItem.fiatValue.visible) {
                            Text(
                                text = viewItem.fiatValue.text ?: "",
                                color = if (viewItem.fiatValue.dimmed) ComposeAppTheme.colors.yellow50 else ComposeAppTheme.colors.jacob,
                                style = ComposeAppTheme.typography.headline2,
                                maxLines = 1,
                            )
                        }
                    }

                    Spacer(modifier = androidx.compose.ui.Modifier.height(1.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                        ) {
                            if (viewItem.syncingTextValue.visible) {
                                Text(
                                    text = viewItem.syncingTextValue.text ?: "",
                                    color = ComposeAppTheme.colors.grey,
                                    style = ComposeAppTheme.typography.subhead2,
                                    maxLines = 1,
                                )
                            }
                            if (viewItem.exchangeValue.visible) {
                                Row {
                                    Text(
                                        text = viewItem.exchangeValue.text ?: "",
                                        color = if (viewItem.exchangeValue.dimmed) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.grey,
                                        style = ComposeAppTheme.typography.subhead2,
                                        maxLines = 1,
                                    )
                                    Text(
                                        modifier = Modifier.padding(start = 4.dp),
                                        text = RateText(viewItem.diff),
                                        color = RateColor(viewItem.diff),
                                        style = ComposeAppTheme.typography.subhead2,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier.padding(start = 16.dp),
                        ) {
                            if (viewItem.syncedUntilTextValue.visible) {
                                Text(
                                    text = viewItem.syncedUntilTextValue.text ?: "",
                                    color = ComposeAppTheme.colors.grey,
                                    style = ComposeAppTheme.typography.subhead2,
                                    maxLines = 1,
                                )
                            }
                            if (viewItem.coinValue.visible) {
                                Text(
                                    text = viewItem.coinValue.text ?: "",
                                    color = if (viewItem.coinValue.dimmed) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.grey,
                                    style = ComposeAppTheme.typography.subhead2,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = androidx.compose.ui.Modifier.width(16.dp))
            }
        }

        ExpandableContent(viewItem)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ExpandableContent(viewItem: BalanceViewItem) {

    val enterExpand = remember {
        expandVertically(animationSpec = tween(BalanceFragment.EXPAND_ANIMATION_DURATION))
    }

    val exitCollapse = remember {
        shrinkVertically(animationSpec = tween(BalanceFragment.COLLAPSE_ANIMATION_DURATION))
    }

    AnimatedVisibility(
        visible = viewItem.expanded,
        enter = enterExpand,
        exit = exitCollapse
    ) {
        Column {
            LockedValueRow(viewItem)
            Divider(
                modifier = Modifier.padding(horizontal = 14.dp),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )
            ButtonsRow(viewItem)
        }
    }
}

@Composable
private fun ButtonsRow(viewItem: BalanceViewItem) {
    Row(
        modifier = Modifier
            .padding(start = 12.dp, end = 12.dp, bottom = 2.dp)
            .height(70.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ButtonPrimaryYellow(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.Balance_Send),
            onClick = {
//                onSendClicked(viewItem)
            },
            enabled = viewItem.sendEnabled
        )
        Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
        if (viewItem.swapVisible) {
            ButtonPrimaryCircle(
                icon = R.drawable.ic_arrow_down_left_24,
                onClick = {
//                    onReceiveClicked(viewItem)
                },
                enabled = viewItem.receiveEnabled
            )
            Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
            ButtonPrimaryCircle(
                icon = R.drawable.ic_swap_24,
                onClick = {
//                    onSwapClicked(viewItem)
                },
                enabled = viewItem.swapEnabled
            )
        } else {
            ButtonPrimaryDefault(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.Balance_Receive),
                onClick = {
//                    onReceiveClicked(viewItem)
                },
                enabled = viewItem.receiveEnabled
            )
        }
        Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
        ButtonPrimaryCircle(
            icon = R.drawable.ic_chart_24,
            onClick = {
//                onChartClicked(viewItem)
            },
            enabled = viewItem.exchangeValue.text != null
        )
    }
}

@Composable
private fun LockedValueRow(viewItem: BalanceViewItem) {
    if (viewItem.coinValueLocked.visible) {
        Divider(
            modifier = Modifier.padding(horizontal = 14.dp),
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10
        )
        Row(
            modifier = androidx.compose.ui.Modifier
                .height(36.dp)
                .padding(start = 16.dp, end = 17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_lock_16),
                contentDescription = "lock icon"
            )
            Text(
                modifier = Modifier.padding(start = 6.dp),
                text = viewItem.coinValueLocked.text ?: "",
                color = if (viewItem.coinValueLocked.dimmed) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.grey,
                style = ComposeAppTheme.typography.subhead2,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = viewItem.fiatValueLocked.text ?: "",
                color = if (viewItem.fiatValueLocked.dimmed) ComposeAppTheme.colors.yellow50 else ComposeAppTheme.colors.jacob,
                style = ComposeAppTheme.typography.subhead2,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun WalletIcon(viewItem: BalanceViewItem) {
    Box(
        modifier = androidx.compose.ui.Modifier
            .width(56.dp)
            .fillMaxHeight(),
    ) {
        if (!viewItem.mainNet) {
            Image(
                modifier = Modifier.align(Alignment.TopCenter),
                painter = painterResource(R.drawable.testnet),
                contentDescription = "Testnet"
            )
        }
        viewItem.syncingProgress.progress?.let { progress ->
            AndroidView(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(41.dp),
                factory = { context ->
                    RotatingCircleProgressView(context)
                },
                update = { view ->
                    val color = when (viewItem.syncingProgress.dimmed) {
                        true -> R.color.grey_50
                        false -> R.color.grey
                    }
                    view.setProgressColored(progress, view.context.getColor(color))
                }
            )
        }
        if (viewItem.failedIconVisible) {
            Image(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
                    .clickable {
//                        onSyncErrorClicked(viewItem)
                    },
                painter = painterResource(id = R.drawable.ic_attention_24),
                contentDescription = "coin icon",
                colorFilter = ColorFilter.tint(ComposeAppTheme.colors.lucian)
            )
        } else {
            CoinImage(
                iconUrl = viewItem.coinIconUrl,
                placeholder = viewItem.coinIconPlaceholder,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
            )
        }
    }
}