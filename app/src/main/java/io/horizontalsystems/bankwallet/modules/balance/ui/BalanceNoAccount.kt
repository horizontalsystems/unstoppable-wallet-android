package io.horizontalsystems.bankwallet.modules.balance.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsFragment
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ScreenMessageWithAction
import io.horizontalsystems.core.getNavigationResult

@Composable
fun BalanceNoAccount(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenMessageWithAction(
            text = stringResource(id = R.string.Balance_NoWalletAlert),
            icon = R.drawable.ic_wallet_48,
        ) {
            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                title = stringResource(R.string.Button_Create),
                onClick = {
                    openPageWithTermsAgreed(navController, R.id.createAccountFragment)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            ButtonPrimaryDefault(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                title = stringResource(R.string.Button_Restore),
                onClick = {
                    openPageWithTermsAgreed(navController, R.id.restoreMnemonicFragment)
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

private fun openPageWithTermsAgreed(navController: NavController, destination: Int) {
    navController.getNavigationResult(TermsFragment.resultBundleKey) { bundle ->
        val agreedToTerms = bundle.getInt(TermsFragment.requestResultKey)

        if (agreedToTerms == TermsFragment.RESULT_OK) {
            navController.slideFromRight(destination)
        }
    }

    navController.slideFromBottom(R.id.termsFragment)
}
