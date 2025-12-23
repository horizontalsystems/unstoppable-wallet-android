package io.horizontalsystems.bankwallet.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.multiswap.QuoteInfoRow
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType

data class DataFieldRecipientExtended(
    val address: Address,
    val blockchainType: BlockchainType
) : DataField {
    @Composable
    override fun GetContent(navController: NavController, borderTop: Boolean) {
        val view = LocalView.current
        QuoteInfoRow(
            title = stringResource(R.string.Swap_Recipient),
            value = address.hex.shorten().hs,
            onCellClick = {
                TextHelper.copyText(address.hex)
                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
            },
        )

        val contact = App.contactsRepository.getContactsFiltered(
            blockchainType,
            addressQuery = address.hex
        ).firstOrNull()

        contact?.name?.let { name ->
            QuoteInfoRow(
                title = stringResource(R.string.TransactionInfo_ContactName),
                value = name.hs
            )
        }
    }
}

@Preview
@Composable
fun DataFieldRecipientExtendedPreview() {
    val navController = rememberNavController()
    ComposeAppTheme {
        DataFieldRecipientExtended(
            Address("0x1234567890abcdef1234567890abcdef12345678"),
            BlockchainType.Bitcoin
        ).GetContent(navController = navController, borderTop = true)
    }
}
