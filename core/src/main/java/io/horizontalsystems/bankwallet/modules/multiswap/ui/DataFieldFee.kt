package io.horizontalsystems.bankwallet.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.core.R
import io.horizontalsystems.bankwallet.modules.multiswap.FeeRow
import io.horizontalsystems.bankwallet.modules.multiswap.SwapInfoSheet
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation

@Composable
fun DataFieldFee(
    navigation: HSNavigation,
    primary: String,
    secondary: String?,
) {
    DataFieldFeeTemplate(
        navigation = navigation,
        primary = primary,
        secondary = secondary,
        title = stringResource(id = R.string.FeeSettings_NetworkFee),
        infoText = stringResource(id = R.string.FeeSettings_NetworkFee_Info)
    )
}

@Composable
fun DataFieldFeeTemplate(
    navigation: HSNavigation,
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
                navigation.slideFromBottom(
                    SwapInfoSheet(SwapInfoSheet.Input(title, infoText))
                )
            }
        }
    )
}