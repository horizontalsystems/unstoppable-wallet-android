package io.horizontalsystems.bankwallet.modules.balance.ui

import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.balance.BalanceContextMenuItem
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem2
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewModel
import io.horizontalsystems.bankwallet.modules.balance.contextMenuItems
import io.horizontalsystems.bankwallet.modules.syncerror.SyncErrorDialog
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.DraggableCardSimple
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.Badge
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.diffColor
import io.horizontalsystems.bankwallet.ui.compose.components.diffText
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead_grey
import io.horizontalsystems.bankwallet.ui.extensions.RotatingCircleProgressView
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.launch

@Composable
fun BalanceCardSwipable(
    viewItem: BalanceViewItem2,
    revealed: Boolean,
    onReveal: (Int) -> Unit,
    onConceal: () -> Unit,
    onClick: () -> Unit,
    onContextMenuItemClick: (BalanceContextMenuItem) -> Unit,
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
                    viewItem = viewItem,
                    onContextMenuItemClick = onContextMenuItemClick
                )
            }
        )
    }
}

@Composable
fun BalanceCard(
    onClick: () -> Unit,
    onClickSyncError: () -> Unit,
    viewItem: BalanceViewItem2,
    onContextMenuItemClick: (BalanceContextMenuItem) -> Unit,
) {

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var isContextMenuVisible by rememberSaveable {
        mutableStateOf(false)
    }
    var pressOffset by remember {
        mutableStateOf(DpOffset.Zero)
    }
    var itemHeight by remember {
        mutableStateOf(0.dp)
    }
    var itemWidth by remember {
        mutableStateOf(0.dp)
    }
    val interactionSource = remember {
        MutableInteractionSource()
    }
    var pressInteraction by remember {
        mutableStateOf<PressInteraction.Press?>(null)
    }
    LaunchedEffect(isContextMenuVisible) {
        if (!isContextMenuVisible && pressInteraction != null) {
            pressInteraction?.let {
                interactionSource.emit(PressInteraction.Release(it))
                pressInteraction = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged {
                itemHeight = with(density) { it.height.toDp() }
                itemWidth = with(density) { it.width.toDp() }
            }
            .indication(interactionSource, LocalIndication.current)
            .pointerInput(true) {
                detectTapGestures(
                    onLongPress = {
                        isContextMenuVisible = true
                        pressOffset = DpOffset(it.x.toDp(), it.y.toDp())
                        val press = PressInteraction.Press(it)
                        pressInteraction = press
                        coroutineScope.launch {
                            interactionSource.emit(press)
                        }
                    },
                    onTap = {
                        if (!isContextMenuVisible) {
                            onClick()
                        }
                    }
                )
            }
    ) {
        BalanceCardInner(
            viewItem = viewItem,
            type = BalanceCardSubtitleType.Rate,
            onClickSyncError = onClickSyncError
        )
    }
    val menuMinWidth = 200.dp
    val horizontalMargin = 16.dp
    val xOffset = if (pressOffset.x + menuMinWidth > itemWidth - horizontalMargin) {
        itemWidth - menuMinWidth - horizontalMargin
    } else {
        pressOffset.x
    }
    DropdownMenu(
        expanded = isContextMenuVisible,
        onDismissRequest = {
            isContextMenuVisible = false
        },
        offset = pressOffset.copy(
            y = pressOffset.y - itemHeight,
            x = xOffset
        ),
        shape = RoundedCornerShape(16.dp),
        containerColor = ComposeAppTheme.colors.lawrence,
        modifier = Modifier.defaultMinSize(minWidth = 200.dp)
    ) {
        viewItem.contextMenuItems.forEachIndexed { index, dropDownMenu ->
            if (index > 0) {
                HsDivider()
            }
            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        painter = painterResource(dropDownMenu.item.icon),
                        contentDescription = stringResource(dropDownMenu.item.title),
                        tint = if (dropDownMenu.isEnabled) ComposeAppTheme.colors.leah else ComposeAppTheme.colors.andy,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    if(dropDownMenu.isEnabled) {
                        onContextMenuItemClick(dropDownMenu.item)
                        isContextMenuVisible = false
                    }
                },
                text = {
                    body_leah(stringResource(dropDownMenu.item.title))
                }
            )
        }
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
                    text = if (viewItem.primaryValue.visible) viewItem.primaryValue.value else "------",
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
                    if (viewItem.failedIconVisible) {
                        subhead_grey(text = stringResource(R.string.BalanceSyncError_Text))
                    } else if (viewItem.syncingTextValue != null) {
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
                            text = if (viewItem.secondaryValue.visible) viewItem.secondaryValue.value else "",
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
        IconCell(
            viewItem.failedIconVisible,
            viewItem.wallet.token,
            iconAlpha,
            onClickSyncError
        )
    }
}

@Composable
private fun IconCell(
    failedIconVisible: Boolean,
    token: Token,
    iconAlpha: Float,
    onClickSyncError: (() -> Unit)?
) {
    if (failedIconVisible) {
        if (onClickSyncError != null) {
            Modifier.clickable(onClick = onClickSyncError)
            HsIconButton(
                modifier = Modifier
                    .size(32.dp)
                    .then(
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClickSyncError
                        )
                    ),
                onClick = onClickSyncError,
            ) {
                Image(
                    modifier = Modifier
                        .size(32.dp),
                    painter = painterResource(id = R.drawable.ic_balance_sync_error_32),
                    contentDescription = "sync error",
                )
            }
        } else {
            Image(
                modifier = Modifier.size(32.dp),
                painter = painterResource(id = R.drawable.ic_balance_sync_error_32),
                contentDescription = "sync error",
            )
        }
    } else {
        CoinImage(
            token = token,
            modifier = Modifier
                .size(32.dp)
                .alpha(iconAlpha),
        )
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
