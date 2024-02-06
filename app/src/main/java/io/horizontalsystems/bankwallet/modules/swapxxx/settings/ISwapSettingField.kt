package cash.p.terminal.modules.swapxxx.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

interface ISwapSettingField {
    val id: String

    @Composable
    fun GetContent(
        navController: NavController,
        onError: (Throwable?) -> Unit,
        onValueChange: (Any?) -> Unit
    )
}
