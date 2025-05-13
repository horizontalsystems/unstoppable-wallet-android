package io.horizontalsystems.bankwallet.modules.receive

import android.content.Intent
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromBottomForResult
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.activatetoken.ActivateTokenFragment
import io.horizontalsystems.bankwallet.modules.receive.ui.ReceiveAddressScreen
import io.horizontalsystems.bankwallet.modules.receive.ui.UsedAddressesParams
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.ReceiveAddressViewModel
import kotlinx.parcelize.Parcelize

class ReceiveFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) {
            ReceiveScreen(navController, it.wallet, it.receiveEntryPointDestId)
        }
    }

    @Parcelize
    data class Input(val wallet: Wallet, val receiveEntryPointDestId: Int = 0) : Parcelable

}

@Composable
fun ReceiveScreen(navController: NavController, wallet: Wallet, receiveEntryPointDestId: Int) {
    val addressViewModel = viewModel<ReceiveAddressViewModel>(factory = ReceiveModule.Factory(wallet))
    val context = LocalContext.current

    ReceiveAddressScreen(
        title = stringResource(R.string.Deposit_Title, wallet.coin.code),
        uiState = addressViewModel.uiState,
        onErrorClick = { addressViewModel.onErrorClick() },
        setAmount = { amount -> addressViewModel.setAmount(amount) },
        onShareClick = { address ->
            context.startActivity(Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, address)
                type = "text/plain"
            })
        },
        showUsedAddresses = { usedAddresses, usedChangeAddresses ->
            navController.slideFromRight(
                R.id.btcUsedAddressesFragment,
                UsedAddressesParams(
                    wallet.coin.name,
                    usedAddresses,
                    usedChangeAddresses
                )
            )
        },
        onBackPress = { navController.popBackStack() },
        closeModule = {
            if (receiveEntryPointDestId == 0) {
                navController.popBackStack()
            } else {
                navController.popBackStack(receiveEntryPointDestId, true)
            }
        },
        onClickActivate = {
            navController.slideFromBottomForResult<ActivateTokenFragment.Result>(
                R.id.activateTokenFragment,
                wallet
            ) {
                addressViewModel.onActivatedResult(it.activated)
            }
        }
    )
}
