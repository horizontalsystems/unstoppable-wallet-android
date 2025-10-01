package io.horizontalsystems.bankwallet.modules.balance.ui

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ripple
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
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
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellLeftLoaderCoinSyncFailed
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSCellButton
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
            .height(IntrinsicSize.Max)
            .background(ComposeAppTheme.colors.tyler),
        contentAlignment = Alignment.Center
    ) {
        HSCellButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            icon = painterResource(R.drawable.trash_24),
            onClick = onDisable
        )

        DraggableCardSimple(
            key = viewItem.wallet,
            isRevealed = revealed,
            cardOffset = 100f,
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
    val hapticFeedback = LocalHapticFeedback.current

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
            .indication(
                interactionSource,
                indication = ripple(
                    color = ComposeAppTheme.colors.andy
                )
            )
            .pointerInput(true) {
                detectTapGestures(
                    onLongPress = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
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
        Box(modifier = Modifier.background(ComposeAppTheme.colors.lawrence)) {
            BalanceCardInner2(
                viewItem = viewItem,
                type = BalanceCardSubtitleType.Rate,
                onClickSyncError = onClickSyncError,
                onClick = null
            )
        }
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
fun IconCell(
    failedIconVisible: Boolean,
    token: Token,
    iconAlpha: Float,
    onClickSyncError: (() -> Unit)?
) {
    if (failedIconVisible) {
        CellLeftLoaderCoinSyncFailed(onClickSyncError)
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
