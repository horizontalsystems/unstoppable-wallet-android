package com.quantum.wallet.bankwallet.modules.info

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.modules.info.ui.InfoBody
import com.quantum.wallet.bankwallet.modules.info.ui.InfoSubHeader
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold

class TransactionStatusInfoFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        InfoScreen(
            navController
        )
    }

}

@Composable
private fun InfoScreen(
    navController: NavController
) {
    HSScaffold(
        title = stringResource(R.string.TransactionInfo_Status),
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = { navController.popBackStack() }
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            InfoSubHeader(R.string.StatusInfo_Pending)
            InfoBody(R.string.StatusInfo_PendingDescription)
            InfoSubHeader(R.string.StatusInfo_Processing)
            InfoBody(R.string.StatusInfo_ProcessingDescription)
            InfoSubHeader(R.string.StatusInfo_Confirmed)
            InfoBody(R.string.StatusInfo_ConfirmedDescription)
            InfoSubHeader(R.string.StatusInfo_Failed)
            InfoBody(R.string.StatusInfo_FailedDescription)
            VSpacer(20.dp)
        }
    }
}
