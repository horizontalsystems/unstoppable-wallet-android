package io.horizontalsystems.bankwallet.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfoTextIcon
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellSecondary
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.marketkit.models.BlockchainType

data class DataFieldRecipientExtended(
    val address: Address,
    val blockchainType: BlockchainType
) : DataField {
    @Composable
    override fun GetContent(navController: NavController) {
        val contact = App.contactsRepository.getContactsFiltered(
            blockchainType,
            addressQuery = address.hex
        ).firstOrNull()

        CellView(
            title = stringResource(R.string.Swap_Recipient),
            value = contact?.name ?: address.hex.shorten(),
            onEditClick = {
                //todo decide how to open recipient edit screen
                //navController.slideFromRight(R.id.swapSettings)
            },
        )
    }
}

@Composable
fun CellView(
    title: String,
    value: String,
    onEditClick: (() -> Unit)? = null,
) {
    CellSecondary(
        middle = {
            CellMiddleInfo(
                eyebrow = title.hs
            )
        },
        right = {
            CellRightInfoTextIcon(
                text = value.hs(color = ComposeAppTheme.colors.leah),
                icon = painterResource(R.drawable.pen_filled_24),
                iconTint = ComposeAppTheme.colors.grey,
                onIconClick = onEditClick
            )
        },
    )
}

@Preview
@Composable
fun DataFieldRecipientExtendedPreview() {
    val navController = rememberNavController()
    ComposeAppTheme {
        DataFieldRecipientExtended(
            Address("0x1234567890abcdef1234567890abcdef12345678"),
            BlockchainType.Bitcoin
        ).GetContent(navController = navController)
    }
}
