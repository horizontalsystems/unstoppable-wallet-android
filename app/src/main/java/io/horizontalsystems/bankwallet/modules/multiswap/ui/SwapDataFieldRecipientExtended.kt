package cash.p.terminal.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.multiswap.QuoteInfoRow
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.ButtonSecondaryCircle
import cash.p.terminal.ui.compose.components.HSpacer
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.compose.components.subhead2_leah
import cash.p.terminal.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType

data class SwapDataFieldRecipientExtended(
    val address: Address,
    val blockchainType: BlockchainType
) : SwapDataField {
    @Composable
    override fun GetContent(navController: NavController, borderTop: Boolean) {
        QuoteInfoRow(
            borderTop = borderTop,
            title = {
                subhead2_grey(text = stringResource(R.string.Swap_Recipient))
            },
            value = {
                subhead2_leah(
                    modifier = Modifier.weight(1f, false),
                    text = address.hex,
                    textAlign = TextAlign.End
                )

                val view = LocalView.current
                HSpacer(16.dp)
                ButtonSecondaryCircle(
                    icon = R.drawable.ic_copy_20,
                    onClick = {
                        TextHelper.copyText(address.hex)
                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                    }
                )
            }
        )

        val contact = App.contactsRepository.getContactsFiltered(
            blockchainType,
            addressQuery = address.hex
        ).firstOrNull()

        contact?.name?.let { name ->
            QuoteInfoRow(
                borderTop = borderTop,
                title = {
                    subhead2_grey(text = stringResource(R.string.TransactionInfo_ContactName))
                },
                value = {
                    subhead2_leah(
                        text = name,
                        textAlign = TextAlign.End
                    )
                }
            )
        }
    }
}

@Preview
@Composable
fun SwapDataFieldRecipientExtendedPreview() {
    val navController = rememberNavController()
    ComposeAppTheme {
        SwapDataFieldRecipientExtended(
            Address("0x1234567890abcdef1234567890abcdef12345678"),
            BlockchainType.Bitcoin
        ).GetContent(navController = navController, borderTop = true)
    }
}
