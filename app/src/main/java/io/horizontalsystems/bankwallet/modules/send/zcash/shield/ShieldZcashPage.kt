package io.horizontalsystems.bankwallet.modules.send.zcash.shield

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.serializers.HSScreenKClassSerializer
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class ShieldZcashPage(val input: Input) : HSPage() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        val viewModel = hiltViewModel<ShieldZcashViewModel, ShieldZcashViewModel.Factory> { factory ->
            factory.create(input.wallet)
        }
        ShieldZcashScreen(navController, viewModel, input.entryPointDestId)
    }

    @Serializable
    data class Input(
        val wallet: Wallet,
        @Serializable(with = HSScreenKClassSerializer::class) val entryPointDestId: KClass<out HSPage>
    )

}
