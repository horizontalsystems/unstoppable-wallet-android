package io.horizontalsystems.bankwallet.modules.multiswap.action

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen

interface ISwapProviderAction {
    val inProgress: Boolean

    @Composable
    fun getTitle() : String

    @Composable
    fun getTitleInProgress() : String

    @Composable
    fun getDescription() : String? = null

    fun execute(navController: NavBackStack<HSScreen>, onActionCompleted: () -> Unit)
}
