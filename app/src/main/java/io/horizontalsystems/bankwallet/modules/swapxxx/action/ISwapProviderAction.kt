package io.horizontalsystems.bankwallet.modules.swapxxx.action

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

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
