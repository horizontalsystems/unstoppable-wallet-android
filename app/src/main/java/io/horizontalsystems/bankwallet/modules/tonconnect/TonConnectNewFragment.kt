package io.horizontalsystems.bankwallet.modules.tonconnect

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import com.tonapps.wallet.data.tonconnect.entities.DAppRequestEntity
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import kotlinx.parcelize.Parcelize

class TonConnectNewFragment(val input: DAppRequestEntity) : BaseComposeFragment() {
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
