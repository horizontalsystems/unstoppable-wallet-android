package io.horizontalsystems.bankwallet.modules.sendtokenselect

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.send.address.EnterAddressFragment
import io.horizontalsystems.bankwallet.modules.tokenselect.TokenSelectScreen
import io.horizontalsystems.bankwallet.modules.tokenselect.TokenSelectViewModel
import io.horizontalsystems.bankwallet.serializers.BigDecimalSerializer
import io.horizontalsystems.bankwallet.serializers.BlockchainTypeSerializer
import io.horizontalsystems.bankwallet.serializers.TokenTypeSerializer
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class SendTokenSelectScreen(
    val blockchainTypes: List<@Serializable(with = BlockchainTypeSerializer::class) BlockchainType>? = null,
    val tokenTypes: List<@Serializable(with = TokenTypeSerializer::class) TokenType>? = null,
    val address: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal? = null,
    val memo: String? = null,
) : HSScreen()

class SendTokenSelectFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<Input>()

        val blockchainTypes = input?.blockchainTypes
        val tokenTypes = input?.tokenTypes
        val view = LocalView.current
        TokenSelectScreen(
            title = stringResource(R.string.Balance_Send),
            onBack = { navController.popBackStack() },
            onClickItem = {
                val sendTitle = Translator.getString(R.string.Send_Title, it.wallet.token.fullCoin.coin.code)
                navController.slideFromRight(
                    R.id.enterAddressFragment,
                    EnterAddressFragment.Input(
                        wallet = it.wallet,
                        title = sendTitle,
                        sendEntryPointDestId = R.id.sendTokenSelectFragment,
                        address = input?.address,
                        amount = input?.amount,
                        memo = input?.memo,
                    )
                )
            },
            viewModel = viewModel(factory = TokenSelectViewModel.FactoryForSend(blockchainTypes, tokenTypes)),
        )
    }

    @Parcelize
    data class Input(
        val blockchainTypes: List<BlockchainType>?,
        val tokenTypes: List<TokenType>?,
        val address: String,
        val amount: BigDecimal?,
        val memo: String?
    ) : Parcelable
}
