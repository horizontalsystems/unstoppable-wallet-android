package io.horizontalsystems.walletkit.modules.unlinkaccount

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.stats.StatEntity
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
import io.horizontalsystems.walletkit.ui.extensions.HSBottomSheet
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellSecondary
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.walletkit.uiv3.components.controls.HSButton
import io.horizontalsystems.walletkit.uiv3.components.controls.HSSelector
import io.horizontalsystems.walletkit.uiv3.components.info.TextBlock
import kotlinx.serialization.Serializable

@Serializable
data class UnlinkAccountSheet(val account: Account) : HSBottomSheet() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        UnlinkAccountScreen(navigation, account)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnlinkAccountScreen(navigation: HSNavigation, account: Account) {
    val viewModel =
        viewModel<UnlinkAccountViewModel>(factory = UnlinkAccountModule.Factory(account))

    val confirmations = viewModel.confirmations
    val unlinkEnabled = viewModel.unlinkEnabled
    val showDeleteWarning = viewModel.showDeleteWarning

    val view = LocalView.current
    val doneConfirmationMessage = stringResource(R.string.Hud_Text_Done)

    BottomSheetContent(
        onDismissRequest = {
            navigation.removeLastOrNull()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        buttons = {
            HSButton(
                title = stringResource(viewModel.deleteButtonText),
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Secondary,
                enabled = unlinkEnabled,
                onClick = {
                    viewModel.onUnlink()
                    HudHelper.showSuccessMessage(view, doneConfirmationMessage)
                    navigation.removeLastOrNull()

                    stat(page = StatPage.UnlinkWallet, event = StatEvent.Delete(StatEntity.Wallet))
                }
            )
        },
        content = {
            BottomSheetHeaderV3(
                image72 = painterResource(R.drawable.trash_filled_24),
                imageTint = ComposeAppTheme.colors.lucian,
                title = stringResource(R.string.ManageKeys_Delete_Title)
            )
            if (showDeleteWarning) {
                TextBlock(
                    text = stringResource(id = R.string.ManageAccount_DeleteWarning),
                    textAlign = TextAlign.Center
                )
            }
            if (confirmations.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(16.dp))
                        .padding(vertical = 8.dp)
                ) {
                    confirmations.forEachIndexed { index, item ->
                        CellSecondary(
                            left = {
                                HSSelector(
                                    checked = item.confirmed,
                                    onCheckedChange = {
                                        viewModel.toggleConfirm(item)
                                    },
                                )
                            },
                            middle = {
                                CellMiddleInfo(subtitle = item.confirmationType.title.getString().hs)
                            },
                            onClick = {
                                viewModel.toggleConfirm(item)
                            }
                        )
                        if(index < confirmations.size - 1) {
                            HsDivider()
                        }
                    }
                }
            }
        }
    )
}
