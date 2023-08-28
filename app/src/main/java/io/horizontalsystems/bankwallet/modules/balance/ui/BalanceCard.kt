package io.horizontalsystems.bankwallet.modules.balance.ui

import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.isCustom
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.balance.BackupRequiredError
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewModel
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.BackupRequiredDialog
import io.horizontalsystems.bankwallet.modules.receive.ReceiveFragment
import io.horizontalsystems.bankwallet.modules.send.SendFragment
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.syncerror.SyncErrorDialog
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.DraggableCardSimple
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellMultilineClear
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.RateColor
import io.horizontalsystems.bankwallet.ui.compose.components.RateText
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.extensions.RotatingCircleProgressView
import io.horizontalsystems.core.helpers.HudHelper

@Composable
fun BalanceCardSwipable(
    viewItem: BalanceViewItem,
    viewModel: BalanceViewModel,
    navController: NavController,
    revealed: Boolean,
    onReveal: (Int) -> Unit,
    onConceal: () -> Unit,
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
            onClick = { viewModel.disable(viewItem) },
            content = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_circle_minus_24),
                    tint = Color.Gray,
                    contentDescription = "delete",
                )
            }
        )

        DraggableCardSimple(
            isRevealed = revealed,
            cardOffset = 72f,
            onReveal = { onReveal(viewItem.wallet.hashCode()) },
            onConceal = onConceal,
            content = {
                BalanceCard(viewItem, viewModel, navController)
            }
        )
    }
}

@Composable
fun BalanceCard(
    viewItem: BalanceViewItem,
    viewModel: BalanceViewModel,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                viewModel.onItem(viewItem)
            }
    ) {
        val view = LocalView.current

        BalanceCardInner(viewItem) {
            onSyncErrorClicked(viewItem, viewModel, navController, view)
        }

        ExpandableContent(viewItem, navController, viewModel)
    }
}

@Composable
fun BalanceCardInner(
    viewItem: BalanceViewItem,
    onClickSyncError: (() -> Unit)? = null
) {
    CellMultilineClear(height = 64.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                        body_leah(
                            text = viewItem.coinCode,
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
                                        start = 4.dp,
                                        end = 4.dp,
                                        bottom = 1.dp
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
                    if (viewItem.primaryValue.visible) {
                        Text(
                            text = viewItem.primaryValue.value,
                            color = if (viewItem.primaryValue.dimmed) ComposeAppTheme.colors.grey else ComposeAppTheme.colors.leah,
                            style = ComposeAppTheme.typography.headline2,
                            maxLines = 1,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                    ) {
                        if (viewItem.syncingTextValue.visible) {
                            subhead2_grey(
                                text = viewItem.syncingTextValue.value ?: "",
                                maxLines = 1,
                            )
                        }
                        if (viewItem.exchangeValue.visible) {
                            Row {
                                Text(
                                    text = viewItem.exchangeValue.value,
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
                            subhead2_grey(
                                text = viewItem.syncedUntilTextValue.value ?: "",
                                maxLines = 1,
                            )
                        }
                        if (viewItem.secondaryValue.visible) {
                            Text(
                                text = viewItem.secondaryValue.value,
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
    }
}

@Composable
private fun ExpandableContent(
    viewItem: BalanceViewItem,
    navController: NavController,
    viewModel: BalanceViewModel
) {
    AnimatedVisibility(
        visible = viewItem.expanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column {
            LockedValueRow(viewItem)
            Divider(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 5.dp, bottom = 6.dp),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )
            ButtonsRow(viewItem, navController, viewModel)
        }
    }
}

@Composable
private fun ButtonsRow(viewItem: BalanceViewItem, navController: NavController, viewModel: BalanceViewModel) {
    val onClickReceive = {
        try {
            navController.slideFromBottom(
                R.id.receiveFragment,
                bundleOf(ReceiveFragment.WALLET_KEY to viewModel.getWalletForReceive(viewItem))
            )
        } catch (e: BackupRequiredError) {
            val text = Translator.getString(
                R.string.ManageAccount_BackupRequired_Description,
                e.account.name,
                e.coinTitle
            )
            navController.slideFromBottom(
                R.id.backupRequiredDialog,
                BackupRequiredDialog.prepareParams(e.account, text)
            )
        }
    }

    Row(
        modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (viewItem.isWatchAccount) {
            ButtonPrimaryDefault(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.Balance_Address),
                onClick = onClickReceive,
            )
        } else {
            ButtonPrimaryYellow(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.Balance_Send),
                onClick = {
                    navController.slideFromBottom(
                        R.id.sendXFragment,
                        SendFragment.prepareParams(viewItem.wallet)
                    )
                },
                enabled = viewItem.sendEnabled
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (viewItem.swapVisible) {
                ButtonPrimaryCircle(
                    icon = R.drawable.ic_arrow_down_left_24,
                    contentDescription = stringResource(R.string.Balance_Receive),
                    onClick = onClickReceive,
                )
                Spacer(modifier = Modifier.width(8.dp))
                ButtonPrimaryCircle(
                    icon = R.drawable.ic_swap_24,
                    contentDescription = stringResource(R.string.Swap),
                    onClick = {
                        navController.slideFromBottom(
                            R.id.swapFragment,
                            SwapMainModule.prepareParams(viewItem.wallet.token)
                        )
                    },
                    enabled = viewItem.swapEnabled
                )
            } else {
                ButtonPrimaryDefault(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.Balance_Receive),
                    onClick = onClickReceive,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        ButtonPrimaryCircle(
            icon = R.drawable.ic_chart_24,
            contentDescription = stringResource(R.string.Coin_Info),
            enabled = !viewItem.wallet.token.isCustom,
            onClick = {
                val coinUid = viewItem.wallet.coin.uid
                val arguments = CoinFragment.prepareParams(coinUid)

                navController.slideFromRight(R.id.coinFragment, arguments)
            },
        )
    }
}

@Composable
private fun LockedValueRow(viewItem: BalanceViewItem) {
    AnimatedVisibility(
        visible = viewItem.coinValueLocked.visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column {
            Divider(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 5.dp, bottom = 6.dp),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )
            Row(
                modifier = Modifier
                    .height(25.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_lock_16),
                    contentDescription = "lock icon"
                )
                Text(
                    modifier = Modifier.padding(start = 6.dp),
                    text = viewItem.coinValueLocked.value,
                    color = if (viewItem.coinValueLocked.dimmed) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.grey,
                    style = ComposeAppTheme.typography.subhead2,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = viewItem.fiatValueLocked.value,
                    color = if (viewItem.fiatValueLocked.dimmed) ComposeAppTheme.colors.yellow50 else ComposeAppTheme.colors.jacob,
                    style = ComposeAppTheme.typography.subhead2,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun WalletIcon(
    viewItem: BalanceViewItem,
    onClickSyncError: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .width(64.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        viewItem.syncingProgress.progress?.let { progress ->
            AndroidView(
                modifier = Modifier
                    .size(52.dp),
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
            val clickableModifier = if (onClickSyncError != null) {
                Modifier.clickable(onClick = onClickSyncError)
            } else {
                Modifier
            }

            Image(
                modifier = Modifier
                    .size(32.dp)
                    .then(clickableModifier),
                painter = painterResource(id = R.drawable.ic_attention_24),
                contentDescription = "coin icon",
                colorFilter = ColorFilter.tint(ComposeAppTheme.colors.lucian)
            )
        } else {
            CoinImage(
                iconUrl = viewItem.coinIconUrl,
                placeholder = viewItem.coinIconPlaceholder,
                modifier = Modifier
                    .size(32.dp)
            )
        }
    }
}

private fun onSyncErrorClicked(viewItem: BalanceViewItem, viewModel: BalanceViewModel, navController: NavController, view: View) {
    when (val syncErrorDetails = viewModel.getSyncErrorDetails(viewItem)) {
        is BalanceViewModel.SyncError.Dialog -> {
            val wallet = syncErrorDetails.wallet
            val errorMessage = syncErrorDetails.errorMessage

            navController.slideFromBottom(
                R.id.syncErrorDialog,
                SyncErrorDialog.prepareParams(wallet, errorMessage)
            )
        }
        is BalanceViewModel.SyncError.NetworkNotAvailable -> {
            HudHelper.showErrorMessage(view, R.string.Hud_Text_NoInternet)
        }
    }
}
