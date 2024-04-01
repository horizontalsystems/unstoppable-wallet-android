package cash.p.terminal.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

interface SwapDataField {
    @Composable
    fun GetContent(navController: NavController, borderTop: Boolean)
}
