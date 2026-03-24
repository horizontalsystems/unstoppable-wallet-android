package io.horizontalsystems.bankwallet.modules.tonconnect

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import com.tonapps.wallet.data.tonconnect.entities.DAppRequestEntity
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.parcelize.Parcelize

class TonConnectNewFragment(dAppRequestEntity: DAppRequestEntity) : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        withInput<DAppRequestEntity>(navController) { input ->
            TonConnectNewScreen(
                navController = navController,
                requestEntity = input,
                onResult = { approved ->
                    navController.setNavigationResultX(Result(approved))
                    navController.removeLastOrNull()
                },
            )
        }
    }

    @Parcelize
    data class Result(val approved: Boolean) : Parcelable
}
