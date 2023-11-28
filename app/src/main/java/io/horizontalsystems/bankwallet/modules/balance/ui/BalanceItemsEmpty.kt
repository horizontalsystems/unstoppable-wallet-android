package io.horizontalsystems.bankwallet.modules.balance.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ScreenMessageWithAction

@Composable
fun BalanceItemsEmpty(navController: NavController) {
    ScreenMessageWithAction(
        text = stringResource(R.string.Balance_NoCoinsAlert),
        icon = R.drawable.ic_add_to_wallet_2_48
    ) {
        ButtonPrimaryYellow(
            modifier = Modifier
                .padding(horizontal = 48.dp)
                .fillMaxWidth(),
            title = stringResource(R.string.Balance_AddCoins),
            onClick = { navController.slideFromRight(R.id.manageWalletsFragment) }
        )
    }
}
