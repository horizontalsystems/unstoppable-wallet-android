package io.horizontalsystems.bankwallet.modules.depositcex

import android.content.Intent
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.providers.CexAsset
import io.horizontalsystems.bankwallet.core.providers.CexDepositNetwork
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.receive.address.ReceiveAddressScreen
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.parcelize.Parcelize

class DepositCexFragment : BaseComposeFragment() {

    @Parcelize
    data class Input(val cexAsset: CexAsset, val network: CexDepositNetwork? = null) : Parcelable

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<Input>()
        val cexAsset = input?.cexAsset
        val network = input?.network

        if (cexAsset != null) {
            val networks = cexAsset.depositNetworks
            if (networks.isEmpty() || network != null || networks.size == 1) {
                val viewContent = LocalContext.current

                val viewModel =
                    viewModel<DepositAddressViewModel>(factory = DepositAddressViewModel.Factory(cexAsset, network))

                ReceiveAddressScreen(
                    title = stringResource(R.string.CexDeposit_Title, cexAsset.id),
                    coinName = cexAsset.name,
                    uiState = viewModel.uiState,
                    onErrorClick = { viewModel.onErrorClick() },
                    setAmount = { amount -> viewModel.setAmount(amount) },
                    navController = navController,
                    onShareClick = { address ->
                        viewContent.startActivity(Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, address)
                            type = "text/plain"
                        })
                    },
                )
            } else {
                val navigatedFromMain = navController.previousBackStackEntry?.destination?.id == R.id.mainFragment
                val navigateBack: () -> Unit = { navController.popBackStack() }
                SelectNetworkScreen(
                    networks = networks,
                    onNavigateBack = if (navigatedFromMain) null else navigateBack,
                    onClose = { navController.popBackStack(R.id.mainFragment, false) },
                    onSelectNetwork = {
                        navController.slideFromRight(R.id.depositCexFragment, Input(cexAsset, it))
                    }
                )
            }

        } else {
            val view = LocalView.current
            HudHelper.showErrorMessage(view, stringResource(id = R.string.Error_ParameterNotSet))
            navController.popBackStack()
        }
    }

}
