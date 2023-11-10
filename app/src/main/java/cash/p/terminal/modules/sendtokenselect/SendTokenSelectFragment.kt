package cash.p.terminal.modules.sendtokenselect

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.modules.send.SendFragment
import cash.p.terminal.modules.tokenselect.TokenSelectScreen
import cash.p.terminal.modules.tokenselect.TokenSelectViewModel
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType

class SendTokenSelectFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val blockchainTypes = arguments?.getParcelableArrayList<BlockchainType>(blockchainTypesKey)
        val address = arguments?.getString(addressKey)
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
                            SendFragment.prepareParams(
                                wallet = it.wallet,
                                sendEntryPointDestId = R.id.sendTokenSelectFragment,
                                title = sendTitle,
                                prefilledAddress = address,
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

    companion object {
        private const val blockchainTypesKey = "blockchainTypesKey"
        private const val addressKey = "addressKey"
        fun prepareParams(blockchainTypes: List<BlockchainType>? = null, address: String) = bundleOf(
            blockchainTypesKey to blockchainTypes,
            addressKey to address
        )

    }
}
