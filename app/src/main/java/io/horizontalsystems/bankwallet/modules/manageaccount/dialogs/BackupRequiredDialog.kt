package io.horizontalsystems.bankwallet.modules.manageaccount.dialogs

import android.os.Parcelable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.backuplocal.BackupLocalPage
import io.horizontalsystems.bankwallet.modules.manageaccount.backupkey.BackupKeyPage
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheet
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonStyle
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class BackupRequiredDialog(val input: Input) : BaseComposableBottomSheet() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        BackupRequiredScreen(navController, input.account, input.text)
    }

    @Serializable
    @Parcelize
    data class Input(val account: Account, val text: String) : Parcelable
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRequiredScreen(navController: HSNavigation, account: Account, text: String) {
    ComposeAppTheme {
        BottomSheetContent(
            onDismissRequest = navController::removeLastOrNull,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            buttons = {
                HSButton(
                    title = stringResource(R.string.BackupRecoveryPhrase_ManualBackup),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        navController.slideFromBottom(
                            BackupKeyPage(account)
                        )

                        stat(
                            page = StatPage.BackupRequired,
                            event = StatEvent.Open(StatPage.ManualBackup)
                        )
                    }
                )
                HSButton(
                    title = stringResource(R.string.BackupRecoveryPhrase_LocalBackup),
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Secondary,
                    onClick = {
                        navController.slideFromBottom(BackupLocalPage(account))

                        stat(
                            page = StatPage.BackupRequired,
                            event = StatEvent.Open(StatPage.FileBackup)
                        )
                    }
                )
                HSButton(
                    title = stringResource(R.string.BackupRecoveryPhrase_Later),
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.Transparent,
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Medium,
                    onClick = navController::removeLastOrNull
                )
            },
            content = {
                BottomSheetHeaderV3(
                    image72 = painterResource(R.drawable.warning_filled_24),
                    imageTint = ComposeAppTheme.colors.jacob,
                    title = stringResource(R.string.BackupManager_BackupRequired)
                )
                TextBlock(text = text, textAlign = TextAlign.Center)
            }
        )
    }
}
