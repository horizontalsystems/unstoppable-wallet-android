package com.quantum.wallet.bankwallet.modules.tonconnect

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.tonapps.wallet.data.tonconnect.entities.DAppRequestEntity
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.setNavigationResultX
import kotlinx.parcelize.Parcelize

class TonConnectNewFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        withInput<DAppRequestEntity>(navController) { input ->
            TonConnectNewScreen(
                navController = navController,
                requestEntity = input,
                onResult = { approved ->
                    navController.setNavigationResultX(Result(approved))
                    navController.popBackStack()
                },
            )
        }
    }

    @Parcelize
    data class Result(val approved: Boolean) : Parcelable
}
