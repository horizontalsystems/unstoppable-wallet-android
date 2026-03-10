package io.horizontalsystems.bankwallet.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.multiswap.FeeRow
import io.horizontalsystems.bankwallet.modules.multiswap.SwapInfoDialogScreen
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen

@Composable
fun DataFieldFee(
    backStack: NavBackStack<HSScreen>,
    primary: String,
    secondary: String?,
) {
    DataFieldFeeTemplate(
        backStack = backStack,
        primary = primary,
        secondary = secondary,
        title = stringResource(id = R.string.FeeSettings_NetworkFee),
        infoText = stringResource(id = R.string.FeeSettings_NetworkFee_Info)
    )
}

@Composable
fun DataFieldFeeTemplate(
    backStack: NavBackStack<HSScreen>,
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
                backStack.add(
                    SwapInfoDialogScreen(title, infoText)
                )
            }
        }
    )
}