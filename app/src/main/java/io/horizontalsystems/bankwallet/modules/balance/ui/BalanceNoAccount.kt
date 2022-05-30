package io.horizontalsystems.bankwallet.modules.balance.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ScreenMessageNoAccount

@Composable
fun BalanceNoAccount(navController: NavController) {
    ComposeAppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenMessageNoAccount(
                text = stringResource(id = R.string.Balance_NoWalletAlert),
                icon = R.drawable.ic_wallet_48,
            ){
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp),
                    title = stringResource(R.string.Button_Create),
                    onClick = {
                        navController.slideFromRight(R.id.createAccountFragment)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                ButtonPrimaryDefault(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp),
                    title = stringResource(R.string.Button_Restore),
                    onClick = {
                        navController.slideFromRight(R.id.restoreMnemonicFragment)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                ButtonPrimaryTransparent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp),
                    title = stringResource(R.string.Button_WatchAddress),
                    onClick = {
                        navController.slideFromRight(R.id.watchAddressFragment)
                    }
                )
            }
        }
    }
}