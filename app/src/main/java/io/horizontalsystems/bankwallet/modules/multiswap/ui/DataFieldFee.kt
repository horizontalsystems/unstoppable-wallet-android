package io.horizontalsystems.bankwallet.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.multiswap.FeeRow
import io.horizontalsystems.bankwallet.modules.multiswap.SwapInfoDialog
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen

@Composable
fun DataFieldFee(
    navController: NavBackStack<HSScreen>,
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
    navController: NavBackStack<HSScreen>,
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
                    SwapInfoDialog(),
                    SwapInfoDialog.Input(title, infoText)
                )
            }
        }
    )
}