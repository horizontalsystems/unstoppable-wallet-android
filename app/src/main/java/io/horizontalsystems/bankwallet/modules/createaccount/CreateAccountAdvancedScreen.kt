package io.horizontalsystems.bankwallet.modules.createaccount

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.removeLastUntil
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class CreateAccountAdvancedScreen(
    val popOffOnSuccess: KClass<out HSScreen>,
    val popOffInclusive: Boolean
) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>
    ) {
        CreateAccountAdvancedScreen(
            onBackClick = { backStack.removeLastOrNull() },
            onFinish = {
                backStack.removeLastUntil(popOffOnSuccess, popOffInclusive)
            }
        )
    }
}
