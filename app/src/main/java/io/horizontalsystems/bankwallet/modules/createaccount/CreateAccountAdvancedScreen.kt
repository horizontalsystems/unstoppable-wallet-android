package io.horizontalsystems.bankwallet.modules.createaccount

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import kotlinx.serialization.Serializable

@Serializable
data class CreateAccountAdvancedScreen(
    val popOffOnSuccess: Int,
    val popOffInclusive: Boolean
) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        CreateAccountAdvancedScreen(
            onBackClick = { backStack.removeLastOrNull() },
            onFinish = {
//                TODO("xxx nav3")
//                backStack.popBackStack(popUpToInclusiveId, inclusive)
            }
        )
    }
}
