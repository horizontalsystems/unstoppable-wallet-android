package io.horizontalsystems.bankwallet.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

interface SwapDataField {
    @Composable
    fun GetContent(navController: NavController)
}
