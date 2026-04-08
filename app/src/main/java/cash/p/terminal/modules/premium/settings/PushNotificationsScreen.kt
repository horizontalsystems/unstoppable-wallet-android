@file:Suppress("UnusedPrivateMember")

package cash.p.terminal.modules.premium.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.ui.compose.components.SelectorDialogCompose
import cash.p.terminal.ui.compose.components.SelectorItem
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.HSCircularProgressIndicator
import cash.p.terminal.ui_compose.components.HsSwitch
import cash.p.terminal.ui_compose.components.InfoText
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.SwitchWithText
import cash.p.terminal.ui_compose.components.SwitchWithTextWarning
import cash.p.terminal.ui_compose.components.TextImportantError
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_grey50
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.subhead1_leah
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.chartview.rememberAsyncImagePainterWithFallback
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.imageUrl

@Suppress("LongParameterList")
@Composable
internal fun PushNotificationsScreen(
    uiState: PushNotificationsUiState,
    onShowNotificationsToggle: (Boolean) -> Unit,
    onBlockchainNotificationsToggle: (String, Boolean) -> Unit,
    onPollingIntervalChange: (PollingInterval) -> Unit,
    onShowBlockchainNameToggle: (Boolean) -> Unit,
    onShowCoinAmountToggle: (Boolean) -> Unit,
    onShowFiatAmountToggle: (Boolean) -> Unit,
    onClose: () -> Unit,
    noNotificationPermission: Boolean = false,
    onPermissionWarningClick: () -> Unit = {},
) {
    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.push_notification),
                navigationIcon = {
                    HsBackButton(onClick = onClose)
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            VSpacer(12.dp)
            CellUniversalLawrenceSection {
                SwitchWithTextWarning(
                    text = stringResource(R.string.premium_push_notifications_show),
                    checked = uiState.showNotifications,
                    showWarning = uiState.showNotifications && noNotificationPermission,
                    onWarningIconClick = onPermissionWarningClick,
                    onCheckedChange = onShowNotificationsToggle,
                )
            }
            if (uiState.showNotifications && noNotificationPermission) {
                VSpacer(12.dp)
                TextImportantError(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.notification_permission_revoked_message),
                )
            }
            VSpacer(12.dp)
            TextImportantWarning(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.premium_push_notifications_warning)
            )
            PollingIntervalSection(
                pollingInterval = uiState.pollingInterval,
                enabled = uiState.showNotifications,
                onPollingIntervalChange = onPollingIntervalChange,
            )
            BlockchainsSection(
                uiState = uiState,
                onBlockchainNotificationsToggle = onBlockchainNotificationsToggle,
            )
            NotificationContentSection(
                uiState = uiState,
                onShowBlockchainNameToggle = onShowBlockchainNameToggle,
                onShowCoinAmountToggle = onShowCoinAmountToggle,
                onShowFiatAmountToggle = onShowFiatAmountToggle,
            )
            VSpacer(32.dp)
        }
    }
}

@Composable
private fun PollingIntervalSection(
    pollingInterval: PollingInterval,
    enabled: Boolean,
    onPollingIntervalChange: (PollingInterval) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        SelectorDialogCompose(
            title = stringResource(R.string.push_notification_polling_interval),
            items = PollingInterval.entries.map { interval ->
                SelectorItem(
                    title = stringResource(interval.titleResId),
                    selected = interval == pollingInterval,
                    item = interval,
                )
            },
            onDismissRequest = { showDialog = false },
            onSelectItem = { interval ->
                onPollingIntervalChange(interval)
                showDialog = false
            }
        )
    }

    VSpacer(20.dp)
    InfoText(
        text = stringResource(R.string.push_notification_polling_interval).uppercase(),
        paddingBottom = 8.dp
    )
    CellUniversalLawrenceSection {
        RowUniversal(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .alpha(if (enabled) 1f else 0.5f),
            onClick = if (enabled) {
                { showDialog = true }
            } else {
                null
            }
        ) {
            subhead2_grey(
                text = stringResource(R.string.push_notification_polling_interval),
                modifier = Modifier.weight(1f)
            )
            subhead1_leah(
                text = stringResource(pollingInterval.titleResId)
            )
        }
    }
}

@Composable
private fun BlockchainsSection(
    uiState: PushNotificationsUiState,
    onBlockchainNotificationsToggle: (String, Boolean) -> Unit,
) {
    VSpacer(20.dp)
    InfoText(
        text = stringResource(R.string.Market_Filter_Blockchains).uppercase(),
        paddingBottom = 8.dp
    )
    if (uiState.loading) {
        LoadingBlockchainsSection()
    } else {
        CellUniversalLawrenceSection(uiState.blockchains) { item ->
            PushNotificationBlockchainCell(
                item = item,
                enabled = uiState.showNotifications,
                onToggle = { enabled ->
                    onBlockchainNotificationsToggle(item.uid, enabled)
                }
            )
        }
    }
}

@Composable
private fun NotificationContentSection(
    uiState: PushNotificationsUiState,
    onShowBlockchainNameToggle: (Boolean) -> Unit,
    onShowCoinAmountToggle: (Boolean) -> Unit,
    onShowFiatAmountToggle: (Boolean) -> Unit,
) {
    VSpacer(20.dp)
    InfoText(
        text = stringResource(R.string.push_notification_content).uppercase(),
        paddingBottom = 8.dp
    )
    CellUniversalLawrenceSection(
        listOf(
            {
                SwitchWithText(
                    text = stringResource(R.string.push_notification_show_blockchain),
                    checked = uiState.showBlockchainName,
                    enabled = uiState.showNotifications,
                    onCheckedChange = onShowBlockchainNameToggle
                )
            },
            {
                SwitchWithText(
                    text = stringResource(R.string.push_notification_show_coin_amount),
                    checked = uiState.showCoinAmount,
                    enabled = uiState.showNotifications,
                    onCheckedChange = onShowCoinAmountToggle
                )
            },
            {
                SwitchWithText(
                    text = stringResource(R.string.push_notification_show_fiat_amount),
                    checked = uiState.showFiatAmount,
                    enabled = uiState.showNotifications,
                    onCheckedChange = onShowFiatAmountToggle
                )
            }
        )
    )
}

@Composable
private fun LoadingBlockchainsSection() {
    CellUniversalLawrenceSection {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            HSCircularProgressIndicator(progress = 0.15f, size = 32.dp)
        }
    }
}

@Composable
private fun PushNotificationBlockchainCell(
    item: PushNotificationBlockchainViewItem,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = if (enabled) {
            {
                onToggle(!item.notificationsEnabled)
            }
        } else {
            null
        }
    ) {
        Image(
            painter = rememberAsyncImagePainterWithFallback(
                model = item.imageUrl,
                error = painterResource(R.drawable.ic_platform_placeholder_32)
            ),
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .alpha(if (enabled) 1f else 0.5f)
        )
        if (enabled) {
            body_leah(
                text = item.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )
        } else {
            body_grey50(
                text = item.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        HsSwitch(
            checked = item.notificationsEnabled,
            enabled = enabled,
            onCheckedChange = onToggle
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PushNotificationsScreenPreviewEnabled() {
    ComposeAppTheme {
        PushNotificationsScreen(
            uiState = PushNotificationsUiState(
                showNotifications = true,
                pollingInterval = PollingInterval.REALTIME,
                showBlockchainName = true,
                showCoinAmount = true,
                showFiatAmount = true,
                blockchains = previewBlockchains(),
                loading = false,
            ),
            onShowNotificationsToggle = {},
            onBlockchainNotificationsToggle = { _, _ -> },
            onPollingIntervalChange = {},
            onShowBlockchainNameToggle = {},
            onShowCoinAmountToggle = {},
            onShowFiatAmountToggle = {},
            onClose = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PushNotificationsScreenPreviewDisabled() {
    ComposeAppTheme {
        PushNotificationsScreen(
            uiState = PushNotificationsUiState(
                showNotifications = false,
                pollingInterval = PollingInterval.REALTIME,
                showBlockchainName = true,
                showCoinAmount = true,
                showFiatAmount = true,
                blockchains = previewBlockchains(),
                loading = false,
            ),
            onShowNotificationsToggle = {},
            onBlockchainNotificationsToggle = { _, _ -> },
            onPollingIntervalChange = {},
            onShowBlockchainNameToggle = {},
            onShowCoinAmountToggle = {},
            onShowFiatAmountToggle = {},
            onClose = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PushNotificationsScreenPreviewLoading() {
    ComposeAppTheme {
        PushNotificationsScreen(
            uiState = PushNotificationsUiState(
                showNotifications = true,
                pollingInterval = PollingInterval.REALTIME,
                showBlockchainName = true,
                showCoinAmount = true,
                showFiatAmount = true,
                blockchains = emptyList(),
                loading = true,
            ),
            onShowNotificationsToggle = {},
            onBlockchainNotificationsToggle = { _, _ -> },
            onPollingIntervalChange = {},
            onShowBlockchainNameToggle = {},
            onShowCoinAmountToggle = {},
            onShowFiatAmountToggle = {},
            onClose = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PushNotificationsScreenPreviewPermissionRevoked() {
    ComposeAppTheme {
        PushNotificationsScreen(
            uiState = PushNotificationsUiState(
                showNotifications = true,
                pollingInterval = PollingInterval.REALTIME,
                showBlockchainName = true,
                showCoinAmount = true,
                showFiatAmount = true,
                blockchains = previewBlockchains(),
                loading = false,
            ),
            onShowNotificationsToggle = {},
            onBlockchainNotificationsToggle = { _, _ -> },
            onPollingIntervalChange = {},
            onShowBlockchainNameToggle = {},
            onShowCoinAmountToggle = {},
            onShowFiatAmountToggle = {},
            onClose = {},
            noNotificationPermission = true,
            onPermissionWarningClick = {},
        )
    }
}

private fun previewBlockchains() = listOf(
    PushNotificationBlockchainViewItem(
        uid = BlockchainType.Bitcoin.uid,
        name = "Bitcoin",
        imageUrl = BlockchainType.Bitcoin.imageUrl,
        notificationsEnabled = true
    ),
    PushNotificationBlockchainViewItem(
        uid = BlockchainType.Ethereum.uid,
        name = "Ethereum",
        imageUrl = BlockchainType.Ethereum.imageUrl,
        notificationsEnabled = false
    ),
    PushNotificationBlockchainViewItem(
        uid = BlockchainType.Tron.uid,
        name = "Tron",
        imageUrl = BlockchainType.Tron.imageUrl,
        notificationsEnabled = true
    ),
    PushNotificationBlockchainViewItem(
        uid = BlockchainType.Zcash.uid,
        name = "Zcash",
        imageUrl = BlockchainType.Zcash.imageUrl,
        notificationsEnabled = true
    )
)
