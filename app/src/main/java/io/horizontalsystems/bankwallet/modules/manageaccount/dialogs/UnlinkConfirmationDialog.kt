package io.horizontalsystems.bankwallet.modules.manageaccount.dialogs

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportant
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class UnlinkConfirmationDialog : BaseComposableBottomSheetFragment() {

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
                    BottomSheetScreen(findNavController(), account!!)
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
private fun BottomSheetScreen(navController: NavController, account: Account) {
    val viewModel = viewModel<UnlinkConfirmationDialogViewModel>(factory = UnlinkConfirmationDialogModule.Factory(account))

    val items by viewModel.itemsLiveData.observeAsState(listOf())
    val buttonEnabled by viewModel.buttonEnabledLiveData.observeAsState(false)
    val accountName = viewModel.accountName
    val message = viewModel.message

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

        items.forEachIndexed { index, item ->
            CellCheckboxLawrence(
                borderBottom = true,
                onClick = { viewModel.updateItem(index, item, !item.checked) }
            ) {
                HsCheckbox(
                    checked = item.checked,
                    onCheckedChange = { checked ->
                        viewModel.updateItem(index, item, checked)
                    },
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = item.text.getString(),
                    style = ComposeAppTheme.typography.subhead2,
                    color = ComposeAppTheme.colors.leah
                )
            }
        }

        message?.let {
            TextImportant(
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp),
                text = it.getString()
            )
        }

        val view = LocalView.current
        val doneConfirmationMessage = stringResource(R.string.Hud_Text_Done)

        ButtonPrimaryRed(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            title = stringResource(R.string.ManageKeys_Delete_FromPhone),
            onClick = {
                viewModel.onUnlinkConfirm()
                HudHelper.showSuccessMessage(view, doneConfirmationMessage)
                navController.popBackStack()
            },
            enabled = buttonEnabled
        )
    }
}

