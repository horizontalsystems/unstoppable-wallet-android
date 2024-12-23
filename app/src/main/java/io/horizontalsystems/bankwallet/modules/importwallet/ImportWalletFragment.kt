package io.horizontalsystems.bankwallet.modules.importwallet

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.Caution
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.navigateWithTermsAccepted
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.BackupFileValidator
import io.horizontalsystems.bankwallet.modules.contacts.screen.ConfirmationBottomSheet
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.restorelocal.RestoreLocalFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { uriNonNull ->
            context.contentResolver.openInputStream(uriNonNull)?.use { inputStream ->
                try {
                    inputStream.bufferedReader().use { br ->
                        val jsonString = br.readText()
                        //validate json format
                        BackupFileValidator().validate(jsonString)

                        navController.navigateWithTermsAccepted {
                            val fileName = context.getFileName(uriNonNull)
                            navController.slideFromBottom(
                                R.id.restoreLocalFragment,
                                RestoreLocalFragment.Input(
                                    popUpToInclusiveId,
                                    inclusive,
                                    jsonString,
                                    fileName,
                                    StatPage.ImportWalletFromFiles
                                )
                            )

                            stat(page = StatPage.ImportWallet, event = StatEvent.Open(StatPage.ImportWalletFromFiles))
                        }
                    }
                } catch (e: Throwable) {
                    Log.e("TAG", "ImportWalletScreen: ", e)
                    //show json parsing error
                    coroutineScope.launch {
                        delay(300)
                        bottomSheetState.show()
                    }
                }
            }
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
                    restoreLauncher.launch(arrayOf("application/json"))
                    coroutineScope.launch { bottomSheetState.hide() }
                },
                onClose = {
                    coroutineScope.launch { bottomSheetState.hide() }
                }
            )
        }
    ) {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
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

                            stat(page = StatPage.ImportWallet, event = StatEvent.Open(StatPage.ImportWalletFromKey))
                        }
                    }
                )
                VSpacer(12.dp)
                ImportOption(
                    title = stringResource(R.string.ImportWallet_BackupFile),
                    description = stringResource(R.string.ImportWallet_BackupFile_Description),
                    icon = R.drawable.ic_download_24,
                    onClick = {
                        restoreLauncher.launch(arrayOf("application/json"))
                    }
                )
                VSpacer(12.dp)
                ImportOption(
                    title = stringResource(R.string.ImportWallet_ExchangeWallet),
                    description = stringResource(R.string.ImportWallet_ExchangeWallet_Description),
                    icon = R.drawable.icon_link_24,
                    onClick = {
                        navController.slideFromBottom(
                            R.id.importCexAccountFragment,
                            ManageAccountsModule.Input(popUpToInclusiveId, inclusive)
                        )

                        stat(page = StatPage.ImportWallet, event = StatEvent.Open(StatPage.ImportWalletFromExchangeWallet))
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