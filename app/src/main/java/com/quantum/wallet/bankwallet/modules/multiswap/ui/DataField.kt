package com.quantum.wallet.bankwallet.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

interface DataField {
    @Composable
    fun GetContent(navController: NavController)
}
