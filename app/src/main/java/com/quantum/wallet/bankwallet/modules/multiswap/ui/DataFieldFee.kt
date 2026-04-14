package com.quantum.wallet.bankwallet.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.slideFromBottom
import com.quantum.wallet.bankwallet.modules.multiswap.FeeRow
import com.quantum.wallet.bankwallet.modules.multiswap.SwapInfoDialog

@Composable
fun DataFieldFee(
    navController: NavController,
    primary: String,
    secondary: String?,
) {
    DataFieldFeeTemplate(
        navController = navController,
        primary = primary,
        secondary = secondary,
        title = stringResource(id = R.string.FeeSettings_NetworkFee),
        infoText = stringResource(id = R.string.FeeSettings_NetworkFee_Info)
    )
}

@Composable
fun DataFieldFeeTemplate(
    navController: NavController,
    primary: String,
    secondary: String?,
    title: String,
    infoText: String?,
) {
    FeeRow(
        title = title,
        valueFiat = secondary,
        valueToken = primary,
        onInfoClick = infoText?.let {
            {
                navController.slideFromBottom(
                    R.id.swapInfoDialog,
                    SwapInfoDialog.Input(title, infoText)
                )
            }
        }
    )
}