package com.quantum.wallet.bankwallet.modules.walletconnect.list

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.getInput
import com.quantum.wallet.bankwallet.modules.walletconnect.list.ui.WCSessionsScreen
import kotlinx.parcelize.Parcelize

class WCListFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<Input>()
        WCSessionsScreen(
            navController,
            input?.deepLinkUri
        )
    }

    @Parcelize
    data class Input(val deepLinkUri: String) : Parcelable
}
