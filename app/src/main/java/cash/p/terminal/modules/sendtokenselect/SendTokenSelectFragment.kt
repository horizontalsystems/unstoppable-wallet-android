package cash.p.terminal.modules.sendtokenselect

import android.os.Parcelable
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.modules.balance.BalanceViewItem2
import cash.p.terminal.modules.send.address.EnterAddressFragment
import cash.p.terminal.modules.tokenselect.TokenSelectScreen
import cash.p.terminal.modules.tokenselect.TokenSelectViewModel
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.getInput
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.BlockchainType
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
            onClickItem = { viewItem ->
                openSendScreen(
                    viewItem = viewItem,
                    view = view,
                    input = input,
                    navController = navController
                )
            },
            onBalanceClick = { viewItem ->
                if (viewModel.balanceHidden) {
                    viewModel.onBalanceClick(viewItem)
                } else {
                    openSendScreen(
                        viewItem = viewItem,
                        view = view,
                        input = input,
                        navController = navController
                    )
                }
            },
            uiState = viewModel.uiState,
            updateFilter = viewModel::updateFilter,
            emptyItemsText = stringResource(R.string.Balance_NoAssetsToSend)
        )
    }

    private fun openSendScreen(
        viewItem: BalanceViewItem2,
        view: View,
        input: Input?,
        navController: NavController
    ) {
        when {
            viewItem.sendEnabled -> {
                val sendTitle = Translator.getString(
                    R.string.Send_Title,
                    viewItem.wallet.token.fullCoin.coin.code
                )
                navController.slideFromRight(
                    R.id.enterAddressFragment,
                    EnterAddressFragment.Input(
                        wallet = viewItem.wallet,
                        title = sendTitle,
                        sendEntryPointDestId = R.id.sendTokenSelectFragment,
                        address = input?.address,
                        amount = input?.amount,
                    )
                )
            }

            viewItem.syncingProgress.progress != null -> {
                HudHelper.showWarningMessage(view, R.string.Hud_WaitForSynchronization)
            }

            viewItem.errorMessage != null -> {
                HudHelper.showErrorMessage(view, viewItem.errorMessage)
            }
        }
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