package io.horizontalsystems.bankwallet.modules.importwallet

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import com.google.gson.Gson
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.navigateWithTermsAccepted
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.backuplocal.BackupLocalModule
import io.horizontalsystems.bankwallet.modules.backuplocal.password.BackupLocalPasswordViewModel.*
import io.horizontalsystems.bankwallet.modules.contacts.screen.ConfirmationBottomSheet
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.restorelocal.RestoreLocalFragment
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File


class ImportWalletFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            val popUpToInclusiveId =
                arguments?.getInt(ManageAccountsModule.popOffOnSuccessKey, R.id.importWalletFragment) ?: R.id.importWalletFragment

            val inclusive =
                arguments?.getBoolean(ManageAccountsModule.popOffInclusiveKey) ?: true

            setContent {
                ImportWalletScreen(findNavController(), popUpToInclusiveId, inclusive)
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
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
                        val json = Gson().fromJson(jsonString, BackupLocalModule.WalletBackup::class.java)
                        //Gson will set field as null if the json file doesn't have the matching field
                        if (json.version == null || json.crypto == null){
                            throw Exception("Invalid json format")
                        }
                        navController.navigateWithTermsAccepted {
                            val fileName = context.getFileName(uriNonNull)
                            navController.slideFromBottom(
                                R.id.restoreLocalFragment,
                                bundleOf(
                                    ManageAccountsModule.popOffOnSuccessKey to popUpToInclusiveId,
                                    ManageAccountsModule.popOffInclusiveKey to inclusive,
                                    RestoreLocalFragment.jsonFileKey to jsonString,
                                    RestoreLocalFragment.fileNameKey to fileName
                                )
                            )
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

    ComposeAppTheme {
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
                        title = TranslatableString.ResString(R.string.ManageAccounts_ImportWallet),
                        menuItems = listOf(
                            MenuItem(
                                title = TranslatableString.ResString(R.string.Button_Close),
                                icon = R.drawable.ic_close,
                                onClick = { navController.popBackStack() }
                            )
                        )
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
                                    bundleOf(
                                        ManageAccountsModule.popOffOnSuccessKey to popUpToInclusiveId,
                                        ManageAccountsModule.popOffInclusiveKey to inclusive,
                                    )
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
                                bundleOf(
                                    ManageAccountsModule.popOffOnSuccessKey to popUpToInclusiveId,
                                    ManageAccountsModule.popOffInclusiveKey to inclusive,
                                )
                            )
                        }
                    )
                    VSpacer(12.dp)
                }
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

fun Context.getFileName(uri: Uri): String? = when(uri.scheme) {
    ContentResolver.SCHEME_CONTENT -> getContentFileName(uri)
    else -> uri.path?.let(::File)?.name
}

private fun Context.getContentFileName(uri: Uri): String? = runCatching {
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        cursor.moveToFirst()
        return@use cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME).let(cursor::getString)
    }
}.getOrNull()