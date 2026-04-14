package com.quantum.wallet.bankwallet.modules.unlinkaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.requireInput
import com.quantum.wallet.bankwallet.core.stats.StatEntity
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.entities.Account
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.components.HsDivider
import com.quantum.wallet.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import com.quantum.wallet.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import com.quantum.wallet.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellMiddleInfo
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellSecondary
import com.quantum.wallet.bankwallet.uiv3.components.cell.hs
import com.quantum.wallet.bankwallet.uiv3.components.controls.ButtonVariant
import com.quantum.wallet.bankwallet.uiv3.components.controls.HSButton
import com.quantum.wallet.bankwallet.uiv3.components.controls.HSSelector
import com.quantum.wallet.bankwallet.uiv3.components.info.TextBlock
import com.quantum.wallet.core.findNavController
import com.quantum.wallet.core.helpers.HudHelper

class UnlinkAccountDialog : BaseComposableBottomSheetFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                val navController = findNavController()

                ComposeAppTheme {
                    UnlinkAccountScreen(navController, navController.requireInput())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnlinkAccountScreen(navController: NavController, account: Account) {
    val viewModel =
        viewModel<UnlinkAccountViewModel>(factory = UnlinkAccountModule.Factory(account))

    val confirmations = viewModel.confirmations
    val unlinkEnabled = viewModel.unlinkEnabled
    val showDeleteWarning = viewModel.showDeleteWarning

    val view = LocalView.current
    val doneConfirmationMessage = stringResource(R.string.Hud_Text_Done)

    BottomSheetContent(
        onDismissRequest = {
            navController.popBackStack()
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
                    navController.popBackStack()

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
