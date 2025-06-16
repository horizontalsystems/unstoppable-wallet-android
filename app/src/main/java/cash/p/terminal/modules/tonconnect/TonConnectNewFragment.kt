package cash.p.terminal.modules.tonconnect

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.core.setNavigationResultX
import cash.p.terminal.ui_compose.BaseComposeFragment
import com.tonapps.wallet.data.tonconnect.entities.DAppRequestEntity
import kotlinx.android.parcel.Parcelize

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
