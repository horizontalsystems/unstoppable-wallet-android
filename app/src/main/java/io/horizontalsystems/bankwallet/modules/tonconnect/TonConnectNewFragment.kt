package io.horizontalsystems.bankwallet.modules.tonconnect

import android.os.Parcelable
import androidx.compose.runtime.Composable
import com.tonapps.wallet.data.tonconnect.entities.DAppRequestEntity
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.modules.nav3.NavController
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
