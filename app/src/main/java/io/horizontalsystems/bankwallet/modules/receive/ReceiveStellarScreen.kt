package io.horizontalsystems.bankwallet.modules.receive

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.receive.ui.ReceiveAddressScreen

@Composable
fun ReceiveStellarScreen(navController: NavController, wallet: Wallet, receiveEntryPointDestId: Int) {
    val viewModel = viewModel<ReceiveStellarViewModel>(factory = ReceiveStellarViewModel.Factory(wallet))
    val uiState = viewModel.uiState

    val context = LocalContext.current

    ReceiveAddressScreen(
        title = stringResource(R.string.Deposit_Title, wallet.coin.code),
        uiState = uiState,
        setAmount = viewModel::setAmount,
        onErrorClick = viewModel::onErrorClick,
        onShareClick = { address ->
            context.startActivity(Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, address)
                type = "text/plain"
            })
        },
        onBackPress = { navController.popBackStack() },
        closeModule = {
            if (receiveEntryPointDestId == 0) {
                navController.popBackStack()
            } else {
                navController.popBackStack(receiveEntryPointDestId, true)
            }
        }
    )
}

//      ReceiveTokenActivationRequired(onClickActivate)

//        onClickActivate = {
//            navController.slideFromBottomForResult<ActivateTokenFragment.Result>(
//                R.id.activateTokenFragment,
//                wallet
//            ) {
//                addressViewModel.onActivatedResult(it.activated)
//            }
//        }