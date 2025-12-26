package io.horizontalsystems.bankwallet.modules.restorelocal

import android.os.Parcelable
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.Caution
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.core.composablePopup
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.OtherBackupItems
import io.horizontalsystems.bankwallet.modules.contacts.screen.ConfirmationBottomSheet
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.restoreaccount.RestoreViewModel
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.ManageWalletsScreen
import io.horizontalsystems.bankwallet.modules.restoreconfig.BirthdayHeightConfigScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellowWithSpinner
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputPassword
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_lucian
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class RestoreLocalFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            RestoreLocalNavHost(
                input.jsonFile,
                input.fileName,
                input.statPage,
                navController,
                input.popOffOnSuccess,
                input.popOffInclusive
            ) { activity?.let { MainModule.startAsNewTask(it) } }
        }
    }

    @Parcelize
    data class Input(
        val popOffOnSuccess: Int,
        val popOffInclusive: Boolean,
        val jsonFile: String,
        val fileName: String?,
        val statPage: StatPage
    ) : Parcelable
}

@Composable
private fun RestoreLocalNavHost(
    backupJsonString: String?,
    fileName: String?,
    statPage: StatPage,
    fragmentNavController: NavController,
    popUpToInclusiveId: Int,
    popUpInclusive: Boolean,
    reloadApp: () -> Unit,
) {
    val navController = rememberNavController()
    val mainViewModel: RestoreViewModel = viewModel()
    val viewModel = viewModel<RestoreLocalViewModel>(
        factory = RestoreLocalModule.Factory(
            backupJsonString,
            fileName,
            statPage
        )
    )
    NavHost(
        navController = navController,
        startDestination = "restore_local",
    ) {
        composable("restore_local") {
            RestoreLocalScreen(
                viewModel = viewModel,
                mainViewModel = mainViewModel,
                statPage = statPage,
                onBackClick = { fragmentNavController.popBackStack() },
                close = { fragmentNavController.popBackStack(popUpToInclusiveId, popUpInclusive) },
                openSelectCoins = { navController.navigate("restore_select_coins") },
                openBackupItems = { navController.navigate("backup_file") }
            )
        }
        composablePage("backup_file") {
            BackupFileItems(
                viewModel,
                onBackClick = { navController.popBackStack() },
                close = { fragmentNavController.popBackStack(popUpToInclusiveId, popUpInclusive) },
                reloadApp = reloadApp
            )
        }
        composablePage("restore_select_coins") {
            ManageWalletsScreen(
                mainViewModel = mainViewModel,
                openBirthdayHeightConfigure = { token ->
                    when (token.blockchainType) {
                        BlockchainType.Zcash -> navController.navigate("zcash_configure")
                        BlockchainType.Monero -> navController.navigate("monero_configure")
                        else -> Unit
                    }
                },
                onBackClick = { navController.popBackStack() }
            ) { fragmentNavController.popBackStack(popUpToInclusiveId, popUpInclusive) }
        }
        composablePopup("zcash_configure") {
            BirthdayHeightConfigScreen(
                blockchainType = BlockchainType.Zcash,
                onCloseWithResult = { config ->
                    mainViewModel.setBirthdayHeightConfig(config)
                    navController.popBackStack()
                },
                onCloseClick = {
                    mainViewModel.cancelBirthdayHeightConfig = true
                    navController.popBackStack()
                }
            )
        }
        composablePopup("monero_configure") {
            BirthdayHeightConfigScreen(
                blockchainType = BlockchainType.Monero,
                onCloseWithResult = { config ->
                    mainViewModel.setBirthdayHeightConfig(config)
                    navController.popBackStack()
                },
                onCloseClick = {
                    mainViewModel.cancelBirthdayHeightConfig = true
                    navController.popBackStack()
                }
            )
        }
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
            openSelectCoins.invoke()
            viewModel.onSelectCoinsShown()

            stat(page = statPage, event = StatEvent.Open(StatPage.RestoreSelect))
        }
    }

    LaunchedEffect(uiState.showBackupItems) {
        if (uiState.showBackupItems) {
            keyboardController?.hide()
            delay(300)
            openBackupItems.invoke()
            viewModel.onBackupItemsShown()
        }
    }

    HSScaffold(
        title = stringResource(R.string.ImportBackupFile_EnterPassword),
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = onBackClick
            )
        )
    ) {
        Column {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {

                InfoText(text = stringResource(R.string.ImportBackupFile_EnterPassword_Description))
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
                ButtonPrimaryYellowWithSpinner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.Button_Restore),
                    showSpinner = uiState.showButtonSpinner,
                    enabled = uiState.showButtonSpinner.not(),
                    onClick = {
                        viewModel.onImportClick()
                    },
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
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    HSScaffold(
        title = stringResource(R.string.BackupManager_BаckupFile),
        onBack = onBackClick,
        bottomBar = {
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.BackupManager_Restore),
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
                InfoText(
                    text = stringResource(R.string.BackupManager_BackupFileContents),
                    paddingBottom = 32.dp
                )
            }

            if (walletBackupViewItems.isNotEmpty()) {
                item {
                    HeaderText(text = stringResource(id = R.string.BackupManager_Wallets))
                    CellUniversalLawrenceSection(
                        items = walletBackupViewItems,
                        showFrame = true
                    ) { walletBackupViewItem ->
                        RowUniversal(
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {

                            Column(modifier = Modifier.weight(1f)) {
                                headline2_leah(text = walletBackupViewItem.name)
                                if (walletBackupViewItem.backupRequired) {
                                    subhead2_lucian(text = stringResource(id = R.string.BackupManager_BackupRequired))
                                } else {
                                    subhead2_grey(
                                        text = walletBackupViewItem.type,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                    VSpacer(height = 24.dp)
                }
            }

            item {
                OtherBackupItems(otherBackupViewItems)
                VSpacer(height = 32.dp)
            }
        }
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                },
                sheetState = sheetState,
                containerColor = ComposeAppTheme.colors.transparent
            ) {
                ConfirmationBottomSheet(
                    title = stringResource(R.string.BackupManager_MergeTitle),
                    text = stringResource(R.string.BackupManager_MergeDescription),
                    iconPainter = painterResource(R.drawable.icon_warning_2_20),
                    iconTint = ColorFilter.tint(ComposeAppTheme.colors.lucian),
                    confirmText = stringResource(R.string.BackupManager_MergeButton),
                    cautionType = Caution.Type.Error,
                    cancelText = stringResource(R.string.Button_Cancel),
                    onConfirm = {
                        viewModel.restoreFullBackup()
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                    },
                    onClose = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                    }
                )
            }
        }
    }
}
