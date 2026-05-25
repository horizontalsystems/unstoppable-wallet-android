package io.horizontalsystems.bankwallet.modules.createaccount

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.manageaccounts.PassKeyTermsPage
import io.horizontalsystems.bankwallet.modules.nav3.EntryScreen
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.BoxBordered
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.Section
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellLeftImage
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.bankwallet.uiv3.components.cell.HSString
import io.horizontalsystems.bankwallet.uiv3.components.cell.ImageType
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import kotlinx.serialization.Serializable

@Serializable
data class CreateAccountPage(val input: ManageAccountsModule.Input? = null) : HSPage() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        CreateAccountScreen(navController, input)
    }
}


@Composable
fun CreateAccountScreen(navController: HSNavigation, input: ManageAccountsModule.Input?) {
    HSScaffold(
        title = stringResource(R.string.ManageAccounts_CreateNewWallet),
        onBack = navController::removeLastOrNull
    ) {
        Column {
            VSpacer(16.dp)

            Section {
                WalletType(
                    icon = painterResource(R.drawable.list_24),
                    title = stringResource(R.string.CreateNewWallet_Standard).hs,
                    subtitle = stringResource(R.string.CreateNewWallet_Standard_Description).hs,
                    borderTop = false
                ) {
                    navController.slideFromRight(CreateAccountStandardPage(input))
                }

                WalletType(
                    icon = painterResource(R.drawable.touchid_24),
                    title = stringResource(R.string.CreateNewWallet_Passkey).hs,
                    subtitle = stringResource(R.string.CreateNewWallet_Passkey_Description).hs,
                    borderTop = true
                ) {
                    if (!App.localStorage.passkeyTermsAccepted) {
                        navController.slideFromRight(PassKeyTermsPage(CreateAccountPasskeyPage(input)))
                    } else {
                        navController.slideFromRight(CreateAccountPasskeyPage(input))
                    }
                }
            }

        }
    }
}

@Composable
fun WalletType(
    icon: Painter,
    title: HSString,
    subtitle: HSString,
    borderTop: Boolean,
    onClick: () -> Unit
) {
    BoxBordered(top = borderTop) {
        CellPrimary(
            left = {
                CellLeftImage(
                    painter = icon,
                    type = ImageType.Rectangle,
                    size = 24
                )
            },
            middle = {
                CellMiddleInfo(
                    title = title,
                    subtitle = subtitle,
                )
            },
            right = {
                CellRightNavigation()
            },
            onClick = onClick
        )
    }
}

@Composable
@Preview
fun Preview_CreateAccountScreen() {
    ComposeAppTheme {
        CreateAccountScreen(HSNavigation(NavBackStack(EntryScreen)), null)
    }
}
