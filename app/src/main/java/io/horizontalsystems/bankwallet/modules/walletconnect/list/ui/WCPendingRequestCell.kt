package io.horizontalsystems.bankwallet.modules.walletconnect.list.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.ui.compose.components.BadgeCount
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah

@Composable
fun PendingRequestsCell(pendingRequests: Int, navController: NavController) {
    CellSingleLineLawrenceSection(listOf(pendingRequests)) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    enabled = pendingRequests > 0,
                    onClick = {
                        navController.slideFromBottom(R.id.wc2RequestListFragment)
                    }
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            body_leah(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.WalletConnect_PendingRequests),
            )
            if (pendingRequests > 0) {
                BadgeCount(
                    text = pendingRequests.toString(),
                )
            }
            Image(
                modifier = Modifier.padding(start = 8.dp),
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = null
            )
        }
    }
}
