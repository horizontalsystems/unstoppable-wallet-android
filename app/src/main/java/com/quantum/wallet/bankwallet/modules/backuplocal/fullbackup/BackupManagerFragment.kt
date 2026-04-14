package com.quantum.wallet.bankwallet.modules.backuplocal.fullbackup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.Caution
import com.quantum.wallet.bankwallet.core.authorizedAction
import com.quantum.wallet.bankwallet.core.navigateWithTermsAccepted
import com.quantum.wallet.bankwallet.core.slideFromBottom
import com.quantum.wallet.bankwallet.core.slideFromRight
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.modules.contacts.screen.ConfirmationBottomSheet
import com.quantum.wallet.bankwallet.modules.importwallet.getFileName
import com.quantum.wallet.bankwallet.modules.restorelocal.RestoreLocalFragment
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import com.quantum.wallet.bankwallet.ui.compose.components.RowUniversal
import com.quantum.wallet.bankwallet.ui.compose.components.body_jacob
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import com.quantum.wallet.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BackupManagerFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        BackupManagerScreen(
            onBackClick = {
                navController.popBackStack()
            },
            onRestoreBackup = { jsonString, fileName ->
                navController.navigateWithTermsAccepted {
                    navController.slideFromBottom(
                        R.id.restoreLocalFragment,
                        RestoreLocalFragment.Input(
                            R.id.backupManagerFragment,
                            false,
                            jsonString,
                            fileName,
                            StatPage.ImportFullFromFiles
                        )
                    )

                    stat(
                        page = StatPage.BackupManager,
                        event = StatEvent.Open(StatPage.ImportFullFromFiles)
                    )
                }
            },
            onCreateBackup = {
                navController.authorizedAction {
                    navController.slideFromRight(R.id.backupLocalFragment)

                    stat(
                        page = StatPage.BackupManager,
                        event = StatEvent.Open(StatPage.ExportFullToFiles)
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupManagerScreen(
    onBackClick: () -> Unit,
    onRestoreBackup: (jsonString: String, fileName: String?) -> Unit,
    onCreateBackup: () -> Unit,
) {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { uriNonNull ->
                context.contentResolver.openInputStream(uriNonNull)?.use { inputStream ->
                    try {
                        inputStream.bufferedReader().use { br ->
                            val jsonString = br.readText()
                            //validate json format
                            BackupFileValidator().validate(jsonString)

                            val fileName = context.getFileName(uriNonNull)
                            onRestoreBackup(jsonString, fileName)
                        }
                    } catch (e: Throwable) {
                        //show json parsing error
                        scope.launch {
                            delay(300)
                            showBottomSheet = true
                        }
                    }
                }
            }
        }

    HSScaffold(
        title = stringResource(R.string.BackupManager_Title),
        onBack = onBackClick,
    ) {
        Column {
            Spacer(modifier = Modifier.height(12.dp))
            CellUniversalLawrenceSection(
                buildList {
                    add {
                        RowUniversal(onClick = { restoreLauncher.launch(arrayOf("application/json")) }) {
                            Icon(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                painter = painterResource(R.drawable.ic_download_20),
                                contentDescription = null,
                                tint = ComposeAppTheme.colors.jacob
                            )
                            body_jacob(text = stringResource(R.string.BackupManager_RestoreBackup))
                        }
                    }

                    add {
                        RowUniversal(onClick = onCreateBackup) {
                            Icon(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                painter = painterResource(R.drawable.ic_plus),
                                contentDescription = null,
                                tint = ComposeAppTheme.colors.jacob
                            )
                            body_jacob(text = stringResource(R.string.BackupManager_CreateBackup))
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showBottomSheet) {
            BottomSheetContent(
                onDismissRequest = {
                    showBottomSheet = false
                },
                sheetState = sheetState
            ) {
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
                        scope.launch {
                            sheetState.hide()
                            showBottomSheet = false
                        }
                    },
                    onClose = {
                        scope.launch {
                            sheetState.hide()
                            showBottomSheet = false
                        }
                    }
                )
            }
        }
    }
}
