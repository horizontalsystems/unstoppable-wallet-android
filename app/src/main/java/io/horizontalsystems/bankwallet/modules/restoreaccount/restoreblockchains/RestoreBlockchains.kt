package io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.enablecoin.blockchaintokens.BlockchainTokensViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import io.horizontalsystems.bankwallet.modules.restoreaccount.RestoreViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellMultilineClear
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorMultiple
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ManageWalletsScreen(
    mainViewModel: RestoreViewModel,
    openZCashConfigure: () -> Unit,
    onBackClick: () -> Unit,
    onFinish: () -> Unit
) {
    val accountType = mainViewModel.accountType ?: run {
        Toast.makeText(App.instance, "Error: accountType is NULL", Toast.LENGTH_SHORT).show()
        onBackClick.invoke()
        return
    }
    val manualBackup = mainViewModel.manualBackup
    val fileBackup = mainViewModel.fileBackup

    val factory = RestoreBlockchainsModule.Factory(mainViewModel.accountName, accountType, manualBackup, fileBackup)
    val viewModel: RestoreBlockchainsViewModel = viewModel(factory = factory)
    val restoreSettingsViewModel: RestoreSettingsViewModel = viewModel(factory = factory)
    val blockchainTokensViewModel: BlockchainTokensViewModel = viewModel(factory = factory)

    val view = LocalView.current

    val coinItems by viewModel.viewItemsLiveData.observeAsState()
    val doneButtonEnabled by viewModel.restoreEnabledLiveData.observeAsState(false)
    val restored = viewModel.restored

    mainViewModel.zCashConfig?.let { config ->
        restoreSettingsViewModel.onEnter(config)
        mainViewModel.setZCashConfig(null)
    }

    if (mainViewModel.cancelZCashConfig) {
        restoreSettingsViewModel.onCancelEnterBirthdayHeight()
        mainViewModel.cancelZCashConfig = false
    }

    if (restoreSettingsViewModel.openZcashConfigure != null) {
        restoreSettingsViewModel.zcashConfigureOpened()
        openZCashConfigure.invoke()
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

    val coroutineScope = rememberCoroutineScope()
    val skipHalfExpanded by remember { mutableStateOf(true) }
    val modalBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = skipHalfExpanded
    )

    LaunchedEffect(blockchainTokensViewModel.showBottomSheetDialog) {
        if (blockchainTokensViewModel.showBottomSheetDialog) {
            coroutineScope.launch {
                modalBottomSheetState.show()
            }
            blockchainTokensViewModel.bottomSheetDialogShown()
        }
    }

    ModalBottomSheetLayout(
        sheetState = modalBottomSheetState,
        sheetBackgroundColor = ComposeAppTheme.colors.transparent,
        sheetContent = {
            blockchainTokensViewModel.config?.let { config ->
                BottomSheetSelectorMultiple(
                    config = config,
                    onItemsSelected = { blockchainTokensViewModel.onSelect(it) },
                    onCloseClick = {
                        blockchainTokensViewModel.onCancelSelect()
                        coroutineScope.launch { modalBottomSheetState.hide() }
                    },
                )
            }
        },
    ) {
        Column(
            modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
        ) {
            AppBar(
                title = TranslatableString.ResString(R.string.Restore_Title),
                navigationIcon = {
                    HsBackButton(onClick = onBackClick)
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Restore),
                        onClick = { viewModel.onRestore() },
                        enabled = doneButtonEnabled
                    )
                ),
            )

            LazyColumn {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(
                        thickness = 1.dp,
                        color = ComposeAppTheme.colors.steel10,
                    )
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
                                Spacer(Modifier.width(12.dp))
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
            }
        }
    }
}

private fun onItemClick(viewItem: CoinViewItem<Blockchain>, viewModel: RestoreBlockchainsViewModel) {
    if (viewItem.enabled) {
        viewModel.disable(viewItem.item)
    } else {
        viewModel.enable(viewItem.item)
    }
}
