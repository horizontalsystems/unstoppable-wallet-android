package io.horizontalsystems.bankwallet.modules.restorelocal

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Caution
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.OtherBackupItems
import io.horizontalsystems.bankwallet.modules.contacts.screen.ConfirmationBottomSheet
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.removeLastUntil
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_lucian
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class RestoreLocalBackupFile(
    val popOffOnSuccess: KClass<out HSScreen>,
    val popOffInclusive: Boolean,
) : RestoreLocalChildScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>
    ) {
        val viewModel = viewModel<RestoreLocalViewModel>()
        val activity = LocalActivity.current
        BackupFileItems(
            viewModel,
            onBackClick = { backStack.removeLastOrNull() },
            close = {
                backStack.removeLastUntil(popOffOnSuccess, popOffInclusive)
            },
            reloadApp = { activity?.let { MainModule.startAsNewTask(it) } }
        )
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
            BottomSheetContent(
                onDismissRequest = {
                    showBottomSheet = false
                },
                sheetState = sheetState
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