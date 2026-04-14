package com.quantum.wallet.bankwallet.modules.info

import android.os.Parcelable
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
import com.quantum.wallet.bankwallet.modules.info.ui.InfoHeader
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.InfoTextBody
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import kotlinx.parcelize.Parcelize

class TransactionLockTimeInfoFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            InfoScreen(input.lockTime, navController)
        }
    }

    @Parcelize
    data class Input(val lockTime: String) : Parcelable
}

@Composable
private fun InfoScreen(
    lockDate: String,
    navController: NavController
) {

    val description = stringResource(R.string.Info_LockTime_Description, lockDate)

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
            InfoHeader(R.string.Info_LockTime_Title)
            InfoTextBody(description)
            VSpacer(20.dp)
        }
    }
}
