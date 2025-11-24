package io.horizontalsystems.bankwallet.modules.receive.monero

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.receive.ui.ReceiveAddressScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey

@Composable
fun ReceiveMoneroScreen(navController: NavController, wallet: Wallet, receiveEntryPointDestId: Int) {
    val addressViewModel = viewModel<ReceiveMoneroAddressViewModel>(factory = ReceiveMoneroAddressViewModel.Factory(wallet))

    val uiState = addressViewModel.uiState
    ReceiveAddressScreen(
        title = stringResource(R.string.Deposit_Title, wallet.coin.code),
        uiState = uiState,
        setAmount = { amount -> addressViewModel.setAmount(amount) },
        onErrorClick = { addressViewModel.onErrorClick() },
        slot1 = {
            if (uiState.subaddresses.isNotEmpty()) {
                HsDivider(modifier = Modifier.fillMaxWidth())
                RowUniversal(
                    modifier = Modifier.height(52.dp),
                    onClick = {
                        navController.slideFromRight(
                            R.id.moneroSubaddressesFragment,
                            SubaddressesParams(uiState.subaddresses)
                        )
                    }
                ) {
                    subhead2_grey(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .weight(1f),
                        text = stringResource(R.string.Balance_Receive_Subaddresses),
                    )

                    Icon(
                        modifier = Modifier.padding(end = 16.dp),
                        painter = painterResource(id = R.drawable.ic_arrow_right),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.grey
                    )
                }
            }
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
