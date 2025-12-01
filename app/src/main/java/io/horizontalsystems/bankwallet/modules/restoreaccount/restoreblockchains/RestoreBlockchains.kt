package io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.enablecoin.blockchaintokens.BlockchainTokensViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import io.horizontalsystems.bankwallet.modules.restoreaccount.RestoreViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.CellMultilineClear
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorMultiple
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageWalletsScreen(
    mainViewModel: RestoreViewModel,
    openBirthdayHeightConfigure: (Token) -> Unit,
    onBackClick: () -> Unit,
    onFinish: () -> Unit
) {
    val accountType = mainViewModel.accountType ?: run {
        Toast.makeText(App.instance, "Error: accountType is NULL", Toast.LENGTH_SHORT).show()
        onBackClick.invoke()
        return
    }

    val statPage = mainViewModel.statPage ?: run {
        Toast.makeText(App.instance, "Error: statPage is NULL", Toast.LENGTH_SHORT).show()
        onBackClick.invoke()
        return
    }

    val manualBackup = mainViewModel.manualBackup
    val fileBackup = mainViewModel.fileBackup

    val factory = RestoreBlockchainsModule.Factory(
        mainViewModel.accountName,
        accountType,
        manualBackup,
        fileBackup,
        statPage
    )
    val viewModel: RestoreBlockchainsViewModel = viewModel(factory = factory)
    val restoreSettingsViewModel: RestoreSettingsViewModel = viewModel(factory = factory)
    val blockchainTokensViewModel: BlockchainTokensViewModel = viewModel(factory = factory)

    val view = LocalView.current

    val coinItems by viewModel.viewItemsLiveData.observeAsState()
    val doneButtonEnabled by viewModel.restoreEnabledLiveData.observeAsState(false)
    val restored = viewModel.restored

    mainViewModel.birthdayHeightConfig?.let { config ->
        restoreSettingsViewModel.onEnter(config)
        mainViewModel.setBirthdayHeightConfig(null)
    }

    if (mainViewModel.cancelBirthdayHeightConfig) {
        restoreSettingsViewModel.onCancelEnterBirthdayHeight()
        mainViewModel.cancelBirthdayHeightConfig = false
    }

    restoreSettingsViewModel.openBirthdayHeightConfig?.let { token ->
        restoreSettingsViewModel.birthdayHeightConfigOpened()
        openBirthdayHeightConfigure.invoke(token)

        stat(page = StatPage.RestoreSelect, event = StatEvent.Open(StatPage.BirthdayInput))
    }

    LaunchedEffect(restored) {
        if (restored) {
            HudHelper.showSuccessMessage(
                contenView = view,
                resId = R.string.Hud_Text_Restored,
                icon = R.drawable.icon_add_to_wallet_2_24,
                iconTint = R.color.white
            )
            delay(300)
            onFinish.invoke()
        }
    }

    val skipHalfExpanded by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = skipHalfExpanded,
    )
    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(blockchainTokensViewModel.showBottomSheetDialog) {
        if (blockchainTokensViewModel.showBottomSheetDialog) {
            showBottomSheet = true
            blockchainTokensViewModel.bottomSheetDialogShown()
        }
    }

    HSScaffold(
        title = stringResource(R.string.Restore_Title),
        onBack = onBackClick,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Restore),
                onClick = { viewModel.onRestore() },
                enabled = doneButtonEnabled,
                tint = ComposeAppTheme.colors.jacob
            )
        ),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                HsDivider()
            }
            coinItems?.let {
                items(it) { viewItem ->
                    CellMultilineClear(
                        borderBottom = true,
                        onClick = { onItemClick(viewItem, viewModel) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            Image(
                                painter = viewItem.imageSource.painter(),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(32.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                body_leah(
                                    text = viewItem.title,
                                    maxLines = 1,
                                )
                                subhead2_grey(
                                    text = viewItem.subtitle,
                                    maxLines = 1,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                            HSpacer(12.dp)
                            if (viewItem.hasSettings) {
                                HsIconButton(
                                    onClick = { viewModel.onClickSettings(viewItem.item) }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_edit_20),
                                        contentDescription = null,
                                        tint = ComposeAppTheme.colors.grey
                                    )
                                }
                            }
                            HsSwitch(
                                checked = viewItem.enabled,
                                onCheckedChange = { onItemClick(viewItem, viewModel) },
                            )
                        }
                    }
                }
            }

            item {
                VSpacer(height = 32.dp)
            }
        }

        if (showBottomSheet) {
            blockchainTokensViewModel.config?.let { config ->
                BottomSheetSelectorMultiple(
                    sheetState = sheetState,
                    config = config,
                    onItemsSelected = { blockchainTokensViewModel.onSelect(it) },
                    onCloseClick = {
                        blockchainTokensViewModel.onCancelSelect()
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                    },
                )
            }
        }
    }
}

private fun onItemClick(
    viewItem: CoinViewItem<Blockchain>,
    viewModel: RestoreBlockchainsViewModel
) {
    if (viewItem.enabled) {
        viewModel.disable(viewItem.item)
    } else {
        viewModel.enable(viewItem.item)
    }
}
