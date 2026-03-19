package io.horizontalsystems.bankwallet.modules.multiswap.ui

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.NavController

interface DataField {
    @Composable
    fun GetContent(navController: NavController)
}
