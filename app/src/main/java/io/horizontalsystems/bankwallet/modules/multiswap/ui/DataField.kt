package io.horizontalsystems.bankwallet.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen

interface DataField {
    @Composable
    fun GetContent(navController: NavBackStack<HSScreen>)
}
