package cash.p.terminal.modules.swapxxx.action

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

interface ISwapProviderAction {
    val inProgress: Boolean

    @Composable
    fun getTitle() : String

    @Composable
    fun getTitleInProgress() : String

    fun execute(navController: NavController, onActionCompleted: () -> Unit)
}
