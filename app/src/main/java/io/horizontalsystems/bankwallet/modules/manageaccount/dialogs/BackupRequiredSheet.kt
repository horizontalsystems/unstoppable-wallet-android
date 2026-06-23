package io.horizontalsystems.bankwallet.modules.manageaccount.dialogs

import android.os.Parcelable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.backuplocal.BackupLocalPage
import io.horizontalsystems.bankwallet.modules.manageaccount.backupkey.BackupKeyPage
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.extensions.HSBottomSheet
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetTextBlock
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellGroup
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class BackupRequiredSheet(val input: Input) : HSBottomSheet() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        BackupRequiredScreen(navigation, input.account)
    }

    @Serializable
    @Parcelize
    data class Input(val account: Account) : Parcelable
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRequiredScreen(navigation: HSNavigation, account: Account) {
    ComposeAppTheme {
        BottomSheetContent(
            onDismissRequest = navigation::removeLastOrNull,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            buttons = {
                HSButton(
                    title = stringResource(R.string.BackupRequired_RemindLater),
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Secondary,
                    onClick = navigation::removeLastOrNull
                )
            },
            content = {
                BottomSheetHeaderV3(
                    image72 = painterResource(R.drawable.warning_filled_24),
                    imageTint = ComposeAppTheme.colors.jacob,
                    title = stringResource(R.string.BackupRequired_Title)
                )
                BottomSheetTextBlock(stringResource(R.string.BackupRequired_Description))
                VSpacer(8.dp)
                CellGroup(paddingValues = PaddingValues(horizontal = 16.dp)) {
                    CellPrimary(
                        left = {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(R.drawable.ic_edit_24px),
                                contentDescription = null,
                                tint = ComposeAppTheme.colors.jacob
                            )
                        },
                        middle = {
                            CellMiddleInfo(
                                title = stringResource(R.string.BackupRecoveryPhrase_ManualBackup).hs,
                                subtitle = stringResource(R.string.BackupRequired_ManualBackupDescription).hs
                            )
                        },
                        right = { CellRightNavigation() },
                        onClick = {
                            navigation.slideFromBottom(
                                BackupKeyPage(account)
                            )

                            stat(
                                page = StatPage.BackupRequired,
                                event = StatEvent.Open(StatPage.ManualBackup)
                            )
                        }
                    )
                    HsDivider()
                    CellPrimary(
                        left = {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(R.drawable.ic_file_24),
                                contentDescription = null,
                                tint = ComposeAppTheme.colors.jacob
                            )
                        },
                        middle = {
                            CellMiddleInfo(
                                title = stringResource(R.string.BackupRecoveryPhrase_LocalBackup).hs,
                                subtitle = stringResource(R.string.BackupRequired_LocalBackupDescription).hs
                            )
                        },
                        right = { CellRightNavigation() },
                        onClick = {
                            navigation.slideFromBottom(BackupLocalPage(account))

                            stat(
                                page = StatPage.BackupRequired,
                                event = StatEvent.Open(StatPage.FileBackup)
                            )
                        }
                    )
                }
                VSpacer(16.dp)
            }
        )
    }
}
