package io.horizontalsystems.bankwallet.modules.balance.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.FaqManager
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.balance.*
import io.horizontalsystems.bankwallet.modules.rateapp.RateAppModule
import io.horizontalsystems.bankwallet.modules.rateapp.RateAppViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.*
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
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
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
                style = ComposeAppTheme.typography.subhead1
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
    balanceViewItems: List<BalanceViewItem>,
    viewModel: BalanceViewModel,
    accountViewItem: AccountViewItem,
    navController: NavController,
    uiState: BalanceUiState,
    totalState: TotalUIState
) {
    val rateAppViewModel = viewModel<RateAppViewModel>(factory = RateAppModule.Factory())
    DisposableEffect(true) {
        rateAppViewModel.onBalancePageActive()
        onDispose {
            rateAppViewModel.onBalancePageInactive()
        }
    }

    Column {
        val context = LocalContext.current

        when (totalState) {
            TotalUIState.Hidden -> {
                DoubleText(
                    title = "*****",
                    body = "*****",
                    dimmed = false,
                    onClickTitle = {
                        viewModel.toggleBalanceVisibility()
                        HudHelper.vibrate(context)
                    },
                    onClickSubtitle = {
                        viewModel.toggleTotalType()
                        HudHelper.vibrate(context)
                    }
                )
            }
            is TotalUIState.Visible -> {
                DoubleText(
                    title = totalState.primaryAmountStr,
                    body = totalState.secondaryAmountStr,
                    dimmed = totalState.dimmed,
                    onClickTitle = {
                        viewModel.toggleBalanceVisibility()
                        HudHelper.vibrate(context)
                    },
                    onClickSubtitle = {
                        viewModel.toggleTotalType()
                        HudHelper.vibrate(context)
                    },
                )
            }
        }

        HeaderSorting(borderTop = true) {
            var showSortTypeSelectorDialog by remember { mutableStateOf(false) }

            ButtonSecondaryTransparent(
                title = stringResource(viewModel.sortType.getTitleRes()),
                iconRight = R.drawable.ic_down_arrow_20,
                onClick = {
                    showSortTypeSelectorDialog = true
                }
            )

            if (showSortTypeSelectorDialog) {
                SelectorDialogCompose(
                    title = stringResource(R.string.Balance_Sort_PopupTitle),
                    items = viewModel.sortTypes.map {
                        TabItem(stringResource(it.getTitleRes()), it == viewModel.sortType, it)
                    },
                    onDismissRequest = {
                        showSortTypeSelectorDialog = false
                    },
                    onSelectItem = {
                        viewModel.sortType = it
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (accountViewItem.isWatchAccount) {
                Image(
                    painter = painterResource(R.drawable.icon_binocule_24),
                    contentDescription = "binoculars icon"
                )
            } else {
                ButtonSecondaryCircle(
                    icon = R.drawable.ic_manage_2,
                    contentDescription = stringResource(R.string.ManageCoins_title),
                    onClick = {
                        navController.slideFromRight(R.id.manageWalletsFragment)
                    }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))
        }

        when (uiState.headerNote) {
            HeaderNote.None -> Unit
            HeaderNote.NonStandardAccount -> {
                NoteError(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                    text = stringResource(R.string.AccountRecovery_MigrationRequired),
                    onClick = {
                        FaqManager.showFaqPage(
                            navController,
                            FaqManager.faqPathMigrationRequired
                        )
                    }
                )
            }
            HeaderNote.NonRecommendedAccount -> {
                NoteWarning(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                    text = stringResource(R.string.AccountRecovery_MigrationRecommended),
                    onClick = {
                        FaqManager.showFaqPage(
                            navController,
                            FaqManager.faqPathMigrationRecommended
                        )
                    },
                    onClose = {
                        viewModel.onCloseHeaderNote(HeaderNote.NonRecommendedAccount)
                    }
                )
            }
        }

        Wallets(balanceViewItems, viewModel, navController, accountViewItem.id, viewModel.sortType, uiState)
    }
}

@Composable
fun Wallets(
    balanceViewItems: List<BalanceViewItem>,
    viewModel: BalanceViewModel,
    navController: NavController,
    accountId: String,
    sortType: BalanceSortType,
    uiState: BalanceUiState
) {
    var revealedCardId by remember { mutableStateOf<Int?>(null) }

    val listState = rememberSaveable(
        accountId,
        sortType,
        saver = LazyListState.Saver
    ) {
        LazyListState()
    }

    HSSwipeRefresh(
        refreshing = uiState.isRefreshing,
        onRefresh = {
            viewModel.onRefresh()
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = 8.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(balanceViewItems, key = { item -> item.wallet.hashCode() }) { item ->
                if (item.isWatchAccount) {
                    BalanceCard(item, viewModel, navController)
                } else {
                    BalanceCardSwipable(
                        viewItem = item,
                        viewModel = viewModel,
                        navController = navController,
                        revealed = revealedCardId == item.wallet.hashCode(),
                        onReveal = { walletHashCode ->
                            if (revealedCardId != walletHashCode) {
                                revealedCardId = walletHashCode
                            }
                        },
                        onConceal = {
                            revealedCardId = null
                        },
                    )
                }
            }
        }
    }
}


