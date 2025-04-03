package cash.p.terminal.modules.sendtokenselect

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.modules.send.SendFragment
import cash.p.terminal.modules.tokenselect.TokenSelectScreen
import cash.p.terminal.modules.tokenselect.TokenSelectViewModel
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.getInput
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

class SendTokenSelectFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<Input>()

        val blockchainTypes = input?.blockchainTypes
        val tokenTypes = input?.tokenTypes
        val prefilledData = input?.prefilledData
        val view = LocalView.current
        val viewModel: TokenSelectViewModel =
            viewModel(factory = TokenSelectViewModel.FactoryForSend(blockchainTypes, tokenTypes))
        TokenSelectScreen(
            navController = navController,
            title = stringResource(R.string.Balance_Send),
            searchHintText = stringResource(R.string.Balance_SendHint_CoinName),
            onClickItem = {
                when {
                    it.sendEnabled -> {
                        val sendTitle = Translator.getString(
                            R.string.Send_Title,
                            it.wallet.token.fullCoin.coin.code
                        )
                        navController.slideFromRight(
                            R.id.sendXFragment,
                            SendFragment.Input(
                                wallet = it.wallet,
                                sendEntryPointDestId = R.id.sendTokenSelectFragment,
                                title = sendTitle,
                                prefilledAddressData = prefilledData,
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
            uiState = viewModel.uiState,
            updateFilter = viewModel::updateFilter,
            emptyItemsText = stringResource(R.string.Balance_NoAssetsToSend)
        )
    }

    @Parcelize
    data class Input(
        val blockchainTypes: List<BlockchainType>?,
        val tokenTypes: List<TokenType>?,
        val address: String,
        val amount: BigDecimal?,
    ) : Parcelable {
        val prefilledData: PrefilledData
            get() = PrefilledData(address, amount)
    }
}

@Parcelize
data class PrefilledData(
    val address: String,
    val amount: BigDecimal? = null,
) : Parcelable