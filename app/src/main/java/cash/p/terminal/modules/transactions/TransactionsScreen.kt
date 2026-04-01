package cash.p.terminal.modules.transactions

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.os.Build
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.premiumAction
import cash.p.terminal.modules.balance.BalanceAccountsViewModel
import cash.p.terminal.modules.balance.BalanceModule
import cash.p.terminal.modules.balance.BalanceScreenState
import cash.p.terminal.modules.balance.token.addresspoisoning.AddressPoisoningViewMode
import cash.p.terminal.modules.transactions.poison_status.PoisonStatus
import cash.p.terminal.modules.transactions.poison_status.PoisonStatusBadge
import cash.p.terminal.modules.transactions.poison_status.TransactionStatusesInfoSheet
import cash.p.terminal.navigation.slideFromBottom
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui.compose.components.ListEmptyView
import cash.p.terminal.ui_compose.ColorName
import cash.p.terminal.ui_compose.ColoredValue
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.ColumnUniversal
import cash.p.terminal.ui_compose.components.HSCircularProgressIndicator
import cash.p.terminal.ui_compose.components.HeaderStick
import cash.p.terminal.ui_compose.components.HsImage
import cash.p.terminal.ui_compose.components.HudHelper
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.ScrollableTabs
import cash.p.terminal.ui_compose.components.SectionUniversalItem
import cash.p.terminal.ui_compose.components.SnackbarDuration
import cash.p.terminal.ui_compose.components.TabItem
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.subhead1_grey
import cash.p.terminal.ui_compose.components.subhead2
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.entities.SectionItemPosition
import cash.p.terminal.ui_compose.entities.ViewState
import cash.p.terminal.ui_compose.sectionItemBorder
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import java.util.Date

@Composable
fun TransactionsScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    viewModel: TransactionsViewModel,
    onShowAllTransactionsClicked: () -> Unit
) {
    val accountsViewModel =
        viewModel<BalanceAccountsViewModel>(factory = BalanceModule.AccountsFactory())

    val filterTypes by viewModel.filterTypesLiveData.observeAsState()
    val showFilterAlertDot by viewModel.filterResetEnabled.observeAsState(false)

    val uiState = viewModel.uiState
    val syncing = uiState.syncing
    val transactions = uiState.transactions

    var showAmlInfoSheet by remember { mutableStateOf(false) }
    val view = LocalView.current

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
            AppBar(
                title = stringResource(R.string.Transactions_Title),
                showSpinner = syncing,
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Transactions_Filter),
                        icon = R.drawable.ic_manage_2_24,
                        showAlertDot = showFilterAlertDot,
                        onClick = {
                            navController.slideFromRight(R.id.transactionFilterFragment)
                        },
                    )
                )
            )
            filterTypes?.let { filterTypes ->
                FilterTypeTabs(
                    filterTypes = filterTypes,
                    onTransactionTypeClick = {
                        viewModel.setFilterTransactionType(it)
                    }
                )
            }

            Crossfade(uiState.viewState, label = "") { viewState ->
                when (viewState) {
                    ViewState.Loading -> {
                        ListEmptyView(
                            text = stringResource(R.string.Transactions_WaitForSync),
                            icon = R.drawable.ic_clock
                        )
                    }

                    ViewState.Success -> {
                        transactions?.let { transactionItems ->
                            if (transactionItems.isEmpty() && !uiState.hasHiddenTransactions) {
                                if (uiState.syncing) {
                                    ListEmptyView(
                                        text = stringResource(R.string.Transactions_WaitForSync),
                                        icon = R.drawable.ic_clock
                                    )
                                } else {
                                    ListEmptyView(
                                        text = stringResource(R.string.Transactions_EmptyList),
                                        icon = R.drawable.ic_outgoingraw
                                    )
                                }
                            } else {
                                TransactionListContent(
                                    uiState = uiState,
                                    accountViewItemId = (accountsViewModel.balanceScreenState as? BalanceScreenState.HasAccount)?.accountViewItem?.id,
                                    onClickTransaction = { onTransactionClick(it, viewModel, navController) },
                                    onClickSensitiveValue = {
                                        HudHelper.vibrate(App.instance)
                                        viewModel.toggleTransactionInfoHidden(it.uid)
                                    },
                                    onToggleBalanceVisibility = {
                                        HudHelper.vibrate(App.instance)
                                        viewModel.toggleBalanceHidden()
                                    },
                                    willShow = viewModel::willShow,
                                    onReachBottom = viewModel::onBottomReached,
                                    onClickShowAll = onShowAllTransactionsClicked
                                ) {
                                    AmlCheckPromoBanner(
                                        amlCheckEnabled = uiState.amlCheckEnabled,
                                        onToggleChange = { enabled ->
                                            if (enabled) {
                                                navController.premiumAction {
                                                    viewModel.setAmlCheckEnabled(true)
                                                }
                                            } else {
                                                viewModel.setAmlCheckEnabled(false)
                                            }
                                        },
                                        onInfoClick = { showAmlInfoSheet = true },
                                        onClose = {
                                            viewModel.dismissAmlPromo()
                                            HudHelper.showPremiumMessage(
                                                view,
                                                R.string.aml_promo_dismiss_hud,
                                                SnackbarDuration.LONG
                                            )
                                        },
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                }
                            }
                        }
                    }

                    is ViewState.Error -> {
                        // Show error state if needed
                    }
                }
            }
        }
    }

    if (showAmlInfoSheet) {
        AmlCheckInfoBottomSheet(
            onPremiumSettingsClick = {
                showAmlInfoSheet = false
                navController.slideFromRight(
                    R.id.premiumSettingsFragment
                )
            },
            onLaterClick = { showAmlInfoSheet = false },
            onDismiss = { showAmlInfoSheet = false }
        )
    }
}

private fun onTransactionClick(
    transactionViewItem: TransactionViewItem,
    viewModel: TransactionsViewModel,
    navController: NavController
) {
    val transactionItem = viewModel.getTransactionItem(transactionViewItem) ?: return

    viewModel.tmpItemToShow = transactionItem

    navController.slideFromBottom(R.id.transactionInfoFragment)
}

@Composable
private fun TransactionListContent(
    uiState: TransactionsUiState,
    onClickTransaction: (TransactionViewItem) -> Unit,
    onClickSensitiveValue: (TransactionViewItem) -> Unit,
    onToggleBalanceVisibility: () -> Unit,
    willShow: (TransactionViewItem) -> Unit,
    onReachBottom: () -> Unit,
    accountViewItemId: String? = null,
    onClickShowAll: () -> Unit = {},
    amlPromoContent: @Composable () -> Unit = {}
) {
    val transactionItems = uiState.transactions ?: return
    val showAmlPromo = uiState.showAmlPromo
    val listState = rememberSaveable(
        uiState.transactionListId, accountViewItemId, saver = LazyListState.Saver
    ) { LazyListState(0, 0) }

    val currentStickyDate by remember(listState, transactionItems, showAmlPromo) {
        derivedStateOf {
            val firstVisible = listState.firstVisibleItemIndex
            val offset = if (showAmlPromo) 1 else 0
            if (firstVisible < offset) return@derivedStateOf null
            var idx = offset
            var lastDate: String? = null
            for ((date, txs) in transactionItems) {
                lastDate = date
                if (firstVisible < idx + 1 + txs.size + 1) return@derivedStateOf date
                idx += 1 + txs.size + 1
            }
            lastDate
        }
    }

    Box {
        LazyColumn(state = listState, contentPadding = PaddingValues(top = 44.dp)) {
            if (showAmlPromo) { item { amlPromoContent() } }
            transactionList(
                transactionsMap = transactionItems, willShow = willShow,
                isItemBalanceHidden = { !it.showAmount },
                onSensitiveValueClick = onClickSensitiveValue,
                onClick = onClickTransaction, onBottomReached = onReachBottom
            )
            if (uiState.hasHiddenTransactions) {
                transactionsHiddenBlock(transactionItems.isNotEmpty(), onClickShowAll)
            }
        }
        HideBalanceOverlay(
            currentStickyDate, uiState.balanceHidden,
            onToggleBalanceVisibility, Modifier.align(Alignment.TopStart)
        )
    }
}

fun LazyListScope.transactionsHiddenBlock(
    shortBlock: Boolean,
    onShowAllTransactionsClicked: () -> Unit
) {
    item {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!shortBlock) {
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
                        painter = painterResource(R.drawable.ic_eye_off),
                        contentDescription = "transactions hidden",
                        tint = ComposeAppTheme.colors.grey
                    )
                }
                Spacer(Modifier.height(32.dp))
                subhead2_grey(
                    text = stringResource(R.string.Transactions_Hide),
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                subhead2_grey(
                    text = "*****",
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(32.dp))
            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                title = stringResource(R.string.show_all_transactions),
                onClick = onShowAllTransactionsClicked
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.transactionList(
    transactionsMap: Map<String, List<TransactionViewItem>>,
    willShow: (TransactionViewItem) -> Unit,
    isItemBalanceHidden: (TransactionViewItem) -> Boolean,
    onSensitiveValueClick: (TransactionViewItem) -> Unit,
    onClick: (TransactionViewItem) -> Unit,
    onBottomReached: () -> Unit
) {
    val bottomReachedUid = getBottomReachedUid(transactionsMap)

    transactionsMap.forEach { (dateHeader, transactions) ->
        stickyHeader {
            HeaderStick(text = dateHeader)
        }

        val itemsCount = transactions.size
        val singleElement = itemsCount == 1

        itemsIndexed(
            items = transactions,
            key = { _, item ->
                item.uid
            }
        ) { index, item ->
            val position: SectionItemPosition = when {
                singleElement -> SectionItemPosition.Single
                index == 0 -> SectionItemPosition.First
                index == itemsCount - 1 -> SectionItemPosition.Last
                else -> SectionItemPosition.Middle
            }

            val shouldShowAmount = !isItemBalanceHidden(item)

            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                TransactionCell(
                    item = item,
                    position = position,
                    showAmount = shouldShowAmount,
                    onValueClick = {
                        onSensitiveValueClick(item)
                    },
                    onClick = {
                        onClick(item)
                    }
                )
            }

            willShow.invoke(item)

            if (item.uid == bottomReachedUid) {
                onBottomReached.invoke()
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    item {
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun HideBalanceOverlay(
    dateHeader: String?,
    balanceHidden: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ComposeAppTheme.colors.tyler)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dateHeader != null) {
                subhead1_grey(
                    text = dateHeader,
                    maxLines = 1,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggle
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                subhead2_grey(
                    text = stringResource(R.string.hide_balance),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Icon(
                    painter = painterResource(
                        if (balanceHidden) R.drawable.ic_eye_off
                        else R.drawable.ic_eye_20
                    ),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel20,
        )
    }
}

private fun getBottomReachedUid(transactionsMap: Map<String, List<TransactionViewItem>>): String? {
    val txList = transactionsMap.values.flatten()
    //get index not exact bottom but near to the bottom, to make scroll smoother
    val index = if (txList.size > 4) txList.size - 4 else 0

    return txList.getOrNull(index)?.uid
}

@Composable
fun TransactionCell(
    item: TransactionViewItem,
    position: SectionItemPosition,
    showAmount: Boolean = item.showAmount,
    onValueClick: () -> Unit,
    onClick: () -> Unit
) {
    var showStatusesInfo by remember { mutableStateOf(false) }

    if (showStatusesInfo) {
        TransactionStatusesInfoSheet(onDismiss = { showStatusesInfo = false })
    }
    val divider = position == SectionItemPosition.Middle || position == SectionItemPosition.Last
    SectionUniversalItem(
        borderTop = divider,
    ) {
        val clipModifier = when (position) {
            SectionItemPosition.First -> {
                Modifier.clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            }

            SectionItemPosition.Last -> {
                Modifier.clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            }

            SectionItemPosition.Single -> {
                Modifier.clip(RoundedCornerShape(12.dp))
            }

            SectionItemPosition.Middle -> {
                Modifier.clipToBounds()
            }
        }

        val borderModifier = if (position != SectionItemPosition.Single) {
            Modifier.sectionItemBorder(1.dp, ComposeAppTheme.colors.steel20, 12.dp, position)
        } else {
            Modifier.border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(12.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(clipModifier)
                .then(borderModifier)
                .clickable(onClick = onClick)
        ) {
            // AML status stripe on left edge
            item.amlStatus?.let { status ->
                AmlStatusStripe(status)
            }

            // Loading indicator in top-left corner
            if (item.amlStatus == AmlStatus.Loading) {
                AmlLoadingIndicator()
            }

            val isScam = item.poisonStatus == PoisonStatus.SUSPICIOUS
            val blurModifier = if (isScam && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.blur(3.dp)
            } else if (isScam) {
                Modifier.alpha(0.15f)
            } else {
                Modifier
            }

            ColumnUniversal(
                modifier = Modifier.fillMaxSize()
            ) {
                Row {
                    TransactionIconBox(item)
                    TransactionContentRow(
                        item = item,
                        showAmount = showAmount,
                        blurModifier = blurModifier,
                        onValueClick = onValueClick,
                    )
                }
                if (item.addressPoisoningViewMode == AddressPoisoningViewMode.STANDARD) {
                    VSpacer(6.dp)
                    body_leah(
                        text = if (showAmount) item.subtitle else "*****",
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(blurModifier)
                            .padding(horizontal = 12.dp),
                        maxLines = 1,
                    )
                    VSpacer(11.dp)
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = ComposeAppTheme.colors.steel10,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    PoisonStatusBadge(
                        poisonStatus = item.poisonStatus,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 5.dp),
                        onInfoClick = { showStatusesInfo = true },
                    )
                } else {
                    VSpacer(11.dp)
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = ComposeAppTheme.colors.steel10,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    VSpacer(6.dp)
                    PoisonStatusBadge(
                        poisonStatus = item.poisonStatus,
                        text = if (showAmount) item.subtitle else "*****",
                        modifier = Modifier.padding(horizontal = 12.dp),
                        onInfoClick = { showStatusesInfo = true },
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionIconBox(item: TransactionViewItem) {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(42.dp)
            .alpha(if (item.spam) 0.5f else 1f),
        contentAlignment = Alignment.Center
    ) {
        item.progress?.let { progress ->
            HSCircularProgressIndicator(progress)
        }
        when (val icon = item.icon) {
            TransactionViewItem.Icon.Failed -> {
                Icon(
                    painter = painterResource(R.drawable.ic_attention_24),
                    tint = ComposeAppTheme.colors.lucian,
                    contentDescription = null
                )
            }

            is TransactionViewItem.Icon.Platform -> {
                Icon(
                    modifier = Modifier.size(32.dp),
                    painter = painterResource(icon.iconRes ?: R.drawable.coin_placeholder),
                    tint = ComposeAppTheme.colors.leah,
                    contentDescription = null
                )
            }

            is TransactionViewItem.Icon.Regular -> {
                val shape =
                    if (icon.rectangle) RoundedCornerShape(CornerSize(4.dp)) else CircleShape
                HsImage(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(shape),
                    url = icon.url,
                    alternativeUrl = icon.alternativeUrl,
                    placeholder = icon.placeholder
                )
            }

            is TransactionViewItem.Icon.Double -> {
                val backShape =
                    if (icon.back.rectangle) RoundedCornerShape(CornerSize(4.dp)) else CircleShape
                val frontShape =
                    if (icon.front.rectangle) RoundedCornerShape(CornerSize(4.dp)) else CircleShape
                HsImage(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 4.dp, start = 6.dp)
                        .size(24.dp)
                        .clip(backShape),
                    url = icon.back.url,
                    alternativeUrl = icon.back.alternativeUrl,
                    placeholder = icon.back.placeholder,
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 4.5.dp, end = 6.5.dp)
                        .size(24.dp)
                        .clip(frontShape)
                        .background(ComposeAppTheme.colors.tyler)
                )

                HsImage(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 4.dp, end = 6.dp)
                        .size(24.dp)
                        .clip(frontShape),
                    url = icon.front.url,
                    alternativeUrl = icon.front.alternativeUrl,
                    placeholder = icon.front.placeholder,
                )
            }

            is TransactionViewItem.Icon.ImageResource -> {}
        }
    }
}

@Composable
private fun TransactionContentRow(
    item: TransactionViewItem,
    showAmount: Boolean,
    blurModifier: Modifier,
    onValueClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .then(blurModifier)
            .padding(end = 16.dp)
            .alpha(if (item.spam) 0.5f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            body_leah(
                modifier = Modifier.padding(end = 24.dp),
                text = item.title,
                maxLines = 1,
            )
            Text(
                text = if (showAmount) item.formattedTime else "*****",
                style = ComposeAppTheme.typography.subhead2,
                color = ComposeAppTheme.colors.grey50,
                maxLines = 1,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onValueClick,
                )
            )
        }
        Column(
            modifier = Modifier.wrapContentWidth(),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                item.primaryValue?.let { coloredValue ->
                    Text(
                        text = if (showAmount) coloredValue.value else "*****",
                        style = ComposeAppTheme.typography.body,
                        color = coloredValue.color.compose(),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onValueClick,
                        )
                    )
                }

                if (item.doubleSpend) {
                    Image(
                        modifier = Modifier.padding(start = 6.dp),
                        painter = painterResource(R.drawable.ic_double_spend_20),
                        contentDescription = null
                    )
                }
                item.locked?.let { locked ->
                    Image(
                        modifier = Modifier.padding(start = 6.dp),
                        painter = painterResource(if (locked) R.drawable.ic_lock_20 else R.drawable.ic_unlock_20),
                        contentDescription = null
                    )
                }
                if (item.sentToSelf) {
                    Image(
                        modifier = Modifier.padding(start = 6.dp),
                        painter = painterResource(R.drawable.ic_arrow_return_20),
                        contentDescription = null
                    )
                }
            }
            if (item.primaryValue != null || item.secondaryValue != null) {
                Text(
                    text = if (showAmount) item.secondaryValue?.value.orEmpty() else "*****",
                    style = ComposeAppTheme.typography.subhead2,
                    color = item.secondaryValue?.color?.compose() ?: Color.Unspecified,
                    maxLines = 1,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onValueClick,
                    )
                )
            }
        }
    }
}

@Composable
private fun AmlStatusStripe(
    status: AmlStatus
) {
    if (status == AmlStatus.Loading) return
    Box(
        modifier = Modifier
            .size(28.dp)
            .graphicsLayer {
                translationX = -14.dp.toPx()
                translationY = -14.dp.toPx()
                rotationZ = 45f
            }
            .clip(RoundedCornerShape(topStart = 12.dp))
            .background(status.riskColor())
    )
}

@Composable
private fun AmlLoadingIndicator() {
    Box(
        modifier = Modifier
            .padding(8.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(12.dp),
            color = ComposeAppTheme.colors.grey,
            strokeWidth = 1.5.dp
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionCellPreview() {
    ComposeAppTheme {
        Box(
            modifier = Modifier
                .wrapContentHeight()
                .padding(horizontal = 16.dp)
        ) {
            TransactionCell(
                item = TransactionViewItem(
                    uid = "uid",
                    progress = null,
                    title = "Received Bitcoin",
                    subtitle = "From Alice to My Bitcoin Wallet",
                    icon = TransactionViewItem.Icon.Failed,
                    doubleSpend = true,
                    locked = true,
                    sentToSelf = true,
                    primaryValue = ColoredValue("0.00123 BTC", ColorName.Leah),
                    secondaryValue = ColoredValue("$45.67", ColorName.Leah),
                    date = Date(),
                    formattedTime = "12:00",
                    amlStatus = AmlStatus.Low
                ),
                position = SectionItemPosition.Middle,
                onValueClick = {},
                onClick = {}
            )
        }
    }
}

@Composable
private fun FilterTypeTabs(
    filterTypes: List<Filter<FilterTransactionType>>,
    onTransactionTypeClick: (FilterTransactionType) -> Unit
) {
    val tabItems = filterTypes.map {
        TabItem(stringResource(it.item.title), it.selected, it.item)
    }

    ScrollableTabs(tabItems) { transactionType ->
        onTransactionTypeClick.invoke(transactionType)
    }
}

data class Filter<T>(val item: T, val selected: Boolean)
