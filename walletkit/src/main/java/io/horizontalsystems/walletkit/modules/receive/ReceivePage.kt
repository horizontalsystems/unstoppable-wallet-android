package io.horizontalsystems.walletkit.modules.receive

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
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.receive.monero.ReceiveMoneroScreen
import io.horizontalsystems.walletkit.modules.receive.ui.ReceiveAddressScreen
import io.horizontalsystems.walletkit.modules.receive.ui.UsedAddressesParams
import io.horizontalsystems.walletkit.modules.receive.viewmodels.ReceiveAddressViewModel
import io.horizontalsystems.walletkit.serializers.HSScreenKClassSerializer
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
import io.horizontalsystems.walletkit.ui.compose.components.RowUniversal
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_grey
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class ReceivePage(val input: Input) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val wallet = input.wallet
        val token = wallet.token
        when (token.blockchainType) {
            BlockchainType.Stellar -> {
                if (token.type is TokenType.Asset) {
                    ReceiveStellarAssetScreen(navigation, wallet, input.receiveEntryPointDestId)
                } else if (token.type == TokenType.Native) {
                    ReceiveScreen(navigation, wallet, input.receiveEntryPointDestId, input.isTransparentAddress)
                }
            }

            BlockchainType.Monero -> {
                ReceiveMoneroScreen(navigation, wallet, input.receiveEntryPointDestId)
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
                    ReceiveScreen(navigation, wallet, input.receiveEntryPointDestId, input.isTransparentAddress)
                }
            }
    }

    @Serializable
    data class Input(
        val wallet: Wallet,
        @Serializable(with = HSScreenKClassSerializer::class) val receiveEntryPointDestId: KClass<out HSPage>? = null,
        val isTransparentAddress: Boolean = false
    )
}

@Composable
fun ReceiveScreen(
    navigation: HSNavigation,
    wallet: Wallet,
    receiveEntryPointDestId: KClass<out HSPage>?,
    isTransparentAddress: Boolean,
) {
    val addressViewModel =
        viewModel<ReceiveAddressViewModel>(factory = ReceiveModule.Factory(wallet, isTransparentAddress))

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
                    modifier = Modifier.height(52.dp),
                    onClick = {
                        navigation.slideFromRight(
                            BtcUsedAddressesPage(UsedAddressesParams(
                                wallet.coin.name,
                                uiState.usedAddresses,
                                uiState.usedChangeAddresses
                            ))
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
        onBackPress = { navigation.removeLastOrNull() },
        closeModule = if (receiveEntryPointDestId == null) {
            null
        } else {
            { navigation.removeLastUntil(receiveEntryPointDestId, true) }
        }
    )
}
