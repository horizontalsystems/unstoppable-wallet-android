package io.horizontalsystems.bankwallet.modules.send.zcash.shield

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class ShieldZcashFragment(val input: Input) : HSScreen() {

    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        val viewModel = viewModel<ShieldZcashViewModel>(factory = ShieldZcashModule.Factory(input.wallet))
        ShieldZcashScreen(navController, viewModel, input.entryPointDestId)
    }

    @Serializable
    data class Input(
        val wallet: Wallet,
        val entryPointDestId: KClass<out HSScreen>
    )

}
