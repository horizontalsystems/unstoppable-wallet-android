package cash.p.terminal.modules.importwallet

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material3.Scaffold
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.core.Caution
import cash.p.terminal.ui_compose.getInput
import cash.p.terminal.core.navigateWithTermsAccepted
import cash.p.terminal.navigation.openQrScanner
import cash.p.terminal.navigation.slideFromBottom
import cash.p.terminal.modules.contacts.screen.ConfirmationBottomSheet
import cash.p.terminal.modules.manageaccounts.ManageAccountsModule
import cash.p.terminal.modules.restorelocal.RestoreLocalFragment
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HudHelper
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.headline2_leah
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import java.io.File


class ImportWalletFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<ManageAccountsModule.Input>()
        val popUpToInclusiveId = input?.popOffOnSuccess ?: R.id.importWalletFragment
        val inclusive = input?.popOffInclusive ?: true

        ImportWalletScreen(navController, popUpToInclusiveId, inclusive)
    }

}

@Composable
private fun ImportWalletScreen(
    navController: NavController,
    popUpToInclusiveId: Int,
    inclusive: Boolean
) {
    val bottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
    val viewModel = koinViewModel<ImportWalletViewModel>()
    val scannerTitle = stringResource(R.string.ManageAccounts_ImportWallet)

    // Handle one-shot navigation events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is ImportWalletViewModel.NavigationEvent.OpenRestoreFromQr -> {
                    navController.navigateWithTermsAccepted {
                        navController.slideFromBottom(
                            R.id.restoreAccountFragment,
                            ManageAccountsModule.Input(
                                popOffOnSuccess = popUpToInclusiveId,
                                popOffInclusive = inclusive,
                                prefillWords = event.words,
                                prefillPassphrase = event.passphrase,
                                prefillMoneroHeight = event.moneroHeight
                            )
                        )
                    }
                }

                is ImportWalletViewModel.NavigationEvent.OpenRestoreLocal -> {
                    navController.navigateWithTermsAccepted {
                        navController.slideFromBottom(
                            R.id.restoreLocalFragment,
                            RestoreLocalFragment.Input(
                                popUpToInclusiveId,
                                inclusive,
                                event.backupFilePath,
                                event.fileName
                            )
                        )
                    }
                }
            }
        }
    }

    // Handle error from ViewModel
    viewModel.errorMessage?.let { error ->
        LaunchedEffect(error) {
            HudHelper.showErrorMessage(view, error)
            viewModel.onErrorShown()
        }
    }

    // Handle backup file error
    if (viewModel.backupFileError) {
        LaunchedEffect(Unit) {
            delay(300)
            bottomSheetState.show()
            viewModel.onBackupFileErrorShown()
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { uriNonNull ->
            val fileName = context.getFileName(uriNonNull)
            viewModel.processBackupFile(context.contentResolver, uriNonNull, fileName)
        }
    }

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetBackgroundColor = ComposeAppTheme.colors.transparent,
        sheetContent = {
            ConfirmationBottomSheet(
                title = stringResource(R.string.ImportWallet_WarningInvalidJson),
                text = stringResource(R.string.ImportWallet_WarningInvalidJsonDescription),
                iconPainter = painterResource(R.drawable.icon_warning_2_20),
                iconTint = ColorFilter.tint(ComposeAppTheme.colors.lucian),
                confirmText = stringResource(R.string.ImportWallet_SelectAnotherFile),
                cautionType = Caution.Type.Warning,
                cancelText = stringResource(R.string.Button_Cancel),
                onConfirm = {
                    restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                    coroutineScope.launch { bottomSheetState.hide() }
                },
                onClose = {
                    coroutineScope.launch { bottomSheetState.hide() }
                }
            )
        }
    ) {
        Scaffold(
            containerColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = stringResource(R.string.ManageAccounts_ImportWallet),
                    navigationIcon = { HsBackButton(onClick = { navController.popBackStack() }) }
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .padding(it)
                    .verticalScroll(rememberScrollState())
            ) {
                VSpacer(12.dp)
                ImportOption(
                    title = stringResource(R.string.ImportWallet_RecoveryPhrase),
                    description = stringResource(R.string.ImportWallet_RecoveryPhrase_Description),
                    icon = R.drawable.ic_edit_24,
                    onClick = {
                        navController.navigateWithTermsAccepted {
                            navController.slideFromBottom(
                                R.id.restoreAccountFragment,
                                ManageAccountsModule.Input(popUpToInclusiveId, inclusive)
                            )
                        }
                    }
                )
                VSpacer(12.dp)
                ImportOption(
                    title = stringResource(R.string.ImportWallet_BackupFile),
                    description = stringResource(R.string.ImportWallet_BackupFile_Description),
                    icon = R.drawable.ic_download_24,
                    onClick = {
                        restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                    }
                )
                VSpacer(12.dp)
                ImportOption(
                    title = stringResource(R.string.import_wallet_from_qr),
                    description = stringResource(R.string.import_wallet_from_qr_description),
                    icon = R.drawable.ic_qr_scan_24,
                    onClick = {
                        navController.openQrScanner(
                            title = scannerTitle,
                            showPasteButton = false
                        ) { scannedText ->
                            viewModel.handleScannedData(scannedText)
                        }
                    }
                )
                VSpacer(12.dp)
            }
        }
    }
}

@Composable
private fun ImportOption(
    title: String,
    description: String,
    icon: Int,
    onClick: () -> Unit
) {
    CellUniversalLawrenceSection {
        RowUniversal(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalPadding = 24.dp,
            onClick = onClick,
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
            HSpacer(16.dp)
            Column {
                headline2_leah(title)
                subhead2_grey(description)
            }
        }
    }
}

fun Context.getFileName(uri: Uri): String? = when (uri.scheme) {
    ContentResolver.SCHEME_CONTENT -> getContentFileName(uri)
    else -> uri.path?.let(::File)?.name
}

private fun Context.getContentFileName(uri: Uri): String? = runCatching {
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        cursor.moveToFirst()
        return@use cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME).let(cursor::getString)
    }
}.getOrNull()
