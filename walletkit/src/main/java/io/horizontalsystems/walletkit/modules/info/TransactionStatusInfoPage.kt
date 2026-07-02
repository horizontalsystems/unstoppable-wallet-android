package io.horizontalsystems.walletkit.modules.info

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.info.ui.InfoBody
import io.horizontalsystems.walletkit.modules.info.ui.InfoSubHeader
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import kotlinx.serialization.Serializable

@Serializable
data object TransactionStatusInfoPage : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        InfoScreen(
            navigation
        )
    }

}

@Composable
private fun InfoScreen(
    navigation: HSNavigation
) {
    HSScaffold(
        title = stringResource(R.string.TransactionInfo_Status),
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = { navigation.removeLastOrNull() }
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
