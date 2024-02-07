package io.horizontalsystems.bankwallet.modules.swapxxx.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

interface ISwapSetting {
    val id: String

    @Composable
    fun GetContent(
        navController: NavController,
        onError: (Throwable?) -> Unit,
        onValueChange: (Any?) -> Unit
    )
}
