package io.horizontalsystems.bankwallet.modules.sendtokenselect

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.send.address.EnterAddressPage
import io.horizontalsystems.bankwallet.modules.tokenselect.TokenSelectScreen
import io.horizontalsystems.bankwallet.modules.tokenselect.TokenSelectViewModel
import io.horizontalsystems.bankwallet.serializers.BigDecimalSerializer
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class SendTokenSelectPage(val input: Input? = null) : HSPage() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        val blockchainTypes = input?.blockchainTypes
        val tokenTypes = input?.tokenTypes
        val view = LocalView.current
        TokenSelectScreen(
            navController = navController,
            title = stringResource(R.string.Balance_Send),
            onClickItem = {
                val sendTitle = Translator.getString(R.string.Send_Title, it.wallet.token.fullCoin.coin.code)
                navController.slideFromRight(
                    EnterAddressPage(EnterAddressPage.Input(
                        wallet = it.wallet,
                        title = sendTitle,
                        sendEntryPointDestId = SendTokenSelectPage::class,
                        address = input?.address,
                        amount = input?.amount,
                        memo = input?.memo,
                    ))
                )
            },
            viewModel = viewModel(factory = TokenSelectViewModel.FactoryForSend(blockchainTypes, tokenTypes)),
        )
    }

    @Serializable
    data class Input(
        val blockchainTypes: List<BlockchainType>?,
        val tokenTypes: List<TokenType>?,
        val address: String,
        @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal?,
        val memo: String?
    )
}
