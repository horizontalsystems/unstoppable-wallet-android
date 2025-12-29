package io.horizontalsystems.bankwallet.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.multiswap.QuoteInfoRow
import io.horizontalsystems.bankwallet.modules.multiswap.SwapInfoDialog
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs

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
    QuoteInfoRow(
        title = title,
        value = primary.hs(color = ComposeAppTheme.colors.leah),
        valueSecondary = secondary?.hs(color = ComposeAppTheme.colors.grey),
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