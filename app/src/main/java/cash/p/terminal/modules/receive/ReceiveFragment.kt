package cash.p.terminal.modules.receive

import android.os.Parcelable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import cash.p.terminal.R
import cash.p.terminal.modules.receive.ui.ReceiveAddressScreen
import cash.p.terminal.modules.receive.ui.UsedAddressesParams
import cash.p.terminal.modules.receive.viewmodels.ReceiveAddressViewModel
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.components.HsDivider
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.parcelize.Parcelize

class ReceiveFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) {
            val wallet = it.wallet
            val token = wallet.token
            when (token.blockchainType) {
                BlockchainType.Stellar -> {
                    if (token.type is TokenType.Asset) {
                        ReceiveStellarAssetScreen(navController, wallet, it.receiveEntryPointDestId)
                    } else if (token.type == TokenType.Native) {
                        ReceiveScreen(
                            navController,
                            wallet,
                            it.receiveEntryPointDestId
                        )
                    }
                }
//        BlockchainType.ArbitrumOne -> TODO()
//        BlockchainType.Avalanche -> TODO()
//        BlockchainType.Base -> TODO()
//        BlockchainType.BinanceSmartChain -> TODO()
//        BlockchainType.Bitcoin -> TODO()
//        BlockchainType.BitcoinCash -> TODO()
//        BlockchainType.Dash -> TODO()
//        BlockchainType.ECash -> TODO()
//        BlockchainType.Ethereum -> TODO()
//        BlockchainType.Fantom -> TODO()
//        BlockchainType.Gnosis -> TODO()
//        BlockchainType.Litecoin -> TODO()
//        BlockchainType.Optimism -> TODO()
//        BlockchainType.Polygon -> TODO()
//        BlockchainType.Solana -> TODO()
//        BlockchainType.Ton -> TODO()
//        BlockchainType.Tron -> TODO()
//        is BlockchainType.Unsupported -> TODO()
//        BlockchainType.Zcash -> TODO()
//        BlockchainType.ZkSync -> TODO()
                else -> {
                    ReceiveScreen(
                        navController,
                        wallet,
                        it.receiveEntryPointDestId
                    )
                }
            }
        }
    }

    @Parcelize
    data class Input(val wallet: Wallet, val receiveEntryPointDestId: Int = 0) : Parcelable

}

@Composable
fun ReceiveScreen(navController: NavController, wallet: Wallet, receiveEntryPointDestId: Int) {
    val addressViewModel =
        viewModel<ReceiveAddressViewModel>(factory = ReceiveModule.Factory(wallet))

    val uiState = addressViewModel.uiState
    ReceiveAddressScreen(
        title = stringResource(R.string.Deposit_Title, wallet.coin.code),
        uiState = uiState,
        setAmount = { amount -> addressViewModel.setAmount(amount) },
        onErrorClick = { addressViewModel.onErrorClick() },
        slot1 = {
            if (uiState.usedAddresses.isNotEmpty()) {
                HsDivider(modifier = Modifier.fillMaxWidth())
                RowUniversal(
                    modifier = Modifier.height(48.dp),
                    onClick = {
                        navController.slideFromRight(
                            R.id.btcUsedAddressesFragment,
                            UsedAddressesParams(
                                wallet.coin.name,
                                uiState.usedAddresses,
                                uiState.usedChangeAddresses
                            )
                        )
                    }
                ) {
                    subhead2_grey(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .weight(1f),
                        text = stringResource(R.string.Balance_Receive_UsedAddresses),
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

@Composable
inline fun <reified T : ViewModel> NavBackStackEntry.sharedViewModel(
    navController: NavHostController,
): T {
    val navGraphRoute = destination.parent?.route ?: return viewModel()
    val parentEntry = remember(this) {
        navController.getBackStackEntry(navGraphRoute)
    }
    return viewModel(parentEntry)
}