package cash.p.terminal.modules.managewallets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import cash.p.terminal.R
import cash.p.terminal.modules.enablecoin.restoresettings.IRestoreSettingsUi
import cash.p.terminal.modules.enablecoin.restoresettings.TokenConfig
import cash.p.terminal.modules.enablecoin.restoresettings.openRestoreSettingsDialog
import cash.p.terminal.modules.restoreaccount.restoreblockchains.CoinViewItem
import cash.p.terminal.modules.addtoken.AddTokenFragment
import cash.p.terminal.navigation.slideFromRightForResult
import cash.p.terminal.navigation.slideFromBottom
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui.compose.components.ListEmptyView
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryDefaults
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.HsIconButton
import cash.p.terminal.ui_compose.components.HsSwitch
import cash.p.terminal.ui_compose.components.HudHelper
import cash.p.terminal.ui_compose.components.ImageSource
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.SearchField
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.ui_compose.theme.SteelLight
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun ManageWalletsScreen(
    navController: NavController,
    manageWalletsCallback: ManageWalletsCallback,
    onBackPressed: () -> Unit,
    requestScan: () -> Unit,
    restoreSettingsViewModel: IRestoreSettingsUi
) {
    val groupsList by manageWalletsCallback.groupsList.collectAsStateWithLifecycle()
    val context = LocalView.current
    var initialLoading by remember { mutableStateOf(true) }

    LaunchedEffect(groupsList) {
        if (groupsList.isNotEmpty()) {
            initialLoading = false
        }
    }

    restoreSettingsViewModel.openTokenConfigure?.let { token ->
        navController.openRestoreSettingsDialog(token, restoreSettingsViewModel)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
        ) {
            AppBar(
                title = stringResource(R.string.ManageCoins_title),
                navigationIcon = {
                    HsBackButton(onClick = onBackPressed)
                },
                menuItems = if (manageWalletsCallback.addTokenEnabled) {
                    listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.ManageCoins_AddToken),
                            icon = R.drawable.ic_add_yellow,
                            onClick = {
                                navController.slideFromRightForResult<AddTokenFragment.Result>(R.id.addTokenFragment) { result ->
                                    if (result.success) {
                                        navController.popBackStack(R.id.mainFragment, false)
                                    }
                                }
                            }
                        ))
                } else {
                    listOf()
                }
            )

            SearchField(
                onSearchTextChanged = manageWalletsCallback::updateFilter,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
            )

            if (groupsList.isEmpty()) {
                if (initialLoading) {
                    LoadingComponent()
                } else {
                    Column {
                        ListEmptyView(
                            text = stringResource(R.string.ManageCoins_NoResults),
                            icon = R.drawable.ic_not_found,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            } else {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = ComposeAppTheme.colors.steel10,
                )
                LazyColumn(Modifier.weight(1f)) {
                    items(
                        items = groupsList,
                        key = { it.coinUid }
                    ) { group ->
                        CoinGroupItem(
                            group = group,
                            onGroupClick = {
                                manageWalletsCallback.toggleGroupExpansion(group.coinUid)
                            },
                            onItemClick = { token ->
                                if (group.items.find { it.item == token }?.enabled == true) {
                                    manageWalletsCallback.disable(token)
                                } else {
                                    manageWalletsCallback.enable(token)
                                }
                            },
                            onInfoClick = { token ->
                                navController.slideFromBottom(
                                    R.id.configuredTokenInfo,
                                    token
                                )
                            }
                        )
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = ComposeAppTheme.colors.steel10,
                        )
                    }
                    item {
                        VSpacer(height = 32.dp)
                    }
                    if (manageWalletsCallback.showScanToAddButton) {
                        item {
                            VSpacer(height = ButtonPrimaryDefaults.MinHeight + 32.dp)
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = manageWalletsCallback.showScanToAddButton,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            ScanToAddBlock(requestScan)
        }
        LaunchedEffect(manageWalletsCallback.errorMsg) {
            if (manageWalletsCallback.errorMsg != null) {
                HudHelper.showErrorMessage(context, manageWalletsCallback.errorMsg!!)
            }
        }

        LaunchedEffect(manageWalletsCallback.closeScreen) {
            if (manageWalletsCallback.closeScreen) {
                navController.popBackStack()
            }
        }
        val totalItems = remember(groupsList) { groupsList.sumOf { it.items.size } }
        TotalItemsPanel(
            count = totalItems,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
        )
    }
}

@Composable
private fun TotalItemsPanel(count: Int, modifier: Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .background(ComposeAppTheme.colors.midnight)
    ) {
        Text(
            text = pluralStringResource(
                R.plurals.entries_count,
                count,
                count
            ),
            style = ComposeAppTheme.typography.caption,
            color = SteelLight,
        )
    }
}

@Composable
private fun LoadingComponent() {
    Box(Modifier.fillMaxSize()) {
        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.Center)
                .size(56.dp)
                .padding(top = 4.dp, end = 8.dp),
            color = ComposeAppTheme.colors.grey,
            strokeWidth = 4.dp
        )
    }
}

@Composable
private fun CoinGroupItem(
    group: CoinGroup,
    onGroupClick: () -> Unit,
    onItemClick: (Token) -> Unit,
    onInfoClick: (Token) -> Unit
) {
    if (group.isSingleOption) {
        val item = group.items.first()
        TokenRow(
            viewItem = item,
            startPadding = 16.dp,
            iconSize = 32.dp,
            onItemClick = {
                onItemClick(item.item)
            },
            onInfoClick = {
                onInfoClick(item.item)
            }
        )
    } else {
        Column {
            GroupHeader(
                group = group,
                onClick = onGroupClick
            )

            AnimatedVisibility(
                visible = group.isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    group.items.forEach { viewItem ->
                        TokenRow(
                            viewItem = viewItem,
                            startPadding = 32.dp,
                            iconSize = 24.dp,
                            onItemClick = {
                                onItemClick(viewItem.item)
                            },
                            onInfoClick = {
                                onInfoClick(viewItem.item)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(
    group: CoinGroup,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (group.isExpanded) 180f else 0f,
        label = "arrow_rotation"
    )

    RowUniversal(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalPadding = 0.dp
    ) {
        val firstItem = group.items.firstOrNull()
        firstItem?.let {
            Image(
                painter = it.imageSource.painter(),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 16.dp, top = 12.dp, bottom = 12.dp)
                    .size(32.dp)
                    .clip(CircleShape)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
        ) {
            body_leah(
                text = group.coinName,
                maxLines = 1,
            )
            subhead2_grey(
                text = pluralStringResource(
                    R.plurals.options_count,
                    group.items.size,
                    group.items.size
                ),
                maxLines = 1,
                modifier = Modifier.padding(top = 1.dp)
            )
        }

        Icon(
            painter = painterResource(R.drawable.ic_arrow_big_down_20),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey,
            modifier = Modifier
                .padding(end = 2.dp)
                .rotate(rotation)
        )
    }
}

@Composable
private fun ScanToAddBlock(requestScan: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        color = ComposeAppTheme.colors.tyler,
    ) {
        ButtonPrimaryYellow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp + with(LocalDensity.current) {
                        NavigationBarDefaults.windowInsets
                            .getBottom(LocalDensity.current)
                            .toDp()
                    },
                    top = 16.dp
                ),
            title = stringResource(R.string.scan_card_to_add),
            onClick = requestScan
        )
    }
}

@Composable
private fun TokenRow(
    viewItem: CoinViewItem<Token>,
    startPadding: Dp,
    iconSize: Dp,
    onItemClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    RowUniversal(
        onClick = onItemClick,
        modifier = Modifier.padding(start = startPadding, end = 16.dp),
        verticalPadding = 0.dp
    ) {
        Image(
            painter = viewItem.imageSource.painter(),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp, top = 12.dp, bottom = 12.dp)
                .size(iconSize)
                .clip(CircleShape)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                body_leah(
                    text = viewItem.title,
                    maxLines = 1,
                )
                viewItem.label?.let { labelText ->
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ComposeAppTheme.colors.jeremy)
                    ) {
                        Text(
                            modifier = Modifier.padding(
                                start = 4.dp,
                                end = 4.dp,
                                bottom = 1.dp
                            ),
                            text = labelText,
                            color = ComposeAppTheme.colors.bran,
                            style = ComposeAppTheme.typography.microSB,
                            maxLines = 1,
                        )
                    }
                }
            }
            subhead2_grey(
                text = viewItem.subtitle,
                maxLines = 1,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        if (viewItem.hasInfo) {
            HsIconButton(onClick = onInfoClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_info_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }
        }
        HsSwitch(
            modifier = Modifier.padding(0.dp),
            checked = viewItem.enabled,
            onCheckedChange = { onItemClick.invoke() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ManageWalletsScreenPreview() {
    ComposeAppTheme(darkTheme = true) {
        val items = listOf(
            CoinViewItem(
                item = Token(
                    coin = Coin("Bitcoin", "Bitcoin", "BTC"),
                    blockchain = Blockchain(BlockchainType.Bitcoin, "Bitcoin", null),
                    type = TokenType.Native,
                    decimals = 8
                ),
                imageSource = ImageSource.Local(R.drawable.ic_placeholder),
                title = "BTC",
                subtitle = "Bitcoin",
                enabled = true
            ),
            CoinViewItem(
                item = Token(
                    coin = Coin("Ethereum", "Ethereum", "ETH"),
                    blockchain = Blockchain(BlockchainType.Ethereum, "Ethereum", null),
                    type = TokenType.Native,
                    decimals = 18
                ),
                imageSource = ImageSource.Local(R.drawable.ic_placeholder),
                title = "ETH",
                subtitle = "Ethereum",
                enabled = false,
                label = "ERC20",
                hasInfo = true
            ),
            CoinViewItem(
                item = Token(
                    coin = Coin("Ethereum", "Ethereum", "ETH"),
                    blockchain = Blockchain(BlockchainType.Base, "Base", null),
                    type = TokenType.Native,
                    decimals = 18
                ),
                imageSource = ImageSource.Local(R.drawable.ic_placeholder),
                title = "ETH",
                subtitle = "Ethereum",
                enabled = true,
                label = "BASE",
                hasInfo = true
            )
        )

        val groups = listOf(
            CoinGroup(
                coinName = "Bitcoin",
                coinUid = "bitcoin",
                items = listOf(items[0]),
                isExpanded = false
            ),
            CoinGroup(
                coinName = "Ethereum",
                coinUid = "ethereum",
                items = listOf(items[1], items[2]),
                isExpanded = true
            )
        )

        ManageWalletsScreen(
            navController = rememberNavController(),
            manageWalletsCallback = object : ManageWalletsCallback {
                override val groupsList = MutableStateFlow(groups)
                override val addTokenEnabled = true
                override val showScanToAddButton = false
                override val errorMsg: String? = null
                override val closeScreen: Boolean = false
                override fun updateFilter(text: String) = Unit
                override fun enable(token: Token) = Unit
                override fun disable(token: Token) = Unit
                override fun toggleGroupExpansion(coinUid: String) = Unit
            },
            onBackPressed = {},
            requestScan = {},
            restoreSettingsViewModel = object : IRestoreSettingsUi {
                override val openTokenConfigure: Token? = null
                override fun tokenConfigureOpened() = Unit
                override fun consumeInitialConfig(): TokenConfig? = null
                override fun onEnter(tokenConfig: TokenConfig) = Unit
                override fun onCancelEnterBirthdayHeight() = Unit
            }
        )
    }
}
