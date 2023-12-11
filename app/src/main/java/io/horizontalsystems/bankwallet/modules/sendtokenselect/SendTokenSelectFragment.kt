package io.horizontalsystems.bankwallet.modules.sendtokenselect

import android.os.Bundle
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.send.SendFragment
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
        val blockchainTypesUids = arguments?.getStringArrayList(blockchainTypesKey)
        val tokenTypesIds = arguments?.getStringArrayList(tokenTypesKey)
        val prefilledData = arguments?.getParcelable<PrefilledData>(addressDataKey)
        val view = LocalView.current
        val blockchainTypes = blockchainTypesUids?.mapNotNull { BlockchainType.fromUid(it) }
        val tokenTypes = tokenTypesIds?.mapNotNull { TokenType.fromId(it) }
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
            viewModel = viewModel(factory = TokenSelectViewModel.FactoryForSend(blockchainTypes, tokenTypes)),
            emptyItemsText = stringResource(R.string.Balance_NoAssetsToSend)
        )
    }

    companion object {
        private const val blockchainTypesKey = "blockchainTypesKey"
        private const val tokenTypesKey = "tokenTypesKey"
        private const val addressDataKey = "addressDataKey"

        fun prepareParams(
            blockchainTypes: List<BlockchainType>? = null,
            tokenTypes: List<TokenType>? = null,
            address: String,
            amount: BigDecimal?
        ) : Bundle {
            val blockchainTypesUids = blockchainTypes?.map { it.uid }
            val tokenTypesIds = tokenTypes?.map { it.id }
            return bundleOf(
                blockchainTypesKey to blockchainTypesUids,
                tokenTypesKey to tokenTypesIds,
                addressDataKey to PrefilledData(address, amount)
            )
        }

    }
}

@Parcelize
data class PrefilledData(
    val address: String,
    val amount: BigDecimal? = null,
) : Parcelable