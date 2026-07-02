package io.horizontalsystems.walletkit.modules.send.zcash.shield

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.serializers.HSScreenKClassSerializer
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class ShieldZcashPage(val input: Input) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = viewModel<ShieldZcashViewModel>(factory = ShieldZcashModule.Factory(input.wallet))
        ShieldZcashScreen(navigation, viewModel, input.entryPointDestId)
    }

    @Serializable
    data class Input(
        val wallet: Wallet,
        @Serializable(with = HSScreenKClassSerializer::class) val entryPointDestId: KClass<out HSPage>
    )

}
