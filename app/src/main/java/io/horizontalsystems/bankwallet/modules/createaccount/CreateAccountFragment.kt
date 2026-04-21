package io.horizontalsystems.bankwallet.modules.createaccount

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.slideFromRightForResult
import io.horizontalsystems.bankwallet.modules.manageaccounts.PassKeyTermsFragment
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
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

class CreateAccountFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<ManageAccountsModule.Input>()
        CreateAccountScreen(navController, input)
    }
}


@Composable
fun CreateAccountScreen(navController: NavController, input: ManageAccountsModule.Input?) {
    HSScaffold(
        title = stringResource(R.string.ManageAccounts_CreateNewWallet),
        onBack = navController::popBackStack
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
                    navController.slideFromRight(R.id.createAccountStandardFragment, input)
                }
                WalletType(
                    icon = painterResource(R.drawable.touchid_24),
                    title = stringResource(R.string.CreateNewWallet_Passkey).hs,
                    subtitle = stringResource(R.string.CreateNewWallet_Passkey_Description).hs,
                    borderTop = true
                ) {
                    if (!App.localStorage.passkeyTermsAccepted) {
                        navController.slideFromRightForResult<PassKeyTermsFragment.Result>(R.id.passkeyTermsFragment) { result ->
                            if (result.termsAccepted) {
                                navController.slideFromRight(R.id.createAccountPasskeyFragment, input)
                            }
                        }
                    } else {
                        navController.slideFromRight(R.id.createAccountPasskeyFragment, input)
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
        CreateAccountScreen(NavController(LocalContext.current), null)
    }
}
