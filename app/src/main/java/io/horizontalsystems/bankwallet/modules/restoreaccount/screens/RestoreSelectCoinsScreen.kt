package io.horizontalsystems.bankwallet.modules.restoreaccount.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.removeLastUntil
import io.horizontalsystems.bankwallet.modules.restoreaccount.RestoreViewModel
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.ManageWalletsScreen
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class RestoreSelectCoinsScreen(
    val popOffOnSuccess: KClass<out HSScreen>,
    val popOffInclusive: Boolean
) :  RestoreAccountChildScreen() {
    @Composable
    override fun GetContent(backStack: NavBackStack<HSScreen>) {
        val mainViewModel = viewModel<RestoreViewModel>()

        ManageWalletsScreen(
            mainViewModel = mainViewModel,
            openBirthdayHeightConfigure = { token ->
                when (token.blockchainType) {
                    BlockchainType.Zcash -> backStack.add(ZcashConfigureScreen)
                    BlockchainType.Monero -> backStack.add(MoneroConfigureScreen)
                    else -> Unit
                }
            },
            onBackClick = {
                backStack.removeLastOrNull()
            }
        ) {
            backStack.removeLastUntil(popOffOnSuccess, popOffInclusive)
        }
    }
}