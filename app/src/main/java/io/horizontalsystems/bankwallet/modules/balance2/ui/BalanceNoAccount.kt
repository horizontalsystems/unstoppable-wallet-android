package io.horizontalsystems.bankwallet.modules.balance2.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow

@Composable
fun BalanceNoAccount(navController: NavController) {
    ComposeAppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(painter = painterResource(id = R.drawable.ic_wallet_in_circle), contentDescription = null)
                Spacer(modifier = Modifier.height(26.dp))
                Text(
                    modifier = Modifier.padding(horizontal = 48.dp),
                    text = stringResource(id = R.string.Balance_NoWalletAlert),
                    style = ComposeAppTheme.typography.subhead2,
                    color = ComposeAppTheme.colors.grey,
                    textAlign = TextAlign.Center,
                )
            }

            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                title = stringResource(R.string.Button_Create),
                onClick = {
                    navController.slideFromRight(R.id.createAccountFragment)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            ButtonPrimaryDefault(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                title = stringResource(R.string.Button_Restore),
                onClick = {
                    navController.slideFromRight(R.id.restoreMnemonicFragment)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            ButtonPrimaryDefault(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                title = stringResource(R.string.Button_WatchAddress),
                onClick = {
                    navController.slideFromRight(R.id.watchAddressFragment)
                }
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}