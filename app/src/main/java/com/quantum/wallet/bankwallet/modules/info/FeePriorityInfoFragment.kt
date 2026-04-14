package com.quantum.wallet.bankwallet.modules.info

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.modules.info.ui.InfoBody
import com.quantum.wallet.bankwallet.modules.info.ui.InfoHeader
import com.quantum.wallet.bankwallet.modules.info.ui.InfoSubHeader
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold

class FeePriorityInfoFragment : BaseComposeFragment() {

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
        title = "",
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
            InfoHeader(R.string.FeeInfo_Title)
            InfoBody(R.string.FeeInfo_Description)
            InfoSubHeader(R.string.FeeInfo_Slow)
            InfoBody(R.string.FeeInfo_SlowDescription)
            InfoSubHeader(R.string.FeeInfo_Average)
            InfoBody(R.string.FeeInfo_AverageDescription)
            InfoSubHeader(R.string.FeeInfo_Fast)
            InfoBody(R.string.FeeInfo_FastDescription)
            VSpacer(20.dp)
        }
    }
}
