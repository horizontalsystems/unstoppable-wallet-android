package cash.p.terminal.modules.btcblockchainsettings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import cash.p.terminal.R
import cash.p.terminal.modules.btcblockchainsettings.BtcBlockchainSettingsModule.BlockchainSettingsIcon
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui.compose.components.FormsInput
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import coil.compose.rememberAsyncImagePainter

@Composable
internal fun BtcBlockchainSettingsScreen(
    uiState: BtcBlockchainSettingsUIState,
    onSaveClick: () -> Unit,
    onSelectRestoreMode: (BtcBlockchainSettingsModule.ViewItem) -> Unit,
    onCustomPeersChange: (String) -> Unit,
    navController: NavController,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
) {

    if (uiState.closeScreen) {
        navController.popBackStack()
    }

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column(modifier = Modifier.windowInsetsPadding(windowInsets)) {
            AppBar(
                title = uiState.title,
                navigationIcon = {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = uiState.blockchainIconUrl,
                            error = painterResource(R.drawable.ic_platform_placeholder_32)
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 14.dp)
                            .size(24.dp)
                    )
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = {
                            navController.popBackStack()
                        }
                    )
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(12.dp))
                RestoreSourceSettings(uiState.restoreSources, onSelectRestoreMode)
                if (uiState.customPeers != null) {
                    Spacer(Modifier.height(16.dp))
                    CustomPeersSettings(
                        customPeers = uiState.customPeers,
                        onCustomPeersChange = onCustomPeersChange
                    )
                }
                Spacer(Modifier.height(32.dp))
                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.BtcBlockchainSettings_RestoreSourceChangeWarning)
                )
                Spacer(Modifier.height(32.dp))
            }

            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    title = stringResource(R.string.Button_Save),
                    enabled = uiState.saveButtonEnabled,
                    onClick = onSaveClick
                )
            }
        }

    }
}

@Composable
private fun CustomPeersSettings(
    customPeers: String,
    onCustomPeersChange: (String) -> Unit
) {
    FormsInput(
        modifier = Modifier
            .padding(horizontal = 16.dp),
        initial = customPeers,
        pasteEnabled = false,
        hint = stringResource(R.string.custom_peers),
        onValueChange = onCustomPeersChange
    )
}


@Composable
private fun RestoreSourceSettings(
    restoreSources: List<BtcBlockchainSettingsModule.ViewItem>,
    onSelectRestoreMode: (BtcBlockchainSettingsModule.ViewItem) -> Unit
) {
    BlockchainSettingSection(restoreSources, onSelectRestoreMode)
}

@Composable
private fun BlockchainSettingSection(
    restoreSources: List<BtcBlockchainSettingsModule.ViewItem>,
    onItemClick: (BtcBlockchainSettingsModule.ViewItem) -> Unit
) {
    subhead2_grey(
        modifier = Modifier.padding(horizontal = 32.dp),
        text = stringResource(R.string.BtcBlockchainSettings_RestoreSourceSettingsDescription)
    )
    VSpacer(32.dp)
    CellUniversalLawrenceSection(restoreSources) { item ->
        BlockchainSettingCell(item.title, item.subtitle, item.selected, item.icon) {
            onItemClick(item)
        }
    }

}

@Composable
internal fun BlockchainSettingCell(
    title: String,
    subtitle: String,
    checked: Boolean,
    icon: BlockchainSettingsIcon?,
    onClick: () -> Unit
) {
    RowUniversal(
        onClick = onClick
    ) {
        icon?.let {
            HSpacer(width = 16.dp)
            Image(
                modifier = Modifier
                    .size(32.dp),
                painter = when (icon) {
                    is BlockchainSettingsIcon.ApiIcon -> painterResource(icon.resId)
                    is BlockchainSettingsIcon.BlockchainIcon -> rememberAsyncImagePainter(
                        model = icon.url,
                        error = painterResource(R.drawable.ic_platform_placeholder_32)
                    )
                },
                contentDescription = null,
            )
        }

        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            body_leah(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            subhead2_grey(
                text = subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    painter = painterResource(R.drawable.ic_checkmark_20),
                    tint = ComposeAppTheme.colors.jacob,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun BtcBlockchainSettingsScreenPreview() {
    ComposeAppTheme {
        BtcBlockchainSettingsScreen(
            uiState = BtcBlockchainSettingsUIState(
                title = "Bitcoin",
                blockchainIconUrl = "https://bitcoin.org/favicon.png",
                restoreSources = listOf(
                    BtcBlockchainSettingsModule.ViewItem(
                        id = "1",
                        title = "Blockchair",
                        subtitle = "Blockchair is a blockchain search and analytics engine",
                        selected = true,
                        icon = BlockchainSettingsIcon.ApiIcon(R.drawable.ic_blockchair)
                    ),
                    BtcBlockchainSettingsModule.ViewItem(
                        id = "2",
                        title = "Hybrid",
                        subtitle = "Hybrid is a blockchain search and analytics engine",
                        selected = false,
                        icon = BlockchainSettingsIcon.ApiIcon(R.drawable.ic_api_hybrid)
                    )
                ),
                saveButtonEnabled = true,
                closeScreen = false,
                customPeers = ""
            ),
            onSaveClick = {},
            onSelectRestoreMode = {},
            onCustomPeersChange = {},
            navController = rememberNavController()
        )
    }
}