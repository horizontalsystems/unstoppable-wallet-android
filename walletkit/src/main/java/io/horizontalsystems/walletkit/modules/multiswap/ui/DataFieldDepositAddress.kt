package io.horizontalsystems.walletkit.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.shorten
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.multiswap.SwapInfoSheet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.helpers.TextHelper
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfoTextIcon
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightControlsButtonText
import io.horizontalsystems.walletkit.uiv3.components.cell.CellSecondary
import io.horizontalsystems.walletkit.uiv3.components.cell.hs

// The provider-supplied address the deposit transaction pays. Shown so the user can see (and
// verify) where their funds actually go, not just the recipient of the swapped output. The row
// shows it shortened and copies the full value.
data class DataFieldDepositAddress(val address: String) : DataField {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val view = LocalView.current
        val leah = ComposeAppTheme.colors.leah
        val infoTitle = stringResource(R.string.Swap_DepositAddress)
        val infoText = stringResource(R.string.Swap_DepositAddress_Description)
        CellSecondary(
            middle = {
                CellMiddleInfoTextIcon(
                    text = infoTitle.hs(color = ComposeAppTheme.colors.grey),
                    icon = painterResource(R.drawable.ic_info_filled_20),
                    iconTint = ComposeAppTheme.colors.grey,
                    onIconClick = {
                        navigation.slideFromBottom(SwapInfoSheet(SwapInfoSheet.Input(infoTitle, infoText)))
                    }
                )
            },
            right = {
                CellRightControlsButtonText(
                    subhead = address.shorten().hs(color = leah),
                    icon = painterResource(R.drawable.copy_filled_24),
                    iconTint = leah,
                    onIconClick = {
                        TextHelper.copyText(address)
                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                    },
                )
            },
        )
    }
}
