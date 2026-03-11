package cash.p.terminal.modules.balance.ui

import android.view.View
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material3.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.modules.balance.BalanceViewItem2
import cash.p.terminal.modules.balance.BalanceViewModel
import cash.p.terminal.modules.balance.SyncingProgress
import cash.p.terminal.modules.balance.SyncingProgressType
import cash.p.terminal.modules.displayoptions.DisplayDiffOptionType
import cash.p.terminal.modules.syncerror.SyncErrorDialog
import cash.p.terminal.ui_compose.components.DraggableCardSimple
import cash.p.terminal.navigation.slideFromBottom
import cash.p.terminal.ui.compose.components.CoinIconWithSyncProgress
import cash.p.terminal.ui_compose.components.CellMultilineClear
import cash.p.terminal.ui_compose.components.HsIconButton
import cash.p.terminal.ui_compose.components.HudHelper
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.captionSB_leah
import cash.p.terminal.ui_compose.components.diffColor
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.oneLineHeight
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.WalletFactory
import cash.p.terminal.wallet.balance.DeemedValue
import java.math.BigDecimal

@Composable
fun BalanceCardSwipable(
    viewItem: BalanceViewItem2,
    revealed: Boolean,
    onReveal: (Int) -> Unit,
    onConceal: () -> Unit,
    onClick: () -> Unit,
    onBalanceClick: () -> Unit,
    onClickSyncError: () -> Unit,
    onDisable: () -> Unit,
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
            onClick = onDisable,
            content = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_circle_minus_24),
                    tint = Color.Gray,
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
            enabled = viewItem.isSwipeToDeleteEnabled,
            content = {
                BalanceCard(
                    onClick = onClick,
                    onClickSyncError = onClickSyncError,
                    viewItem = viewItem,
                    onBalanceClick = onBalanceClick
                )
            }
        )
    }
}

@Composable
fun BalanceCard(
    onClick: () -> Unit,
    onClickSyncError: () -> Unit,
    onBalanceClick: (() -> Unit)? = null,
    viewItem: BalanceViewItem2
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        BalanceCardInner(
            viewItem = viewItem,
            type = BalanceCardSubtitleType.Rate,
            onClickSyncError = onClickSyncError,
            onBalanceClick = onBalanceClick
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
    onClickSyncError: (() -> Unit)? = null,
    onBalanceClick: (() -> Unit)? = null,
) {
    val verticalPadding = if (viewItem.displayDiffOptionType != DisplayDiffOptionType.NONE) {
        12.dp
    } else {
        16.dp
    }
    val mainBlockHeight = if (viewItem.displayDiffOptionType != DisplayDiffOptionType.NONE) {
        61.dp
    } else {
        40.dp
    }
    val stackingBlockHeight = if (viewItem.stackingUnpaid == null) 0.dp else 46.dp
    val cardHeight = mainBlockHeight + stackingBlockHeight + verticalPadding + verticalPadding
    CellMultilineClear(height = cardHeight, onBalanceClick = onBalanceClick) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(cardHeight - stackingBlockHeight)
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            body_leah(
                                text = viewItem.wallet.coin.code,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!viewItem.badge.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ComposeAppTheme.colors.jeremy)
                                ) {
                                    Text(
                                        modifier = Modifier.padding(
                                            horizontal = 4.dp,
                                            vertical = 2.dp
                                        ),
                                        text = viewItem.badge,
                                        color = ComposeAppTheme.colors.bran,
                                        style = ComposeAppTheme.typography.microSB,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(24.dp))
                        Text(
                            text = if (viewItem.primaryValue.visible) viewItem.primaryValue.value else "*****",
                            color = if (viewItem.primaryValue.dimmed) ComposeAppTheme.colors.grey else ComposeAppTheme.colors.leah,
                            style = ComposeAppTheme.typography.headline2,
                            maxLines = 1,
                            textAlign = TextAlign.End,
                            overflow = TextOverflow.MiddleEllipsis,
                            modifier = Modifier.weight(1f)
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
                                            Column {
                                                Text(
                                                    text = viewItem.exchangeValue.value,
                                                    color = if (viewItem.exchangeValue.dimmed) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.grey,
                                                    style = ComposeAppTheme.typography.subhead2,
                                                    modifier = Modifier.oneLineHeight(
                                                        ComposeAppTheme.typography.subhead2
                                                    ),
                                                    overflow = TextOverflow.Ellipsis,
                                                    maxLines = 1,
                                                )
                                                if (viewItem.displayDiffOptionType != DisplayDiffOptionType.NONE) {
                                                    Text(
                                                        text = viewItem.fullDiff,
                                                        color = diffColor(viewItem.diff),
                                                        style = ComposeAppTheme.typography.subhead2,
                                                        modifier = Modifier.oneLineHeight(
                                                            ComposeAppTheme.typography.subhead2
                                                        ),
                                                        overflow = TextOverflow.Ellipsis,
                                                        maxLines = 1,
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    BalanceCardSubtitleType.CoinName -> {
                                        subhead2_grey(text = viewItem.wallet.coin.name)
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
                                    color = if (viewItem.secondaryValue.dimmed) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.grey,
                                    style = ComposeAppTheme.typography.subhead2,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))
            }
            if (viewItem.stackingUnpaid != null) {
                Divider(
                    thickness = 1.dp,
                    color = ComposeAppTheme.colors.steel10,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    subhead2_grey(
                        text = stringResource(R.string.staking_unpaid),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    subhead2_grey(
                        text = if (viewItem.stackingUnpaid.visible) viewItem.stackingUnpaid.value else "*****",
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletIcon(
    viewItem: BalanceViewItem2,
    onClickSyncError: (() -> Unit)?
) {
    val syncingProgress = viewItem.syncingProgress

    Box(
        modifier = Modifier
            .width(64.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        CoinIconWithSyncProgress(
            token = viewItem.wallet.token,
            syncingProgress = syncingProgress,
            failedIconVisible = viewItem.failedIconVisible,
            onClickSyncError = onClickSyncError
        )
    }
}

fun onSyncErrorClicked(
    viewItem: BalanceViewItem2,
    viewModel: BalanceViewModel,
    navController: NavController,
    view: View
) {
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

@Preview(showBackground = true)
@Composable
private fun BalanceCardSwipablePreview() {
    ComposeAppTheme {
        BalanceCard(
            onClick = {},
            onClickSyncError = {},
            viewItem = BalanceViewItem2(
                wallet = WalletFactory.previewWallet(),
                primaryValue = DeemedValue("1.2345678739847587349875938475345345435345345345", false, true),
                secondaryValue = DeemedValue("0.123456 BTC", false, true),
                exchangeValue = DeemedValue("1234.56 USD", false, true),
                fullDiff = "+5.67%",
                diff = BigDecimal("5.67"),
                displayDiffOptionType = DisplayDiffOptionType.BOTH,
                syncingProgress = SyncingProgress(
                    type = SyncingProgressType.ProgressWithRing,
                    progress = 10
                ),
                syncingTextValue = null,
                syncedUntilTextValue = null,
                failedIconVisible = false,
                badge = "HOT",
                stackingUnpaid = DeemedValue("12.34 BTC", false, false),
                errorMessage = null,
                isWatchAccount = false,
                isSwipeToDeleteEnabled = false
            )
        )
    }
}

