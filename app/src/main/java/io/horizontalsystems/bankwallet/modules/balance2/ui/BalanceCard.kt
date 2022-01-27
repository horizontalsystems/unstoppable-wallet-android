package io.horizontalsystems.bankwallet.modules.balance2.ui

import android.content.Intent
import android.view.View
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem
import io.horizontalsystems.bankwallet.modules.balance2.BackupRequiredError
import io.horizontalsystems.bankwallet.modules.balance2.BalanceViewModel
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.BackupRequiredDialog
import io.horizontalsystems.bankwallet.modules.receive.ReceiveFragment
import io.horizontalsystems.bankwallet.modules.send.SendActivity
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.syncerror.SyncErrorDialog
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.RotatingCircleProgressView
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.CoinType

@Composable
fun BalanceCard(viewItem: BalanceViewItem, viewModel: BalanceViewModel, navController: NavController) {
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
                WalletIcon(viewItem, viewModel, navController)
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

                    Spacer(modifier = Modifier.height(1.dp))

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

                Spacer(modifier = Modifier.width(16.dp))
            }
        }

        ExpandableContent(viewItem, navController, viewModel)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ExpandableContent(viewItem: BalanceViewItem, navController: NavController, viewModel: BalanceViewModel) {

    val enterExpand = remember {
        expandVertically(animationSpec = tween(250))
    }

    val exitCollapse = remember {
        shrinkVertically(animationSpec = tween(250))
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
            ButtonsRow(viewItem, navController, viewModel)
        }
    }
}

@Composable
private fun ButtonsRow(viewItem: BalanceViewItem, navController: NavController, viewModel: BalanceViewModel) {
    val onClickReceive = {
        try {
            navController.slideFromBottom(
                R.id.mainFragment_to_receiveFragment,
                bundleOf(ReceiveFragment.WALLET_KEY to viewModel.getWalletForReceive(viewItem))
            )
        } catch (e: BackupRequiredError) {
            navController.slideFromBottom(
                R.id.backupRequiredDialog,
                BackupRequiredDialog.prepareParams(e.account)
            )
        }
    }

    Row(
        modifier = Modifier
            .padding(start = 12.dp, end = 12.dp, bottom = 2.dp)
            .height(70.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current

        ButtonPrimaryYellow(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.Balance_Send),
            onClick = {
                when (viewItem.wallet.coinType) {
                    CoinType.Ethereum, is CoinType.Erc20,
                    CoinType.BinanceSmartChain, is CoinType.Bep20 -> {
                        navController.slideFromBottom(
                            R.id.mainFragment_to_sendEvmFragment,
                            bundleOf(SendEvmModule.walletKey to viewItem.wallet)
                        )
                    }
                    else -> {
                        val intent = Intent(context, SendActivity::class.java)
                        intent.putExtra(SendActivity.WALLET, viewItem.wallet)

                        context.startActivity(intent)
                    }
                }
            },
            enabled = viewItem.sendEnabled
        )
        Spacer(modifier = Modifier.width(8.dp))
        if (viewItem.swapVisible) {
            ButtonPrimaryCircle(
                icon = R.drawable.ic_arrow_down_left_24,
                onClick = onClickReceive,
                enabled = viewItem.receiveEnabled
            )
            Spacer(modifier = Modifier.width(8.dp))
            ButtonPrimaryCircle(
                icon = R.drawable.ic_swap_24,
                onClick = {
                    navController.slideFromBottom(
                        R.id.mainFragment_to_swapFragment,
                        SwapMainModule.prepareParams(viewItem.wallet.platformCoin)
                    )
                },
                enabled = viewItem.swapEnabled
            )
        } else {
            ButtonPrimaryDefault(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.Balance_Receive),
                onClick = onClickReceive,
                enabled = viewItem.receiveEnabled
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        ButtonPrimaryCircle(
            icon = R.drawable.ic_chart_24,
            onClick = {
                val coinUid = viewItem.wallet.coin.uid
                val arguments = CoinFragment.prepareParams(coinUid)

                navController.slideFromRight(R.id.mainFragment_to_coinFragment, arguments)
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
            modifier = Modifier
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
private fun WalletIcon(viewItem: BalanceViewItem, viewModel: BalanceViewModel, navController: NavController) {
    Box(
        modifier = Modifier
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
            val view = LocalView.current
            Image(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
                    .clickable {
                        onSyncErrorClicked(viewItem, viewModel, navController, view)
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
