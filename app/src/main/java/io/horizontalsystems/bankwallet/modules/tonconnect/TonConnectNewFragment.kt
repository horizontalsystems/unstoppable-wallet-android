package io.horizontalsystems.bankwallet.modules.tonconnect

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import com.tonapps.wallet.data.tonconnect.entities.DAppRequestEntity
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.bankwallet.serializers.DAppRequestEntitySerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class TonConnectNewScreen(
    @Serializable(with = DAppRequestEntitySerializer::class)
    val requestEntity: DAppRequestEntity
) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>
    ) {
        val resultBus = LocalResultEventBus.current
        TonConnectNewScreen(
            backStack = backStack,
            requestEntity = requestEntity,
            onResult = { approved ->
                resultBus.sendResult(result = Result(approved))
                backStack.removeLastOrNull()
            },
        )
    }

    data class Result(val approved: Boolean)
}

class TonConnectNewFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
//        withInput<DAppRequestEntity>(navController) { input ->
//            TonConnectNewScreen(
//                backStack = navController,
//                requestEntity = input,
//                onResult = { approved ->
//                    navController.setNavigationResultX(Result(approved))
//                    navController.popBackStack()
//                },
//            )
//        }
    }

    @Parcelize
    data class Result(val approved: Boolean) : Parcelable
}
