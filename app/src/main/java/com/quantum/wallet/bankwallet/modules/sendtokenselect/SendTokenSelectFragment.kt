package com.quantum.wallet.bankwallet.modules.sendtokenselect

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.getInput
import com.quantum.wallet.bankwallet.core.providers.Translator
import com.quantum.wallet.bankwallet.core.slideFromRight
import com.quantum.wallet.bankwallet.modules.send.address.EnterAddressFragment
import com.quantum.wallet.bankwallet.modules.tokenselect.TokenSelectScreen
import com.quantum.wallet.bankwallet.modules.tokenselect.TokenSelectViewModel
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

class SendTokenSelectFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<Input>()

        val blockchainTypes = input?.blockchainTypes
        val tokenTypes = input?.tokenTypes
        val view = LocalView.current
        TokenSelectScreen(
            navController = navController,
            title = stringResource(R.string.Balance_Send),
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
