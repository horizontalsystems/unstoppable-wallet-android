package io.horizontalsystems.bankwallet.modules.tonconnect

import android.os.Parcelable
import androidx.compose.runtime.Composable
import com.tonapps.wallet.data.tonconnect.entities.DAppRequestEntity
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.bankwallet.serializers.DAppRequestEntitySerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class TonConnectNewPage(@Serializable(with = DAppRequestEntitySerializer::class) val input: DAppRequestEntity) : HSPage() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
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
