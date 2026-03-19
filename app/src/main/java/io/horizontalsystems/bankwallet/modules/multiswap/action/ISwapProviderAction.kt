package io.horizontalsystems.bankwallet.modules.multiswap.action

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.NavController

interface ISwapProviderAction {
    val inProgress: Boolean

    @Composable
    fun getTitle() : String

    @Composable
    fun getTitleInProgress() : String

    @Composable
    fun getDescription() : String? = null

    fun execute(navController: NavController, onActionCompleted: () -> Unit)
}
