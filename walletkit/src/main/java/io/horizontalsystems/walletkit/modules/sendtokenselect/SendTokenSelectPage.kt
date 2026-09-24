package io.horizontalsystems.walletkit.modules.sendtokenselect

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.entities.AddressUri
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.v2.SendRecipientPage
import io.horizontalsystems.walletkit.modules.send.v2.SendV2Page
import io.horizontalsystems.walletkit.modules.tokenselect.TokenSelectScreen
import io.horizontalsystems.walletkit.modules.tokenselect.TokenSelectViewModel
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.serialization.Serializable

@Serializable
data class SendTokenSelectPage(val input: Input? = null) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val blockchainTypes = input?.blockchainTypes
        val tokenTypes = input?.tokenTypes
        TokenSelectScreen(
            navigation = navigation,
            title = stringResource(R.string.Balance_Send),
            onClickItem = { item ->
                val page = if (input != null) {
                    // base-unit amounts (eip-681 `value`/`uint256`) can only be converted to a
                    // readable amount here, where the selected token's decimals are known
                    SendRecipientPage(
                        wallet = item.wallet,
                        address = input.address,
                        amount = input.amount?.humanReadable(item.wallet.token.decimals),
                        memo = input.memo,
                        sendEntryPointDestId = SendTokenSelectPage::class,
                    )
                } else {
                    SendV2Page(
                        wallet = item.wallet,
                        sendEntryPointDestId = SendTokenSelectPage::class,
                    )
                }
                navigation.slideFromRight(page)
            },
            viewModel = viewModel(factory = TokenSelectViewModel.FactoryForSend(blockchainTypes, tokenTypes)),
        )
    }

    @Serializable
    data class Input(
        val blockchainTypes: List<BlockchainType>?,
        val tokenTypes: List<TokenType>?,
        val address: String,
        val amount: AddressUri.Amount?,
        val memo: String?
    )
}
