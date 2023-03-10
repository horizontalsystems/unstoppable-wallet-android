package cash.p.terminal.modules.balance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.navigateWithTermsAccepted
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui.compose.components.ButtonPrimaryTransparent
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow

@Composable
fun BalanceNoAccount(navController: NavController) {
    Column(
        modifier = Modifier
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
        Spacer(modifier = Modifier.height(16.dp))
        ButtonPrimaryDefault(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            title = stringResource(R.string.ManageAccounts_ImportWallet),
            onClick = {
                navController.navigateWithTermsAccepted {
                    navController.slideFromRight(R.id.restoreAccountFragment)
                }
            }
        )
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