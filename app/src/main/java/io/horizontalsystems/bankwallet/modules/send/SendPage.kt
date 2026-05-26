package io.horizontalsystems.bankwallet.modules.send

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeModule
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinScreen
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinViewModel
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmScreen
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmViewModel
import io.horizontalsystems.bankwallet.modules.send.monero.SendMoneroScreen
import io.horizontalsystems.bankwallet.modules.send.monero.SendMoneroViewModel
import io.horizontalsystems.bankwallet.modules.send.solana.SendSolanaScreen
import io.horizontalsystems.bankwallet.modules.send.solana.SendSolanaViewModel
import io.horizontalsystems.bankwallet.modules.send.stellar.SendStellarScreen
import io.horizontalsystems.bankwallet.modules.send.stellar.SendStellarViewModel
import io.horizontalsystems.bankwallet.modules.send.ton.SendTonScreen
import io.horizontalsystems.bankwallet.modules.send.ton.SendTonViewModel
import io.horizontalsystems.bankwallet.modules.send.tron.SendTronScreen
import io.horizontalsystems.bankwallet.modules.send.tron.SendTronViewModel
import io.horizontalsystems.bankwallet.modules.send.zano.SendZanoScreen
import io.horizontalsystems.bankwallet.modules.send.zano.SendZanoViewModel
import io.horizontalsystems.bankwallet.modules.send.zcash.SendZCashScreen
import io.horizontalsystems.bankwallet.modules.send.zcash.SendZCashViewModel
import io.horizontalsystems.bankwallet.serializers.BigDecimalSerializer
import io.horizontalsystems.bankwallet.serializers.HSScreenKClassSerializer
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import kotlin.reflect.KClass

@Serializable
data class SendPage(val input: Input) : HSPage() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        val wallet = input.wallet
        val title = input.title
        val sendEntryPointDestId = input.sendEntryPointDestId
        val address = input.address
        val riskyAddress = input.riskyAddress
        val hideAddress = input.hideAddress
        val amount = input.amount
        val memo = input.memo

        val amountInputModeViewModel = hiltViewModel<AmountInputModeViewModel, AmountInputModeViewModel.Factory> { factory ->
            factory.create(wallet.coin.uid)
        }

        when (wallet.token.blockchainType) {
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.ECash,
            BlockchainType.Litecoin,
            BlockchainType.Dash -> {
                val sendBitcoinViewModel = hiltViewModel<SendBitcoinViewModel, SendBitcoinViewModel.Factory> { factory ->
                    factory.create(wallet, address, hideAddress)
                }
                SendBitcoinScreen(
                    title = title,
                    fragmentNavController = navController,
                    viewModel = sendBitcoinViewModel,
                    amountInputModeViewModel = amountInputModeViewModel,
                    sendEntryPointDestId = sendEntryPointDestId,
                    amount = amount,
                    riskyAddress = riskyAddress
                )
            }

            BlockchainType.Zcash -> {
                val sendZCashViewModel = hiltViewModel<SendZCashViewModel, SendZCashViewModel.Factory> { factory ->
                    factory.create(wallet, address, hideAddress)
                }
                SendZCashScreen(
                    title = title,
                    navController = navController,
                    viewModel = sendZCashViewModel,
                    amountInputModeViewModel = amountInputModeViewModel,
                    sendEntryPointDestId = sendEntryPointDestId,
                    amount = amount,
                    riskyAddress = riskyAddress
                )
            }

            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Avalanche,
            BlockchainType.Optimism,
            BlockchainType.Base,
            BlockchainType.ZkSync,
            BlockchainType.Gnosis,
            BlockchainType.Fantom,
            BlockchainType.ArbitrumOne -> {
                val adapter = App.adapterManager.getAdapterForWallet<ISendEthereumAdapter>(wallet) ?: throw IllegalArgumentException("SendEthereumAdapter is null")

                val sendEvmViewModel = viewModel<SendEvmViewModel>(
                    factory = SendEvmModule.Factory(wallet, address, hideAddress, adapter)
                )

                SendEvmScreen(
                    title = title,
                    navController = navController,
                    amountInputModeViewModel = amountInputModeViewModel,
                    viewModel = sendEvmViewModel,
                    address = address,
                    wallet = wallet,
                    amount = amount,
                    hideAddress = hideAddress,
                    riskyAddress = riskyAddress,
                    sendEntryPointDestId = sendEntryPointDestId
                )
            }

            BlockchainType.Solana -> {
                val sendSolanaViewModel = hiltViewModel<SendSolanaViewModel, SendSolanaViewModel.Factory> { factory ->
                    factory.create(wallet, address, hideAddress)
                }
                SendSolanaScreen(
                    title = title,
                    navController = navController,
                    viewModel = sendSolanaViewModel,
                    amountInputModeViewModel = amountInputModeViewModel,
                    sendEntryPointDestId = sendEntryPointDestId,
                    amount = amount,
                    riskyAddress = riskyAddress
                )
            }

            BlockchainType.Ton -> {
                val sendTonViewModel = hiltViewModel<SendTonViewModel, SendTonViewModel.Factory> { factory ->
                    factory.create(wallet, address, hideAddress)
                }
                SendTonScreen(
                    title,
                    navController,
                    sendTonViewModel,
                    amountInputModeViewModel,
                    sendEntryPointDestId,
                    amount,
                    riskyAddress = riskyAddress
                )
            }

            BlockchainType.Tron -> {
                val sendTronViewModel = hiltViewModel<SendTronViewModel, SendTronViewModel.Factory> { factory ->
                    factory.create(wallet, address, hideAddress)
                }
                SendTronScreen(
                    title = title,
                    navController = navController,
                    viewModel = sendTronViewModel,
                    amountInputModeViewModel = amountInputModeViewModel,
                    sendEntryPointDestId = sendEntryPointDestId,
                    amount = amount,
                    riskyAddress = riskyAddress
                )
            }

            BlockchainType.Stellar -> {
                val sendStellarViewModel = hiltViewModel<SendStellarViewModel, SendStellarViewModel.Factory> { factory ->
                    factory.create(wallet, address, hideAddress)
                }
                SendStellarScreen(
                    title,
                    navController,
                    sendStellarViewModel,
                    amountInputModeViewModel,
                    sendEntryPointDestId,
                    amount,
                    riskyAddress = riskyAddress
                )
            }

            BlockchainType.Monero -> {
                val sendMoneroViewModel = hiltViewModel<SendMoneroViewModel, SendMoneroViewModel.Factory> { factory ->
                    factory.create(wallet, address, hideAddress)
                }
                SendMoneroScreen(
                    title,
                    navController,
                    sendMoneroViewModel,
                    amountInputModeViewModel,
                    sendEntryPointDestId,
                    amount,
                    memo,
                    riskyAddress = riskyAddress
                )
            }

            BlockchainType.Zano -> {
                val sendZanoViewModel = hiltViewModel<SendZanoViewModel, SendZanoViewModel.Factory> { factory ->
                    factory.create(wallet, address, hideAddress)
                }
                SendZanoScreen(
                    title,
                    navController,
                    sendZanoViewModel,
                    amountInputModeViewModel,
                    sendEntryPointDestId,
                    amount,
                    memo,
                    riskyAddress = riskyAddress
                )
            }

            else -> {}
        }
    }

    @Serializable
    data class Input(
        val wallet: Wallet,
        val title: String,
        @Serializable(with = HSScreenKClassSerializer::class) val sendEntryPointDestId: KClass<out HSPage>,
        val address: Address,
        val riskyAddress: Boolean = false,
        @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal? = null,
        val hideAddress: Boolean = false,
        val memo: String? = null,
    )
}
