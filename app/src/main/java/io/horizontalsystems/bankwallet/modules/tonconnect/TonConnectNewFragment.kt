package io.horizontalsystems.bankwallet.modules.tonconnect

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import com.tonapps.wallet.data.tonconnect.entities.DAppRequestEntity
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.bankwallet.serializers.DAppRequestEntitySerializer
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class TonConnectNewFragment(@Serializable(with = DAppRequestEntitySerializer::class) val input: DAppRequestEntity) : HSScreen() {
    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        val resultEventBus = LocalResultEventBus.current
        TonConnectNewScreen(
            navController = navController,
            requestEntity = input,
            onResult = { approved ->
                resultEventBus.sendResult(Result(approved))
                navController.removeLastOrNull()
            },
        )
    }

    @Parcelize
    data class Result(val approved: Boolean) : Parcelable
}
