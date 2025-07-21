package io.horizontalsystems.bankwallet.modules.balance.ui

import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem2
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewModel
import io.horizontalsystems.bankwallet.modules.syncerror.SyncErrorDialog
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.DraggableCardSimple
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.Badge
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.diffColor
import io.horizontalsystems.bankwallet.ui.compose.components.diffText
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.extensions.RotatingCircleProgressView
import io.horizontalsystems.core.helpers.HudHelper

@Composable
fun BalanceCardSwipable(
    viewItem: BalanceViewItem2,
    revealed: Boolean,
    onReveal: (Int) -> Unit,
    onConceal: () -> Unit,
    onClick: () -> Unit,
    onClickSyncError: () -> Unit,
    onDisable: () -> Unit,
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ComposeAppTheme.colors.tyler),
        contentAlignment = Alignment.Center
    ) {
        HsIconButton(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .width(88.dp),
            onClick = onDisable,
            content = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_circle_minus_24),
                    tint = ComposeAppTheme.colors.grey,
                    contentDescription = "delete",
                )
            }
        )

        DraggableCardSimple(
            key = viewItem.wallet,
            isRevealed = revealed,
            cardOffset = 72f,
            onReveal = { onReveal(viewItem.wallet.hashCode()) },
            onConceal = onConceal,
            content = {
                BalanceCard(
                    onClick = onClick,
                    onClickSyncError = onClickSyncError,
                    viewItem = viewItem
                )
            }
        )
    }
}

@Composable
fun BalanceCard(
    onClick: () -> Unit,
    onClickSyncError: () -> Unit,
    viewItem: BalanceViewItem2
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        BalanceCardInner(
            viewItem = viewItem,
            type = BalanceCardSubtitleType.Rate,
            onClickSyncError = onClickSyncError
        )
    }
}

enum class BalanceCardSubtitleType {
    Rate, CoinName
}

@Composable
fun BalanceCardInner(
    viewItem: BalanceViewItem2,
    type: BalanceCardSubtitleType,
    onClickSyncError: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .height(72.dp)
            .background(ComposeAppTheme.colors.lawrence),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WalletIcon(viewItem, onClickSyncError)
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
                Row(
                    modifier = Modifier.weight(weight = 1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    headline2_leah(
                        text = viewItem.wallet.coin.code,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!viewItem.badge.isNullOrBlank()) {
                        Badge(
                            modifier = Modifier.padding(start = 8.dp),
                            text = viewItem.badge,
                        )
                    }
                }
                Spacer(Modifier.width(24.dp))
                Text(
                    text = if (viewItem.primaryValue.visible) viewItem.primaryValue.value else "*****",
                    color = if (viewItem.primaryValue.dimmed) ComposeAppTheme.colors.grey else ComposeAppTheme.colors.leah,
                    style = ComposeAppTheme.typography.headline2,
                    maxLines = 1,
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                ) {
                    if (viewItem.syncingTextValue != null) {
                        subhead2_grey(
                            text = viewItem.syncingTextValue,
                            maxLines = 1,
                        )
                    } else {
                        when (type) {
                            BalanceCardSubtitleType.Rate -> {
                                if (viewItem.exchangeValue.visible) {
                                    Row {
                                        Text(
                                            text = viewItem.exchangeValue.value,
                                            color = if (viewItem.exchangeValue.dimmed) ComposeAppTheme.colors.andy else ComposeAppTheme.colors.grey,
                                            style = ComposeAppTheme.typography.subheadR,
                                            maxLines = 1,
                                        )
                                        Text(
                                            modifier = Modifier.padding(start = 4.dp),
                                            text = diffText(viewItem.diff),
                                            color = diffColor(viewItem.diff),
                                            style = ComposeAppTheme.typography.subheadR,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }

                            BalanceCardSubtitleType.CoinName -> {
                                subhead2_grey(
                                    text = viewItem.wallet.coin.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier.padding(start = 16.dp),
                ) {
                    if (viewItem.syncedUntilTextValue != null) {
                        subhead2_grey(
                            text = viewItem.syncedUntilTextValue,
                            maxLines = 1,
                        )
                    } else {
                        Text(
                            text = if (viewItem.secondaryValue.visible) viewItem.secondaryValue.value else "*****",
                            color = if (viewItem.secondaryValue.dimmed) ComposeAppTheme.colors.andy else ComposeAppTheme.colors.grey,
                            style = ComposeAppTheme.typography.subheadR,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Composable
private fun WalletIcon(
    viewItem: BalanceViewItem2,
    onClickSyncError: (() -> Unit)?
) {
    val iconAlpha = if (viewItem.syncingProgress.progress == null) 1f else 0.35f
    Box(
        modifier = Modifier
            .width(64.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        viewItem.syncingProgress.progress?.let { progress ->
            AndroidView(
                modifier = Modifier.size(47.dp),
                factory = { context ->
                    RotatingCircleProgressView(context)
                },
                update = { view ->
                    val color = when (viewItem.syncingProgress.dimmed) {
                        true -> R.color.andy
                        false -> R.color.leah
                    }
                    view.setProgressColored(progress, view.context.getColor(color), true)
                }
            )
        }
        if (viewItem.failedIconVisible) {
            val clickableModifier = if (onClickSyncError != null) {
                Modifier.clickable(onClick = onClickSyncError)
            } else {
                Modifier
            }

            Image(
                modifier = Modifier
                    .size(32.dp)
                    .then(clickableModifier),
                painter = painterResource(id = R.drawable.ic_balance_sync_error_32),
                contentDescription = "sync error",
            )
        } else {
            CoinImage(
                token = viewItem.wallet.token,
                modifier = Modifier
                    .size(32.dp)
                    .alpha(iconAlpha),
            )
        }
    }
}

fun onSyncErrorClicked(viewItem: BalanceViewItem2, viewModel: BalanceViewModel, navController: NavController, view: View) {
    when (val syncErrorDetails = viewModel.getSyncErrorDetails(viewItem)) {
        is BalanceViewModel.SyncError.Dialog -> {
            val wallet = syncErrorDetails.wallet
            val errorMessage = syncErrorDetails.errorMessage

            navController.slideFromBottom(
                R.id.syncErrorDialog,
                SyncErrorDialog.Input(wallet, errorMessage)
            )
        }

        is BalanceViewModel.SyncError.NetworkNotAvailable -> {
            HudHelper.showErrorMessage(view, R.string.Hud_Text_NoInternet)
        }
    }
}
