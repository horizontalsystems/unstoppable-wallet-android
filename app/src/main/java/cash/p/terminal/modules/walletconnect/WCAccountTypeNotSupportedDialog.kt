package cash.p.terminal.modules.walletconnect

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.ui_compose.getInput
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.modules.manageaccounts.ManageAccountsModule
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui.extensions.BaseComposableBottomSheetFragment
import cash.p.terminal.ui.extensions.BottomSheetHeader
import cash.p.terminal.ui_compose.findNavController
import kotlinx.parcelize.Parcelize

class WCAccountTypeNotSupportedDialog : BaseComposableBottomSheetFragment() {
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

                cash.p.terminal.ui_compose.theme.ComposeAppTheme {
                    WCAccountTypeNotSupportedScreen(
                        accountTypeDescription = navController.getInput<Input>()?.accountTypeDescription
                            ?: "",
                        onCloseClick = {
                            navController.popBackStack()
                        },
                        onSwitchClick = {
                            navController.popBackStack()
                            navController.slideFromRight(
                                R.id.manageAccountsFragment,
                                ManageAccountsModule.Mode.Manage
                            )
                        }
                    )
                }
            }
        }
    }

    @Parcelize
    data class Input(val accountTypeDescription: String) : Parcelable
}

@Composable
fun WCAccountTypeNotSupportedScreen(
    accountTypeDescription: String,
    onCloseClick: () -> Unit,
    onSwitchClick: () -> Unit
) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_wallet_connect_24),
        iconTint = ColorFilter.tint(cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.jacob),
        title = stringResource(R.string.WalletConnect_Title),
        onCloseClick = onCloseClick
    ) {
        TextImportantWarning(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            text = stringResource(id = R.string.WalletConnect_NotSupportedDescription, accountTypeDescription)
        )
        ButtonPrimaryYellow(
            modifier = Modifier
                .padding(vertical = 20.dp, horizontal = 24.dp)
                .fillMaxWidth(),
            title = stringResource(R.string.Button_Switch),
            onClick = onSwitchClick
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Preview
@Composable
private fun WalletConnectErrorWatchAccountPreview() {
    cash.p.terminal.ui_compose.theme.ComposeAppTheme {
        WCAccountTypeNotSupportedScreen("Account Type Desc", {}, {})
    }
}
