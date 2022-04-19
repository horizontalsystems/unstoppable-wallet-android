package io.horizontalsystems.bankwallet.modules.unlinkaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryRed
import io.horizontalsystems.bankwallet.ui.compose.components.CellCheckboxLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.HsCheckbox
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class UnlinkAccountDialog : BaseComposableBottomSheetFragment() {
    private val account by lazy { requireArguments().getParcelable<Account>(ACCOUNT) }

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
                ComposeAppTheme {
                    UnlinkAccountScreen(findNavController(), account!!)
                }
            }
        }
    }

    companion object {
        private const val ACCOUNT = "account"

        fun prepareParams(account: Account) = bundleOf(ACCOUNT to account)
    }
}

@Composable
private fun UnlinkAccountScreen(navController: NavController, account: Account) {
    val viewModel = viewModel<UnlinkAccountViewModel>(factory = UnlinkAccountModule.Factory(account))

    val confirmations = viewModel.confirmations
    val unlinkEnabled = viewModel.unlinkEnabled
    val accountName = viewModel.accountName
    val showDeleteWarning = viewModel.showDeleteWarning

    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_attention_red_24),
        title = stringResource(R.string.ManageKeys_Delete_Title),
        subtitle = accountName,
        onCloseClick = {
            navController.popBackStack()
        }
    ) {
        Divider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10
        )

        confirmations.forEach { item ->
            CellCheckboxLawrence(
                borderBottom = true,
                onClick = { viewModel.toggleConfirm(item) }
            ) {
                HsCheckbox(
                    checked = item.confirmed,
                    onCheckedChange = {
                        viewModel.toggleConfirm(item)
                    },
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = item.confirmationType.title.getString(),
                    style = ComposeAppTheme.typography.subhead2,
                    color = ComposeAppTheme.colors.leah
                )
            }
        }

        if (showDeleteWarning) {
            TextImportantWarning(
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp),
                text = stringResource(id = R.string.ManageAccount_DeleteWarning)
            )
        }

        val view = LocalView.current
        val doneConfirmationMessage = stringResource(R.string.Hud_Text_Done)

        ButtonPrimaryRed(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            title = stringResource(viewModel.deleteButtonText),
            onClick = {
                viewModel.onUnlink()
                HudHelper.showSuccessMessage(view, doneConfirmationMessage)
                navController.popBackStack()
            },
            enabled = unlinkEnabled
        )
    }
}

