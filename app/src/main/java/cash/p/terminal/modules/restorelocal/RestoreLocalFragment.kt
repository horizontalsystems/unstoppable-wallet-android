package cash.p.terminal.modules.restorelocal

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellowWithSpinner
import cash.p.terminal.ui.compose.components.FormsInputPassword
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.VSpacer
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
    NavHost(
        navController = navController,
        startDestination = "restore_local",
    ) {
        composable("restore_local") {
            RestoreLocalScreen(
                backupJsonString = backupJsonString,
                fileName = fileName,
                mainViewModel = mainViewModel,
                onBackClick = { fragmentNavController.popBackStack() },
                close = { fragmentNavController.popBackStack(popUpToInclusiveId, popUpInclusive) },
            ) { navController.navigate("restore_select_coins") }
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RestoreLocalScreen(
    backupJsonString: String?,
    fileName: String?,
    mainViewModel: RestoreViewModel,
    onBackClick: () -> Unit,
    close: () -> Unit,
    openSelectCoins: () -> Unit,
) {
    val viewModel = viewModel<RestoreLocalViewModel>(factory = RestoreLocalModule.Factory(backupJsonString, fileName))
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

    uiState.accountType?.let { accountType ->
        mainViewModel.setAccountData(accountType, viewModel.accountName, uiState.manualBackup, true)
        val keyboardController = LocalSoftwareKeyboardController.current
        coroutineScope.launch {
            keyboardController?.hide()
            delay(300)
            openSelectCoins.invoke()
            viewModel.onSelectCoinsShown()
        }
    }

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.ImportBackupFile_EnterPassword),
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