package io.horizontalsystems.bankwallet.modules.btcblockchainsettings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.btcblockchainsettings.BtcBlockchainSettingsModule.BlockchainSettingsIcon
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.marketkit.models.Blockchain

class BtcBlockchainSettingsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Blockchain>(navController) { input ->
            val viewModel = viewModel<BtcBlockchainSettingsViewModel>(
                factory = BtcBlockchainSettingsModule.Factory(input)
            )
            BtcBlockchainSettingsScreen(viewModel, navController)
        }
    }

}

@Composable
private fun BtcBlockchainSettingsScreen(
    viewModel: BtcBlockchainSettingsViewModel,
    navController: NavController
) {

    if (viewModel.closeScreen) {
        navController.popBackStack()
    }

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                title = viewModel.title,
                navigationIcon = {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = viewModel.blockchainIconUrl,
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
                RestoreSourceSettings(viewModel)
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
                    enabled = viewModel.saveButtonEnabled,
                    onClick = { viewModel.onSaveClick() }
                )
            }
        }

    }
}

@Composable
private fun RestoreSourceSettings(
    viewModel: BtcBlockchainSettingsViewModel
) {
    BlockchainSettingSection(viewModel.restoreSources) { viewItem ->
        viewModel.onSelectRestoreMode(viewItem)
    }
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
fun BlockchainSettingCell(
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

        Column(modifier = Modifier
            .padding(start = 16.dp)
            .weight(1f)) {
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
