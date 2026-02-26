package io.horizontalsystems.bankwallet.modules.walletconnect

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import kotlinx.serialization.Serializable

@Serializable
data class WCAccountTypeNotSupportedScreen(
    val accountTypeDescription: String
) : HSScreen(bottomSheet = true) {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        ComposeAppTheme {
            WCAccountTypeNotSupportedScreen(
                accountTypeDescription = accountTypeDescription,
                onCloseClick = {
                    backStack.removeLastOrNull()
                },
                onSwitchClick = {
                    backStack.removeLastOrNull()
//                    TODO("xxx nav3")
//                    navController.slideFromRight(
//                        R.id.manageAccountsFragment,
//                        ManageAccountsModule.Mode.Manage
//                    )
                }
            )
        }
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
