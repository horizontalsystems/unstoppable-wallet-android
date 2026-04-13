package io.horizontalsystems.bankwallet.modules.createaccount

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.BoxBordered
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.Section
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellLeftImage
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.bankwallet.uiv3.components.cell.ImageType
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs

class CreateAccountFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        CreateAccountScreen(navController)
    }
}


@Composable
fun CreateAccountScreen(navController: NavController) {
    HSScaffold(
        title = stringResource(R.string.ManageAccounts_CreateNewWallet),
        onBack = navController::popBackStack
    ) {
        Column {
            VSpacer(16.dp)

            Section {
                CellPrimary(
                    left = {
                        CellLeftImage(
                            painter = painterResource(R.drawable.list_24),
                            type = ImageType.Rectangle,
                            size = 24
                        )
                    },
                    middle = {
                        CellMiddleInfo(
                            title = stringResource(R.string.CreateNewWallet_Standard).hs,
                            subtitle = stringResource(R.string.CreateNewWallet_Standard_Description).hs,
                        )
                    },
                    right = {
                        CellRightNavigation()
                    },
                    onClick = {
                        navController.slideFromRight(R.id.createAccountStandardFragment)
                    }
                )
                BoxBordered(top = true) {
                    CellPrimary(
                        left = {
                            CellLeftImage(
                                painter = painterResource(R.drawable.touchid_24),
                                type = ImageType.Rectangle,
                                size = 24
                            )
                        },
                        middle = {
                            CellMiddleInfo(
                                title = stringResource(R.string.CreateNewWallet_Passkey).hs,
                                subtitle = stringResource(R.string.CreateNewWallet_Passkey_Description).hs,
                            )
                        },
                        right = {
                            CellRightNavigation()
                        },
                        onClick = {
                            navController.slideFromRight(R.id.createAccountPasskeyFragment)
                        }
                    )
                }
            }

        }
    }
}

@Composable
@Preview
fun Preview_CreateAccountScreen() {
    ComposeAppTheme {
        CreateAccountScreen(NavController(LocalContext.current))
    }
}
