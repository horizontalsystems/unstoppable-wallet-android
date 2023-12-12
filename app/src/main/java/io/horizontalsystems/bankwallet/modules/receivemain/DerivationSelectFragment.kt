package io.horizontalsystems.bankwallet.modules.receivemain

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.parcelize.Parcelize

class DerivationSelectFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<Input>()
        if (input == null) {
            HudHelper.showErrorMessage(LocalView.current, R.string.Error_ParameterNotSet)
            navController.popBackStack()
        } else {
            val viewModel = viewModel<DerivationSelectViewModel>(
                factory = DerivationSelectViewModel.Factory(input.coinUid)
            )
            AddressFormatSelectScreen(
                navController,
                viewModel.items,
                stringResource(R.string.Balance_Receive_AddressFormat_RecommendedDerivation)
            )
        }
    }

    @Parcelize
    data class Input(val coinUid: String) : Parcelable
}