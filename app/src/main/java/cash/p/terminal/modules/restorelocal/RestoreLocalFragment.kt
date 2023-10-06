package cash.p.terminal.modules.restorelocal

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.composablePage
import cash.p.terminal.core.composablePopup
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.manageaccounts.ManageAccountsModule
import cash.p.terminal.modules.restoreaccount.RestoreViewModel
import cash.p.terminal.modules.restoreaccount.restoreblockchains.ManageWalletsScreen
import cash.p.terminal.modules.zcashconfigure.ZcashConfigureScreen
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellowWithSpinner
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.FormsInputPassword
import cash.p.terminal.ui.compose.components.HeaderText
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.body_leah
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.compose.components.subhead2_lucian
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RestoreLocalFragment : BaseComposeFragment() {
    companion object {
        const val jsonFileKey = "jsonFileKey"
        const val fileNameKey = "fileNameKey"
    }

    @Composable
    override fun GetContent() {
        val popUpToInclusiveId =
            arguments?.getInt(ManageAccountsModule.popOffOnSuccessKey, R.id.restoreAccountFragment) ?: R.id.restoreAccountFragment

        val popUpInclusive =
            arguments?.getBoolean(ManageAccountsModule.popOffInclusiveKey) ?: false

        val backupJsonString = arguments?.getString(jsonFileKey)
        val fileName = arguments?.getString(fileNameKey)

        ComposeAppTheme {
            RestoreLocalNavHost(
                backupJsonString,
                fileName,
                findNavController(),
                popUpToInclusiveId,
                popUpInclusive
            )
        }
    }

}

@Composable
private fun RestoreLocalNavHost(
    backupJsonString: String?,
    fileName: String?,
    fragmentNavController: NavController,
    popUpToInclusiveId: Int,
    popUpInclusive: Boolean
) {
    val navController = rememberNavController()
    val mainViewModel: RestoreViewModel = viewModel()
    val viewModel = viewModel<RestoreLocalViewModel>(factory = RestoreLocalModule.Factory(backupJsonString, fileName))
    NavHost(
        navController = navController,
        startDestination = "restore_local",
    ) {
        composable("restore_local") {
            RestoreLocalScreen(
                viewModel = viewModel,
                mainViewModel = mainViewModel,
                onBackClick = { fragmentNavController.popBackStack() },
                close = { fragmentNavController.popBackStack(popUpToInclusiveId, popUpInclusive) },
                openSelectCoins = { navController.navigate("restore_select_coins") },
                openBackupItems = { navController.navigate("backup_file") }
            )
        }
        composablePage("backup_file") {
            BackupFileItems(
                viewModel,
                onNextClick = { viewModel.restoreFullBackup() },
                onBackClick = { navController.popBackStack() },
                close = { fragmentNavController.popBackStack(popUpToInclusiveId, popUpInclusive) }
            )
        }
        composablePage("restore_select_coins") {
            ManageWalletsScreen(
                mainViewModel = mainViewModel,
                openZCashConfigure = { navController.navigate("zcash_configure") },
                onBackClick = { navController.popBackStack() }
            ) { fragmentNavController.popBackStack(popUpToInclusiveId, popUpInclusive) }
        }
        composablePopup("zcash_configure") {
            ZcashConfigureScreen(
                onCloseWithResult = { config ->
                    mainViewModel.setZCashConfig(config)
                    navController.popBackStack()
                },
                onCloseClick = {
                    mainViewModel.cancelZCashConfig = true
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
private fun BackupFileItems(
    viewModel: RestoreLocalViewModel,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    close: () -> Unit
) {
    val uiState = viewModel.uiState
    val backupItems = viewModel.uiState.backupItems ?: return
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
        }
    }

    LaunchedEffect(uiState.parseError) {
        uiState.parseError?.let { error ->
            Toast.makeText(App.instance, error.message ?: error.javaClass.simpleName, Toast.LENGTH_LONG).show()
            onBackClick.invoke()
        }
    }

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = stringResource(R.string.BackupManager_BаckupFile),
                    navigationIcon = {
                        HsBackButton(onClick = onBackClick)
                    },
                )
            },
            bottomBar = {
                ButtonsGroupWithShade {
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp),
                        title = stringResource(R.string.BackupManager_Restore),
                        onClick = {
                            onNextClick()
                        }
                    )
                }
            }
        ) {
            LazyColumn(modifier = Modifier.padding(it)) {
                item {
                    InfoText(text = stringResource(R.string.BackupManager_BackupFileContents), paddingBottom = 32.dp)
                }

                item {
                    HeaderText(text = stringResource(id = R.string.BackupManager_Wallets))
                    CellUniversalLawrenceSection(items = backupItems.accounts, showFrame = true) { account ->
                        RowUniversal(
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {

                            Column(modifier = Modifier.weight(1f)) {
                                body_leah(text = account.name)
                                if (!account.hasAnyBackup) {
                                    subhead2_lucian(text = stringResource(id = R.string.BackupManager_BackupRequired))
                                } else {
                                    subhead2_grey(
                                        text = account.type.detailedDescription,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    VSpacer(height = 24.dp)
                    HeaderText(text = stringResource(id = R.string.BackupManager_Other))
                    CellUniversalLawrenceSection(items = backupItems.others, showFrame = true) { item ->
                        RowUniversal(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                body_leah(text = item.title)
                                subhead2_grey(
                                    text = item.subtitle,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    VSpacer(height = 32.dp)
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RestoreLocalScreen(
    viewModel: RestoreLocalViewModel,
    mainViewModel: RestoreViewModel,
    onBackClick: () -> Unit,
    close: () -> Unit,
    openSelectCoins: () -> Unit,
    openBackupItems: () -> Unit
) {
    val uiState = viewModel.uiState
    var hidePassphrase by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
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
        }
    }

    LaunchedEffect(uiState.parseError) {
        uiState.parseError?.let { error ->
            Toast.makeText(App.instance, error.message ?: error.javaClass.simpleName, Toast.LENGTH_LONG).show()
            onBackClick.invoke()
        }
    }

    uiState.showSelectCoins?.let { accountType ->
        mainViewModel.setAccountData(accountType, viewModel.accountName, uiState.manualBackup, true)
        val keyboardController = LocalSoftwareKeyboardController.current
        coroutineScope.launch {
            keyboardController?.hide()
            delay(300)
            openSelectCoins.invoke()
            viewModel.onSelectCoinsShown()
        }
    }

    if (uiState.showBackupItems) {
        val keyboardController = LocalSoftwareKeyboardController.current
        coroutineScope.launch {
            keyboardController?.hide()
            delay(300)
            openBackupItems.invoke()
            viewModel.onBackupItemsShown()
        }
    }

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = stringResource(R.string.ImportBackupFile_EnterPassword),
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = onBackClick
                        )
                    )
                )
            }
        ) {
            Column(modifier = Modifier.padding(it)) {
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
}