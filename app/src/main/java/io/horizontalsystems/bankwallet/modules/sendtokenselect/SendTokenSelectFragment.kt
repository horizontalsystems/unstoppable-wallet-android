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
import io.horizontalsystems.bankwallet.modules.send.SendFragment
import io.horizontalsystems.bankwallet.modules.tokenselect.TokenSelectScreen
import io.horizontalsystems.bankwallet.modules.tokenselect.TokenSelectViewModel
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

class SendTokenSelectFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<Input>()

        val blockchainTypes = input?.blockchainTypes
        val prefilledData = input?.prefilledData
        val view = LocalView.current
        TokenSelectScreen(
            navController = navController,
            title = stringResource(R.string.Balance_Send),
            onClickItem = {
                when {
                    it.sendEnabled -> {
                        val sendTitle = Translator.getString(R.string.Send_Title, it.wallet.token.fullCoin.coin.code)
                        navController.slideFromRight(
                            R.id.sendXFragment,
                            SendFragment.Input(
                                wallet = it.wallet,
                                title = sendTitle,
                                sendEntryPointDestId = R.id.sendTokenSelectFragment,
                                prefilledAddressData = prefilledData,
                            )
                        )
                    }

                    it.syncingProgress.progress != null -> {
                        HudHelper.showWarningMessage(view, R.string.Hud_WaitForSynchronization)
                    }

                    it.errorMessage != null -> {
                        HudHelper.showErrorMessage(view, it.errorMessage ?: "")
                    }
                }
            },
            viewModel = viewModel(factory = TokenSelectViewModel.FactoryForSend(blockchainTypes)),
            emptyItemsText = stringResource(R.string.Balance_NoAssetsToSend)
        )
    }

    @Parcelize
    data class Input(
        val blockchainTypes: List<BlockchainType>,
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