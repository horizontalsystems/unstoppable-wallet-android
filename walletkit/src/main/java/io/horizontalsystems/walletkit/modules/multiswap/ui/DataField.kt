package io.horizontalsystems.walletkit.modules.multiswap.ui

import androidx.compose.runtime.Composable
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation

interface DataField {
    @Composable
    fun GetContent(navigation: HSNavigation)
}
