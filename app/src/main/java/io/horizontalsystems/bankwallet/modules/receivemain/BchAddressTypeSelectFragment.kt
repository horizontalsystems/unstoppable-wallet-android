package io.horizontalsystems.bankwallet.modules.receivemain

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.core.helpers.HudHelper

class BchAddressTypeSelectFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val coinUid = arguments?.getString("coinUid")
        
        if (coinUid == null) {
            HudHelper.showErrorMessage(LocalView.current, R.string.Error_ParameterNotSet)
            navController.popBackStack()
        } else {
            val viewModel = viewModel<BchAddressTypeSelectViewModel>(
                factory = BchAddressTypeSelectViewModel.Factory(coinUid)
            )
            AddressFormatSelectScreen(
                navController,
                viewModel.items,
                stringResource(R.string.Balance_Receive_AddressFormat_RecommendedAddressType),
            )
        }
    }

    companion object {
        fun prepareParams(coinUid: String): Bundle {
            return bundleOf("coinUid" to coinUid)
        }
    }
}
