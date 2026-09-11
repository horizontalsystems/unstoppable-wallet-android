package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.hodler.LockTimeInterval
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.stringResId
import io.horizontalsystems.walletkit.modules.multiswap.QuoteInfoRow
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataField
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.uiv3.components.cell.hs

/** Hodler timelock applied to the recipient's output. */
data class DataFieldBtcTimeLock(val interval: LockTimeInterval) : DataField {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        QuoteInfoRow(
            title = stringResource(R.string.Send_TimeLock),
            value = stringResource(interval.stringResId()).hs(ComposeAppTheme.colors.leah),
        )
    }
}

/** Replace-by-fee switched off, which is the non-default choice. */
data object DataFieldBtcRbfDisabled : DataField {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        QuoteInfoRow(
            title = stringResource(R.string.Send_Rbf),
            value = stringResource(R.string.Send_RbfDisabled).hs(ComposeAppTheme.colors.leah),
        )
    }
}
