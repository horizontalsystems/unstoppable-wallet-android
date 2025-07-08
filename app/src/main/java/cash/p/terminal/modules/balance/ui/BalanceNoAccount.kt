package cash.p.terminal.modules.balance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.hasNFC
import cash.p.terminal.core.navigateWithTermsAccepted
import cash.p.terminal.modules.createaccount.CreateAccountFragment
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.ui.compose.components.ButtonSecondaryDefault
import cash.p.terminal.ui.compose.components.SnackbarError
import cash.p.terminal.ui_compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui_compose.components.ButtonPrimaryDefaults
import cash.p.terminal.ui_compose.components.ButtonPrimaryTransparent
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.core.helpers.HudHelper

@Composable
fun BalanceNoAccount(
    navController: NavController,
    paddingValuesParent: PaddingValues
) {
    val context = LocalContext.current
    val view = LocalView.current
    Column(
        modifier = Modifier
            .padding(bottom = paddingValuesParent.calculateBottomPadding())
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    color = ComposeAppTheme.colors.raina,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(48.dp),
                painter = painterResource(R.drawable.icon_add_to_wallet_24),
                contentDescription = "",
                tint = ComposeAppTheme.colors.grey
            )
        }
        Spacer(Modifier.height(32.dp))
        ButtonPrimaryYellow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            title = stringResource(R.string.ManageAccounts_CreateNewWallet),
            onClick = {
                navController.navigateWithTermsAccepted {
                    navController.slideFromRight(R.id.createAccountFragment)
                }
            }
        )
        Spacer(Modifier.height(16.dp))
        ButtonPrimaryDefault(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            title = stringResource(R.string.new_monero_wallet),
            onClick = {
                navController.navigateWithTermsAccepted {
                    navController.slideFromRight(R.id.createAccountFragment,
                        CreateAccountFragment.Input(
                            popOffOnSuccess = R.id.createAccountFragment,
                            popOffInclusive = true,
                            preselectMonero = true
                        ))
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        ButtonPrimaryDefault(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            title = stringResource(R.string.ManageAccounts_ImportWallet),
            onClick = {
                navController.navigateWithTermsAccepted {
                    navController.slideFromRight(R.id.importWalletFragment)
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (context.hasNFC()) {
            ButtonPrimaryDefault(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                title = stringResource(R.string.hardware_wallet),
                onClick = {
                    navController.navigateWithTermsAccepted {
                        navController.slideFromRight(R.id.hardwareWalletFragment)
                    }
                }
            )
        } else {
            ButtonPrimaryTransparent(
                modifier = Modifier
                    .defaultMinSize(minHeight = ButtonPrimaryDefaults.MinHeight)
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                title = stringResource(R.string.hardware_wallet),
                onClick = {
                    HudHelper.showErrorMessage(view, context.getString(R.string.hardware_wallet_not_detected_error))
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        ButtonPrimaryTransparent(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            title = stringResource(R.string.ManageAccounts_WatchAddress),
            onClick = {
                navController.slideFromRight(R.id.watchAddressFragment)
            }
        )

    }
}
