package io.horizontalsystems.bankwallet.modules.receivemain

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.receive.address.ReceiveAddressFragment
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class DerivationSelectFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        val navController = findNavController()
        val coinUid = arguments?.getString("coinUid")
        val popupDestinationId = arguments?.getInt(
            ReceiveAddressFragment.POPUP_DESTINATION_ID_KEY
        )

        if (coinUid == null) {
            HudHelper.showErrorMessage(LocalView.current, R.string.Error_ParameterNotSet)
            navController.popBackStack()
        } else {
            val viewModel = viewModel<DerivationSelectViewModel>(
                factory = DerivationSelectViewModel.Factory(coinUid)
            )
            AddressFormatSelectScreen(
                navController,
                viewModel.items,
                stringResource(R.string.Balance_Receive_AddressFormat_RecommendedDerivation),
                popupDestinationId
            )
        }
    }

    companion object {
        fun prepareParams(coinUid: String, popupDestinationId: Int?): Bundle {
            return bundleOf(
                "coinUid" to coinUid,
                ReceiveAddressFragment.POPUP_DESTINATION_ID_KEY to popupDestinationId
            )
        }
    }
}