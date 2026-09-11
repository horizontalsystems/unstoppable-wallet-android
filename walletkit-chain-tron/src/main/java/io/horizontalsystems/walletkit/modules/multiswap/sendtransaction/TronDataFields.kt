package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataField
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataFieldFeeTemplate
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.send.SendModule

/** Bandwidth and energy a Tron transaction burns, as the network reports them. */
data class DataFieldTronResources(val resourcesConsumed: String) : DataField {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        DataFieldFeeTemplate(
            navigation = navigation,
            primary = resourcesConsumed,
            secondary = null,
            title = stringResource(R.string.FeeInfo_TronResourcesConsumed_Title),
            infoText = stringResource(R.string.FeeInfo_TronResourcesConsumed_Description),
        )
    }
}

/** Fee charged for activating a recipient account that has not been used yet. */
data class DataFieldTronActivationFee(val fee: SendModule.AmountData) : DataField {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        DataFieldFeeTemplate(
            navigation = navigation,
            primary = fee.primary.getFormattedPlain(),
            secondary = fee.secondary?.getFormattedPlain(),
            title = stringResource(R.string.FeeInfo_TronActivationFee_Title),
            infoText = stringResource(R.string.FeeInfo_TronActivationFee_Description),
        )
    }
}
