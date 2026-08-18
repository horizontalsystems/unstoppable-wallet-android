package io.horizontalsystems.walletkit.modules.importwallet

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.NoCredentialException
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.Caution
import io.horizontalsystems.walletkit.core.launchSafe
import io.horizontalsystems.walletkit.core.NavigationType
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.backuplocal.fullbackup.BackupFileValidator
import io.horizontalsystems.walletkit.modules.contacts.screen.ConfirmationBottomSheet
import io.horizontalsystems.walletkit.modules.createaccount.PasskeyProviderAttentionSheet
import io.horizontalsystems.walletkit.modules.createaccount.RestorePasskeyNotSupportedSheet
import io.horizontalsystems.walletkit.modules.createaccount.WalletType
import io.horizontalsystems.walletkit.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.restoreaccount.RestoreAccountPage
import io.horizontalsystems.walletkit.modules.restoreaccount.RestoreFromPasskeyPage
import io.horizontalsystems.walletkit.modules.restoreaccount.RestoreFromPrivateKeyPage
import io.horizontalsystems.walletkit.modules.restorelocal.RestoreLocalPage
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.Section
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.File
import kotlin.reflect.KClass


@Serializable
data class ImportWalletPage(val input: ManageAccountsModule.Input? = null) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val popUpToInclusiveId = input?.popOffOnSuccess ?: ImportWalletPage::class
        val inclusive = input?.popOffInclusive ?: true

        ImportWalletScreen(navigation, popUpToInclusiveId, inclusive)
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportWalletScreen(
    navigation: HSNavigation,
    popUpToInclusiveId: KClass<out HSPage>,
    inclusive: Boolean
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { uriNonNull ->
            // openInputStream itself can throw (picked file deleted, cloud
            // provider failed to stream) — keep it inside the try.
            try {
                context.contentResolver.openInputStream(uriNonNull)?.use { inputStream ->
                    inputStream.bufferedReader().use { br ->
                        val jsonString = br.readText()
                        //validate json format
                        BackupFileValidator().validate(jsonString)

                        val fileName = context.getFileName(uriNonNull)
                        navigation.navigateWithTermsAccepted(
                            screen = RestoreLocalPage(
                                RestoreLocalPage.Input(
                                    popUpToInclusiveId,
                                    inclusive,
                                    jsonString,
                                    fileName,
                                    StatPage.ImportWalletFromFiles
                                )
                            ),
                            navigationType = NavigationType.SlideFromBottom,
                            statPageFrom = StatPage.ImportWallet,
                            statPageTo = StatPage.ImportWalletFromFiles
                        )
                    }
                }
            } catch (e: Throwable) {
                Log.e("TAG", "ImportWalletScreen: ", e)
                //show json parsing error
                scope.launch {
                    delay(300)
                    showBottomSheet = true
                }
            }
        }
    }

    var error: String? by remember { mutableStateOf(null) }
    LaunchedEffect(error) {
        error?.let {
            HudHelper.showErrorMessage(view, it)
            error = null
        }
    }

    HSScaffold(
        title = stringResource(R.string.ManageAccounts_ImportWallet),
        onBack = navigation::removeLastOrNull,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            VSpacer(16.dp)

            Section {
                WalletType(
                    icon = painterResource(R.drawable.ic_edit_24),
                    title = stringResource(R.string.ImportWallet_RecoveryPhrase).hs,
                    subtitle = stringResource(R.string.ImportWallet_RecoveryPhrase_Description).hs,
                    borderTop = false
                ) {
                    navigation.navigateWithTermsAccepted(
                        screen = RestoreAccountPage(ManageAccountsModule.Input(popUpToInclusiveId, inclusive)),
                        navigationType = NavigationType.SlideFromRight,
                        statPageFrom = StatPage.ImportWallet,
                        statPageTo = StatPage.ImportWalletFromKey
                    )
                }
                WalletType(
                    icon = painterResource(R.drawable.key_24),
                    title = stringResource(R.string.ImportWallet_PrivateKey).hs,
                    subtitle = stringResource(R.string.ImportWallet_PrivateKey_Description).hs,
                    borderTop = true
                ) {
                    navigation.navigateWithTermsAccepted(
                        screen = RestoreFromPrivateKeyPage(ManageAccountsModule.Input(popUpToInclusiveId, inclusive)),
                        navigationType = NavigationType.SlideFromRight,
                        statPageFrom = StatPage.ImportWallet,
                        statPageTo = StatPage.ImportWalletFromPrivateKey
                    )
                }
                var passkeyEnabled by remember { mutableStateOf(true) }
                WalletType(
                    icon = painterResource(R.drawable.touchid_24),
                    title = stringResource(R.string.ImportWallet_Passkey).hs,
                    subtitle = stringResource(R.string.ImportWallet_Passkey_Description).hs,
                    borderTop = true
                ) {
                    if (!passkeyEnabled) return@WalletType
                    passkeyEnabled = false
                    scope.launch {
                        try {
                            val (entropy, accountName) = App.passkeyManager.authenticate(context)
                            navigation.slideFromRight(
                                RestoreFromPasskeyPage(
                                    RestoreFromPasskeyPage.Input(
                                        popUpToInclusiveId,
                                        inclusive,
                                        entropy,
                                        accountName,
                                    )
                                ),
                            )
                            stat(
                                page = StatPage.ImportWallet,
                                event = StatEvent.Open(StatPage.ImportWalletFromPasskey)
                            )
                        } catch (e: GetCredentialCancellationException) {
                        } catch (e: NoCredentialException) {
                            navigation.slideFromBottom(RestorePasskeyNotSupportedSheet)
                        } catch (e: GetCredentialCustomException) {
                            // the provider refused with its own internal state
                            // (locked vault, pending verification) — instructions,
                            // not an error
                            navigation.slideFromBottom(
                                PasskeyProviderAttentionSheet(PasskeyProviderAttentionSheet.isGpmPassphrase(e))
                            )
                        } catch (e: Throwable) {
                            error = e.message ?: e.javaClass.simpleName
                        } finally {
                            passkeyEnabled = true
                        }
                    }
                }
                WalletType(
                    icon = painterResource(R.drawable.ic_download_24),
                    title = stringResource(R.string.ImportWallet_BackupFile).hs,
                    subtitle = stringResource(R.string.ImportWallet_BackupFile_Description).hs,
                    borderTop = true
                ) {
                    restoreLauncher.launchSafe(arrayOf("application/json"), view)
                }
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
                    title = stringResource(R.string.ImportWallet_WarningInvalidJson),
                    text = stringResource(R.string.ImportWallet_WarningInvalidJsonDescription),
                    iconPainter = painterResource(R.drawable.icon_warning_2_20),
                    iconTint = ColorFilter.tint(ComposeAppTheme.colors.lucian),
                    confirmText = stringResource(R.string.ImportWallet_SelectAnotherFile),
                    cautionType = Caution.Type.Warning,
                    cancelText = stringResource(R.string.Button_Cancel),
                    onConfirm = {
                        restoreLauncher.launchSafe(arrayOf("application/json"), view)
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