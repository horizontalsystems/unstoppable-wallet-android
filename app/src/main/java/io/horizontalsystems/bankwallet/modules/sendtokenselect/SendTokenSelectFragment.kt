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
import io.horizontalsystems.bankwallet.modules.send.address.EnterAddressFragment
import io.horizontalsystems.bankwallet.modules.tokenselect.TokenSelectScreen
import io.horizontalsystems.bankwallet.modules.tokenselect.TokenSelectViewModel
import io.horizontalsystems.core.helpers.HudHelper
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
            searchHintText = stringResource(R.string.Balance_SendHint_CoinName),
            onClickItem = {
                when {
                    it.sendEnabled -> {
                        val sendTitle = Translator.getString(R.string.Send_Title, it.wallet.token.fullCoin.coin.code)
                        navController.slideFromRight(
                            R.id.enterAddressFragment,
                            EnterAddressFragment.Input(
                                wallet = it.wallet,
                                title = sendTitle,
                                sendEntryPointDestId = R.id.sendTokenSelectFragment,
                                address = input?.address,
                                amount = input?.amount,
                            )
                        )
                    }

                    it.syncingProgress.progress != null -> {
                        HudHelper.showWarningMessage(view, R.string.Hud_WaitForSynchronization)
                    }

                    it.errorMessage != null -> {
                        HudHelper.showErrorMessage(view, it.errorMessage)
                    }
                }
            },
            viewModel = viewModel(factory = TokenSelectViewModel.FactoryForSend(blockchainTypes, tokenTypes)),
            emptyItemsText = stringResource(R.string.Balance_NoAssetsToSend)
        )
    }

    @Parcelize
    data class Input(
        val blockchainTypes: List<BlockchainType>?,
        val tokenTypes: List<TokenType>?,
        val address: String,
        val amount: BigDecimal?,
    ) : Parcelable
}
