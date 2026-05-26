package io.horizontalsystems.bankwallet.modules.restorelocal

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.restoreaccount.RestoreViewModel
import io.horizontalsystems.bankwallet.modules.restoreaccount.restore_select_coins
import io.horizontalsystems.bankwallet.serializers.HSScreenKClassSerializer
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputPassword
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.Section
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightSelectors
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonStyle
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import io.horizontalsystems.bankwallet.uiv3.components.section.SectionHeaderColored
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class RestoreLocalPage(val input: Input) : HSPage() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        RestoreLocalNavHost(
            input.jsonFile,
            input.fileName,
            input.statPage,
            navController,
            input.popOffOnSuccess,
            input.popOffInclusive
        )
    }

    @Serializable
    data class Input(
        @Serializable(with = HSScreenKClassSerializer::class) val popOffOnSuccess: KClass<out HSPage>,
        val popOffInclusive: Boolean,
        val jsonFile: String,
        val fileName: String?,
        val statPage: StatPage
    )
}

@Composable
private fun RestoreLocalNavHost(
    backupJsonString: String?,
    fileName: String?,
    statPage: StatPage,
    navController: HSNavigation,
    popUpToInclusiveId: KClass<out HSPage>,
    popUpInclusive: Boolean,
) {
    val mainViewModel: RestoreViewModel = viewModel()
    val viewModel = hiltViewModel<RestoreLocalViewModel, RestoreLocalViewModel.Factory> { factory ->
        factory.create(backupJsonString, statPage, fileName)
    }

    RestoreLocalScreen(
        viewModel = viewModel,
        mainViewModel = mainViewModel,
        statPage = statPage,
        onBackClick = { navController.removeLastOrNull() },
        close = { navController.removeLastUntil(popUpToInclusiveId, popUpInclusive) },
        openSelectCoins = {
            val accountType = mainViewModel.accountType
            if (accountType != null) {
                navController.add(
                    restore_select_coins(
                        input = ManageAccountsModule.Input(popUpToInclusiveId, popUpInclusive),
                        accountType = accountType,
                        accountName = mainViewModel.accountName,
                        manualBackup = mainViewModel.manualBackup,
                        fileBackup = mainViewModel.fileBackup,
                        statPage = statPage
                    )
                )
            }
        },
        openBackupItems = {
            navController.add(
                backup_file(
                    input = ManageAccountsModule.Input(popUpToInclusiveId, popUpInclusive)
                )
            )
        }
    )
}

@Serializable
data class backup_file(val input: ManageAccountsModule.Input) : HSPage() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val viewModel = navController.viewModelForScreen<RestoreLocalViewModel>(
            RestoreLocalPage::class
        )

        val activity = LocalActivity.current
        BackupFileItems(
            viewModel = viewModel,
            onBackClick = { navController.removeLastOrNull() },
            close = { navController.removeLastUntil(input.popOffOnSuccess, input.popOffInclusive) },
            reloadApp = { activity?.let { MainModule.startAsNewTask(it) } }
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RestoreLocalScreen(
    viewModel: RestoreLocalViewModel,
    mainViewModel: RestoreViewModel,
    statPage: StatPage,
    onBackClick: () -> Unit,
    close: () -> Unit,
    openSelectCoins: () -> Unit,
    openBackupItems: () -> Unit
) {
    val uiState = viewModel.uiState
    var hidePassphrase by remember { mutableStateOf(true) }
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.restored) {
        if (uiState.restored) {
            HudHelper.showSuccessMessage(
                contenView = view,
                resId = R.string.Hud_Text_Restored,
                icon = R.drawable.icon_add_to_wallet_2_24,
                iconTint = R.color.white
            )
            delay(300)
            close.invoke()
        }
    }

    LaunchedEffect(uiState.parseError) {
        uiState.parseError?.let { error ->
            Toast.makeText(
                App.instance,
                error.message ?: error.javaClass.simpleName,
                Toast.LENGTH_LONG
            ).show()
            onBackClick.invoke()
        }
    }

    LaunchedEffect(uiState.showSelectCoins) {
        uiState.showSelectCoins?.let { accountType ->
            mainViewModel.setAccountData(
                accountType,
                viewModel.accountName,
                uiState.manualBackup,
                true,
                statPage
            )
            keyboardController?.hide()
            delay(300)
            viewModel.onSelectCoinsShown()
            openSelectCoins.invoke()

            stat(page = statPage, event = StatEvent.Open(StatPage.RestoreSelect))
        }
    }

    LaunchedEffect(uiState.showBackupItems) {
        if (uiState.showBackupItems) {
            keyboardController?.hide()
            delay(300)
            viewModel.onBackupItemsShown()
            openBackupItems.invoke()
        }
    }

    HSScaffold(
        title = viewModel.displayFileName ?: "",
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = onBackClick
            )
        ),
    ) {
        Column(
            modifier = Modifier.windowInsetsPadding(WindowInsets.ime)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {

                TextBlock(text = stringResource(R.string.ImportBackupFile_EnterPassword_Description))
                VSpacer(24.dp)
                FormsInputPassword(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    hint = stringResource(R.string.ImportBackupFile_BackupPassword),
                    state = uiState.passphraseState,
                    onValueChange = viewModel::onChangePassphrase,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    hide = hidePassphrase,
                    onToggleHide = {
                        hidePassphrase = !hidePassphrase
                    }
                )
                VSpacer(32.dp)
            }
            ButtonsGroupWithShade {
                HSButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    title = stringResource(R.string.Button_Next),
                    loadingIndicator = uiState.showButtonSpinner,
                    enabled = uiState.showButtonSpinner.not(),
                    onClick = {
                        viewModel.onImportClick()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupFileItems(
    viewModel: RestoreLocalViewModel,
    onBackClick: () -> Unit,
    close: () -> Unit,
    reloadApp: () -> Unit
) {
    val uiState = viewModel.uiState
    val walletBackupViewItems = viewModel.uiState.walletBackupViewItems
    val otherBackupViewItems = viewModel.uiState.otherBackupViewItems
    val view = LocalView.current

    LaunchedEffect(uiState.restored) {
        if (uiState.restored) {
            HudHelper.showSuccessMessage(
                contenView = view,
                resId = R.string.Hud_Text_Restored,
                icon = R.drawable.icon_add_to_wallet_2_24,
                iconTint = R.color.white
            )
            delay(300)
            close.invoke()
            reloadApp.invoke()
        }
    }

    LaunchedEffect(uiState.parseError) {
        uiState.parseError?.let { error ->
            Toast.makeText(
                App.instance,
                error.message ?: error.javaClass.simpleName,
                Toast.LENGTH_LONG
            ).show()
            onBackClick.invoke()
        }
    }

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    HSScaffold(
        title = viewModel.displayFileName ?: "",
        onBack = onBackClick,
        bottomBar = {
            ButtonsGroupWithShade {
                HSButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    title = stringResource(R.string.BackupManager_Restore),
                    enabled = uiState.hasSelection,
                    onClick = {
                        if (viewModel.shouldShowReplaceWarning()) {
                            showBottomSheet = true
                        } else {
                            viewModel.restoreFullBackup()
                        }
                    }
                )
            }
        }
    ) {
        LazyColumn {
            item {
                TextBlock(text = stringResource(R.string.BackupManager_RestoreItemsDescription))
            }

            if (walletBackupViewItems.isNotEmpty()) {
                item {
                    SectionHeaderColored(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = ComposeAppTheme.colors.grey,
                        title = stringResource(R.string.BackupManager_Wallets)
                    )
                    Section {
                        walletBackupViewItems.forEachIndexed { index, wallet ->
                            if (index > 0) Divider(color = ComposeAppTheme.colors.blade)
                            CellPrimary(
                                middle = {
                                    CellMiddleInfo(
                                        title = wallet.name.hs,
                                        subtitle = if (wallet.backupRequired)
                                            stringResource(R.string.BackupManager_BackupRequired).hs(color = ComposeAppTheme.colors.lucian)
                                        else
                                            wallet.type.hs
                                    )
                                },
                                right = {
                                    CellRightSelectors(
                                        icon = painterResource(if (wallet.selected) R.drawable.selector_checked_20 else R.drawable.selector_unchecked_20),
                                        iconTint = if (wallet.selected) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.grey
                                    )
                                },
                                onClick = { viewModel.toggleWallet(wallet) }
                            )
                        }
                    }
                }
            }

            if (otherBackupViewItems.isNotEmpty()) {
                item {
                    SectionHeaderColored(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = ComposeAppTheme.colors.grey,
                        title = stringResource(R.string.BackupManager_Other)
                    )
                    Section {
                        otherBackupViewItems.forEachIndexed { index, item ->
                            if (index > 0) Divider(color = ComposeAppTheme.colors.blade)
                            CellPrimary(
                                middle = {
                                    CellMiddleInfo(
                                        title = item.title.hs,
                                        subtitle = (item.value ?: item.subtitle)?.hs
                                    )
                                },
                                right = {
                                    CellRightSelectors(
                                        icon = painterResource(if (item.selected) R.drawable.selector_checked_20 else R.drawable.selector_unchecked_20),
                                        iconTint = if (item.selected) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.grey
                                    )
                                },
                                onClick = if (item.section != null) {
                                    { viewModel.toggleOtherItem(item) }
                                } else null
                            )
                        }
                    }
                    VSpacer(height = 32.dp)
                }
            }
        }
        if (showBottomSheet) {
            BottomSheetContent(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                buttons = {
                    HSButton(
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.Secondary,
                        title = stringResource(R.string.BackupManager_MergeButton),
                        onClick = {
                            viewModel.restoreFullBackup()
                            scope.launch {
                                sheetState.hide()
                                showBottomSheet = false
                            }
                        }
                    )
                    HSButton(
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.Secondary,
                        style = ButtonStyle.Transparent,
                        title = stringResource(R.string.Button_Cancel),
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                                showBottomSheet = false
                            }
                        }
                    )
                }
            ) {
                BottomSheetHeaderV3(
                    image72 = painterResource(R.drawable.warning_filled_24),
                    imageTint = ComposeAppTheme.colors.jacob,
                    title = stringResource(R.string.BackupManager_MergeTitle)
                )
                TextBlock(
                    text = stringResource(R.string.BackupManager_MergeDescription),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
