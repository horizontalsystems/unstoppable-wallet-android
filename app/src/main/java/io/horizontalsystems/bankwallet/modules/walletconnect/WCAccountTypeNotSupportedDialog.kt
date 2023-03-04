package io.horizontalsystems.bankwallet.modules.walletconnect

import android.os.Bundle
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
import androidx.core.os.bundleOf
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController

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
                ComposeAppTheme {
                    WCAccountTypeNotSupportedScreen(
                        requireArguments().getString(ACCOUNT_TYPE_DESC) ?: "",
                        onCloseClick = { findNavController().popBackStack() },
                        onSwitchClick = {
                            findNavController().popBackStack()
                            findNavController().slideFromRight(
                                R.id.manageAccountsFragment,
                                bundleOf(ManageAccountsModule.MODE to ManageAccountsModule.Mode.Manage)
                            )
                        }
                    )
                }
            }
        }
    }

    companion object {
        private const val ACCOUNT_TYPE_DESC = "account_type_desc"

        fun prepareParams(accountTypeDescription: String) = bundleOf(
            ACCOUNT_TYPE_DESC to accountTypeDescription,
        )
    }
}

@Composable
fun WCAccountTypeNotSupportedScreen(
    accountTypeDescription: String,
    onCloseClick: () -> Unit,
    onSwitchClick: () -> Unit
) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_wallet_connect_24),
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
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
    ComposeAppTheme {
        WCAccountTypeNotSupportedScreen("Account Type Desc", {}, {})
    }
}
