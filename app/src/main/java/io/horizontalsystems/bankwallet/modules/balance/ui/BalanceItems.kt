package io.horizontalsystems.bankwallet.modules.balance.ui

import android.view.View
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.FaqManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.AccountViewItem
import io.horizontalsystems.bankwallet.modules.balance.BalanceContextMenuItem
import io.horizontalsystems.bankwallet.modules.balance.BalanceSortType
import io.horizontalsystems.bankwallet.modules.balance.BalanceUiState
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem2
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewModel
import io.horizontalsystems.bankwallet.modules.balance.ReceiveAllowedState
import io.horizontalsystems.bankwallet.modules.balance.TotalUIState
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.BackupRequiredDialog
import io.horizontalsystems.bankwallet.modules.rateapp.RateAppModule
import io.horizontalsystems.bankwallet.modules.rateapp.RateAppViewModel
import io.horizontalsystems.bankwallet.modules.send.address.EnterAddressFragment
import io.horizontalsystems.bankwallet.modules.sendtokenselect.SendTokenSelectFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subheadSB_lucian
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.AlertCard
import io.horizontalsystems.bankwallet.uiv3.components.AlertFormat
import io.horizontalsystems.bankwallet.uiv3.components.AlertType
import io.horizontalsystems.bankwallet.uiv3.components.BalanceButtonsGroup
import io.horizontalsystems.bankwallet.uiv3.components.BoxBordered
import io.horizontalsystems.bankwallet.uiv3.components.cards.CardsElementAmountText
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellLeftImage
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfoTextIcon
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightControlsButtonText
import io.horizontalsystems.bankwallet.uiv3.components.cell.ImageType
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSDropdownButton
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSIconButton
import io.horizontalsystems.bankwallet.uiv3.components.menu.MenuGroup
import io.horizontalsystems.bankwallet.uiv3.components.menu.MenuItemX
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabsSectionButtons
import io.horizontalsystems.core.helpers.HudHelper

@Composable
fun NoteWarning(
    modifier: Modifier = Modifier,
    text: String,
    onClick: (() -> Unit),
    onClose: (() -> Unit)?
) {
    Note(
        modifier = modifier.clickable(onClick = onClick),
        text = text,
        title = stringResource(id = R.string.AccountRecovery_Note),
        icon = R.drawable.ic_attention_20,
        borderColor = ComposeAppTheme.colors.jacob,
        backgroundColor = ComposeAppTheme.colors.yellow20,
        textColor = ComposeAppTheme.colors.jacob,
        iconColor = ComposeAppTheme.colors.jacob,
        onClose = onClose
    )
}

@Composable
fun NoteError(
    modifier: Modifier = Modifier,
    text: String,
    onClick: (() -> Unit)
) {
    Note(
        modifier = modifier.clickable(onClick = onClick),
        text = text,
        title = stringResource(id = R.string.AccountRecovery_Note),
        icon = R.drawable.ic_attention_20,
        borderColor = ComposeAppTheme.colors.lucian,
        backgroundColor = ComposeAppTheme.colors.red20,
        textColor = ComposeAppTheme.colors.lucian,
        iconColor = ComposeAppTheme.colors.lucian
    )
}

@Composable
fun Note(
    modifier: Modifier = Modifier,
    text: String,
    title: String,
    @DrawableRes icon: Int,
    iconColor: Color,
    borderColor: Color,
    backgroundColor: Color,
    textColor: Color,
    onClose: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(0.5.dp, borderColor, RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = iconColor
            )
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                color = textColor,
                style = ComposeAppTheme.typography.subhead
            )
            onClose?.let {
                HsIconButton(
                    modifier = Modifier.size(20.dp),
                    onClick = onClose
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        tint = iconColor,
                        contentDescription = null,
                    )
                }
            }
        }
        if (text.isNotEmpty()) {
            subhead2_leah(text = text)
        }
    }
}

@Composable
fun BalanceItems(
    balanceViewItems: List<BalanceViewItem2>,
    viewModel: BalanceViewModel,
    accountViewItem: AccountViewItem,
    navController: NavController,
    uiState: BalanceUiState,
    totalState: TotalUIState,
    onScanClick: () -> Unit,
) {
    val rateAppViewModel = viewModel<RateAppViewModel>(factory = RateAppModule.Factory())
    DisposableEffect(true) {
        rateAppViewModel.onBalancePageActive()
        onDispose {
            rateAppViewModel.onBalancePageInactive()
        }
    }

    val context = LocalContext.current
    val view = LocalView.current
    var revealedCardId by remember { mutableStateOf<Int?>(null) }

    val navigateToTokenBalance: (BalanceViewItem2) -> Unit = remember {
        {
            navController.slideFromRight(
                R.id.tokenBalanceFragment,
                it.wallet
            )

            stat(page = StatPage.Balance, event = StatEvent.OpenTokenPage(it.wallet.token))
        }
    }

    val onClickSyncError: (BalanceViewItem2) -> Unit = remember {
        {
            onSyncErrorClicked(
                it,
                viewModel,
                navController,
                view
            )
        }
    }

    val onDisable: (BalanceViewItem2) -> Unit = remember {
        {
            viewModel.disable(it)
        }
    }

    HSSwipeRefresh(
        refreshing = uiState.isRefreshing,
        onRefresh = viewModel::onRefresh
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeAppTheme.colors.lawrence),
            state = rememberSaveable(
                accountViewItem.id,
                uiState.sortType,
                saver = LazyListState.Saver
            ) {
                LazyListState()
            },
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                TotalBalanceRow(
                    totalState = totalState,
                    onClickTitle = remember {
                        {
                            viewModel.toggleBalanceVisibility()
                            HudHelper.vibrate(context)

                            stat(page = StatPage.Balance, event = StatEvent.ToggleBalanceHidden)
                        }
                    },
                    onClickSubtitle = remember {
                        {
                            viewModel.toggleTotalType()
                            HudHelper.vibrate(context)

                            stat(page = StatPage.Balance, event = StatEvent.ToggleConversionCoin)
                        }
                    },
                    loading = uiState.loading
                )
            }

            if (uiState.balanceTabButtonsEnabled && !accountViewItem.isWatchAccount) {
                item {
                    BalanceButtonsGroup {
                        if (accountViewItem.type.supportsWalletConnect) {
                            BalanceActionButton(
                                variant = ButtonVariant.Primary,
                                icon = R.drawable.ic_scan_24,
                                title = stringResource(R.string.Button_ScanQr),
                                onClick = onScanClick
                            )
                        }
                        BalanceActionButton(
                            variant = ButtonVariant.Secondary,
                            icon = R.drawable.ic_arrow_down_24,
                            title = stringResource(R.string.Balance_Receive),
                            enabled = true,
                            onClick = {
                                when (val receiveAllowedState =
                                    viewModel.getReceiveAllowedState()) {
                                    ReceiveAllowedState.Allowed -> {
                                        navController.slideFromRight(R.id.receiveChooseCoinFragment)

                                        stat(
                                            page = StatPage.Balance,
                                            event = StatEvent.Open(StatPage.ReceiveTokenList)
                                        )
                                    }

                                    is ReceiveAllowedState.BackupRequired -> {
                                        showBackupRequiredDialog(
                                            account = receiveAllowedState.account,
                                            navController = navController
                                        )
                                    }

                                    null -> Unit
                                }
                            }
                        )
                        BalanceActionButton(
                            variant = ButtonVariant.Secondary,
                            icon = R.drawable.ic_arrow_up_24,
                            title = stringResource(R.string.Balance_Send),
                            onClick = {
                                navController.slideFromRight(R.id.sendTokenSelectFragment)

                                stat(
                                    page = StatPage.Balance,
                                    event = StatEvent.Open(StatPage.SendTokenList)
                                )
                            }
                        )
                        if (viewModel.isSwapEnabled) {
                            BalanceActionButton(
                                variant = ButtonVariant.Secondary,
                                icon = R.drawable.ic_swap_circle_24,
                                title = stringResource(R.string.Swap),
                                onClick = {
                                    navController.slideFromRight(R.id.multiswap)

                                    stat(
                                        page = StatPage.Balance,
                                        event = StatEvent.Open(StatPage.Swap)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            item {
                if (uiState.nonStandardAccount) {
                    AlertCard(
                        modifier = Modifier
                            .background(ComposeAppTheme.colors.tyler)
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                            .fillMaxWidth(),
                        format = AlertFormat.Structured,
                        type = AlertType.Critical,
                        text = stringResource(R.string.AccountRecovery_MigrationRequired),
                        onClick = {
                            FaqManager.showFaqPage(
                                navController,
                                FaqManager.faqPathMigrationRequired
                            )
                        }
                    )
                }
            }

            if (accountViewItem.isWatchAccount) {
                item {
                    CellPrimary(
                        left = {
                            CellLeftImage(
                                painter = painterResource(R.drawable.binocular_24),
                                contentDescription = "binoculars icon",
                                type = ImageType.Ellipse,
                                size = 24
                            )
                        },
                        middle = {
                            CellMiddleInfoTextIcon(text = stringResource(R.string.WatchWallet).hs)
                        },
                        right = {
                            accountViewItem.watchAddress?.let { watchAddress ->
                                CellRightControlsButtonText(
                                    text = watchAddress.shorten().hs(color = ComposeAppTheme.colors.leah),
                                    icon = painterResource(id = R.drawable.copy_filled_24),
                                    iconTint = ComposeAppTheme.colors.leah,
                                )
                            }
                        },
                        backgroundColor = ComposeAppTheme.colors.tyler,
                        onClick = {
                            accountViewItem.watchAddress?.let { watchAddress ->
                                TextHelper.copyText(watchAddress)
                                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
//                                        stat(
//                                            page = StatPage.Balance,
//                                            event = StatEvent.CopyAddress(viewItem.wallet.token.blockchain.uid)
//                                        )
                            }
                        }
                    )
                }
            }

            stickyHeader {
                TabsSectionButtons(
                    left = {
                        BalanceSortingSelector(
                            sortType = uiState.sortType,
                            sortTypes = uiState.sortTypes
                        ) {
                            viewModel.setSortType(it)
                        }
                        HSIconButton(
                            variant = ButtonVariant.Secondary,
                            size = ButtonSize.Small,
                            icon = painterResource(R.drawable.ic_manage_20),
                            contentDescription = stringResource(R.string.ManageCoins_title),
                            onClick = {
                                navController.slideFromRight(R.id.manageWalletsFragment)

                                stat(
                                    page = StatPage.Balance,
                                    event = StatEvent.Open(StatPage.CoinManager)
                                )
                            }
                        )
                    },
                    right = {
                        if (!uiState.networkAvailable) {
                            subheadSB_lucian(stringResource(R.string.Hud_Text_NoInternet))
                        }
                    }
                )
            }

            if (balanceViewItems.isEmpty()) {
                item {
                    NoCoinsBlock()
                }
            } else {
                itemsIndexed(
                    items = balanceViewItems,
                    key = { _, item ->
                        item.wallet.hashCode()
                    }
                ) { index, item ->
                    BoxBordered(top = true, bottom = index == balanceViewItems.size - 1) {
                        BalanceCardSwipable(
                            viewItem = item,
                            revealed = revealedCardId == item.wallet.hashCode(),
                            onReveal = { walletHashCode ->
                                if (revealedCardId != walletHashCode) {
                                    revealedCardId = walletHashCode
                                }
                            },
                            onConceal = {
                                revealedCardId = null
                            },
                            onClick = {
                                navigateToTokenBalance.invoke(item)
                            },
                            onClickSyncError = {
                                onClickSyncError.invoke(item)
                            },
                            onContextMenuItemClick = {
                                handleContextMenuClick(
                                    menuItem = it,
                                    balanceViewItem = item,
                                    navController = navController,
                                    onAddressCopyClick = { wallet ->
                                        handleReceiveAddress(viewModel, wallet, view, navController)
                                    },
                                    onDisable = onDisable
                                )
                            },
                            onDisable = {
                                onDisable.invoke(item)
                            }
                        )
                    }

                }
            }
        }
    }
    uiState.openSend?.let { openSend ->
        navController.slideFromRight(
            R.id.sendTokenSelectFragment,
            SendTokenSelectFragment.Input(
                openSend.blockchainTypes,
                openSend.tokenTypes,
                openSend.address,
                openSend.amount,
                openSend.memo,
            )
        )
        viewModel.onSendOpened()
    }
}

private fun handleContextMenuClick(
    menuItem: BalanceContextMenuItem,
    balanceViewItem: BalanceViewItem2,
    navController: NavController,
    onAddressCopyClick: (Wallet) -> Unit,
    onDisable: (BalanceViewItem2) -> Unit
) {
    when (menuItem) {
        BalanceContextMenuItem.Send -> {
            val sendTitle = Translator.getString(
                R.string.Send_Title,
                balanceViewItem.wallet.token.fullCoin.coin.code
            )
            navController.slideFromRight(
                R.id.enterAddressFragment,
                EnterAddressFragment.Input(
                    wallet = balanceViewItem.wallet,
                    title = sendTitle
                )
            )

            stat(
                page = StatPage.Balance,
                event = StatEvent.OpenSend(balanceViewItem.wallet.token)
            )
        }

        BalanceContextMenuItem.CopyAddress -> { onAddressCopyClick.invoke(balanceViewItem.wallet) }

        BalanceContextMenuItem.Swap -> {
            navController.slideFromRight(
                R.id.multiswap,
                balanceViewItem.wallet.token
            )

            stat(
                page = StatPage.Balance,
                event = StatEvent.Open(StatPage.Swap)
            )
        }

        BalanceContextMenuItem.CoinInfo -> {
            val coinUid = balanceViewItem.wallet.coin.uid
            val arguments = CoinFragment.Input(coinUid)

            navController.slideFromRight(R.id.coinFragment, arguments)

            stat(
                page = StatPage.Balance,
                event = StatEvent.OpenCoin(coinUid)
            )
        }

        BalanceContextMenuItem.HideToken -> {
            onDisable(balanceViewItem)
        }
    }
}

private fun handleReceiveAddress(viewModel: BalanceViewModel, wallet: Wallet, view: View, navController: NavController) {
    val address = viewModel.getReceiveAddress(wallet)
    val receiveAllowedState = viewModel.getReceiveAllowedState()

    when {
        address == null -> showErrorAddressUnavailable(view)
        receiveAllowedState is ReceiveAllowedState.BackupRequired -> showBackupRequiredDialog(wallet.account, navController)
        else -> copyAddressAndShowSuccess(view, address, wallet)
    }
}

private fun showErrorAddressUnavailable(view: View) {
    HudHelper.showErrorMessage(view, R.string.Error)
}

private fun showBackupRequiredDialog(
    account: Account,
    navController: NavController
) {
    val text = Translator.getString(
        R.string.Balance_Receive_BackupRequired_Description,
        account.name
    )
    navController.slideFromBottom(
        R.id.backupRequiredDialog,
        BackupRequiredDialog.Input(account, text)
    )
    stat(
        page = StatPage.Balance,
        event = StatEvent.Open(StatPage.BackupRequired)
    )
}

private fun copyAddressAndShowSuccess(
    view: View,
    address: String,
    wallet: Wallet
) {
    TextHelper.copyText(address)
    HudHelper.showSuccessMessage(view, R.string.Hud_Text_AddressCopied)
    stat(
        page = StatPage.Balance,
        event = StatEvent.CopyAddress(wallet.token.coin.name)
    )
}

@Composable
fun BalanceActionButton(
    variant: ButtonVariant = ButtonVariant.Primary,
    @DrawableRes icon: Int,
    title: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HSIconButton(
            variant = variant,
            icon = painterResource(icon),
            enabled = enabled,
            onClick = onClick
        )
        VSpacer(8.dp)
        caption_grey(
            text = title,
            maxLines = 1
        )
    }
}

@Composable
private fun NoCoinsBlock() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VSpacer(height = 100.dp)
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    color = ComposeAppTheme.colors.raina,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(48.dp),
                painter = painterResource(R.drawable.ic_empty_wallet),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        }
        VSpacer(32.dp)
        subhead2_grey(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.Balance_NoCoinsAlert),
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
        )
        VSpacer(height = 32.dp)
    }
}

@Composable
fun BalanceSortingSelector(
    sortType: BalanceSortType,
    sortTypes: List<BalanceSortType>,
    onSelectSortType: (BalanceSortType) -> Unit
) {
    var showSortTypeSelectorDialog by remember { mutableStateOf(false) }

    HSDropdownButton(
        variant = ButtonVariant.Secondary,
        title = stringResource(sortType.getTitleRes()),
        onClick = {
            showSortTypeSelectorDialog = true
        }
    )

    if (showSortTypeSelectorDialog) {
        MenuGroup(
            title = stringResource(R.string.Balance_Sort_PopupTitle),
            items = sortTypes.map {
                MenuItemX(stringResource(it.getTitleRes()), it == sortType, it)
            },
            onDismissRequest = {
                showSortTypeSelectorDialog = false
            },
            onSelectItem = onSelectSortType
        )
    }
}

@Composable
fun TotalBalanceRow(
    totalState: TotalUIState,
    onClickTitle: () -> Unit,
    onClickSubtitle: () -> Unit,
    loading: Boolean
) {
    when (totalState) {
        TotalUIState.Hidden -> {
            CardsElementAmountText(
                title = "* * *".hs,
                body = "".hs,
                onClickTitle = onClickTitle,
                onClickSubtitle = onClickSubtitle
            )
        }

        is TotalUIState.Visible -> {
            val color = if (loading) {
                ComposeAppTheme.colors.andy
            } else if (totalState.dimmed) {
                ComposeAppTheme.colors.grey
            } else {
                null
            }

            CardsElementAmountText(
                title = totalState.primaryAmountStr.hs(color = color),
                body = totalState.secondaryAmountStr.hs(color = color),
                onClickTitle = onClickTitle,
                onClickSubtitle = onClickSubtitle,
            )
        }
    }
}
