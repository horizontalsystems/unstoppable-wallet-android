package io.horizontalsystems.walletkit.modules.send

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ISendEthereumAdapter
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.amount.AmountInputModeModule
import io.horizontalsystems.walletkit.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.bitcoin.SendBitcoinModule
import io.horizontalsystems.walletkit.modules.send.bitcoin.SendBitcoinScreen
import io.horizontalsystems.walletkit.modules.send.bitcoin.SendBitcoinViewModel
import io.horizontalsystems.walletkit.modules.send.evm.SendEvmModule
import io.horizontalsystems.walletkit.modules.send.evm.SendEvmScreen
import io.horizontalsystems.walletkit.modules.send.evm.SendEvmViewModel
import io.horizontalsystems.walletkit.modules.send.solana.SendSolanaModule
import io.horizontalsystems.walletkit.modules.send.solana.SendSolanaScreen
import io.horizontalsystems.walletkit.modules.send.solana.SendSolanaViewModel
import io.horizontalsystems.walletkit.modules.send.stellar.SendStellarModule
import io.horizontalsystems.walletkit.modules.send.stellar.SendStellarScreen
import io.horizontalsystems.walletkit.modules.send.stellar.SendStellarViewModel
import io.horizontalsystems.walletkit.modules.send.thorchain.SendThorchainModule
import io.horizontalsystems.walletkit.modules.send.thorchain.SendThorchainScreen
import io.horizontalsystems.walletkit.modules.send.thorchain.SendThorchainViewModel
import io.horizontalsystems.walletkit.modules.send.ton.SendTonModule
import io.horizontalsystems.walletkit.modules.send.ton.SendTonScreen
import io.horizontalsystems.walletkit.modules.send.ton.SendTonViewModel
import io.horizontalsystems.walletkit.modules.send.tron.SendTronModule
import io.horizontalsystems.walletkit.modules.send.tron.SendTronScreen
import io.horizontalsystems.walletkit.modules.send.tron.SendTronViewModel
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.chain.ChainSendScreenArgs
import io.horizontalsystems.walletkit.serializers.BigDecimalSerializer
import io.horizontalsystems.walletkit.serializers.HSScreenKClassSerializer
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import kotlin.reflect.KClass

@Serializable
data class SendPage(val input: Input) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val wallet = input.wallet
        val title = input.title
        val sendEntryPointDestId = input.sendEntryPointDestId
        val address = input.address
        val riskyAddress = input.riskyAddress
        val hideAddress = input.hideAddress
        val amount = input.amount
        val memo = input.memo

        val amountInputModeViewModel = viewModel<AmountInputModeViewModel>(
            factory = AmountInputModeModule.Factory(wallet.coin.uid)
        )

        when (wallet.token.blockchainType) {
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.ECash,
            BlockchainType.Litecoin,
            BlockchainType.Dash -> {
                val factory = SendBitcoinModule.Factory(wallet, address, hideAddress)
                val sendBitcoinViewModel = viewModel<SendBitcoinViewModel>(factory = factory)
                SendBitcoinScreen(
                    title = title,
                    navigation = navigation,
                    viewModel = sendBitcoinViewModel,
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
                    navigation = navigation,
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
                val factory = SendSolanaModule.Factory(wallet, address, hideAddress)
                val sendSolanaViewModel = viewModel<SendSolanaViewModel>(factory = factory)
                SendSolanaScreen(
                    title = title,
                    navigation = navigation,
                    viewModel = sendSolanaViewModel,
                    amountInputModeViewModel = amountInputModeViewModel,
                    sendEntryPointDestId = sendEntryPointDestId,
                    amount = amount,
                    riskyAddress = riskyAddress
                )
            }

            BlockchainType.Ton -> {
                val factory = SendTonModule.Factory(wallet, address, hideAddress)
                val sendTonViewModel = viewModel<SendTonViewModel>(factory = factory)
                SendTonScreen(
                    title,
                    navigation,
                    sendTonViewModel,
                    amountInputModeViewModel,
                    sendEntryPointDestId,
                    amount,
                    riskyAddress = riskyAddress
                )
            }

            BlockchainType.Tron -> {
                val factory = SendTronModule.Factory(wallet, address, hideAddress)
                val sendTronViewModel = viewModel<SendTronViewModel>(factory = factory)
                SendTronScreen(
                    title = title,
                    navigation = navigation,
                    viewModel = sendTronViewModel,
                    amountInputModeViewModel = amountInputModeViewModel,
                    sendEntryPointDestId = sendEntryPointDestId,
                    amount = amount,
                    riskyAddress = riskyAddress
                )
            }

            BlockchainType.Stellar -> {
                val factory = SendStellarModule.Factory(wallet, address, hideAddress)
                val sendStellarViewModel = viewModel<SendStellarViewModel>(factory = factory)
                SendStellarScreen(
                    title,
                    navigation,
                    sendStellarViewModel,
                    amountInputModeViewModel,
                    sendEntryPointDestId,
                    amount,
                    riskyAddress = riskyAddress
                )
            }

            BlockchainType.Thorchain,
            BlockchainType.Mayachain -> {
                val factory = SendThorchainModule.Factory(wallet, address, hideAddress, memo)
                val sendThorchainViewModel = viewModel<SendThorchainViewModel>(factory = factory)
                SendThorchainScreen(
                    title,
                    navigation,
                    sendThorchainViewModel,
                    amountInputModeViewModel,
                    sendEntryPointDestId,
                    amount,
                    memo,
                    riskyAddress = riskyAddress
                )
            }


            else -> {
                ChainRegistry[wallet.token.blockchainType]?.SendScreen(
                    ChainSendScreenArgs(
                        wallet = wallet,
                        title = title,
                        navigation = navigation,
                        amountInputModeViewModel = amountInputModeViewModel,
                        sendEntryPointDestId = sendEntryPointDestId,
                        address = address,
                        amount = amount,
                        memo = memo,
                        hideAddress = hideAddress,
                        riskyAddress = riskyAddress,
                    )
                )
            }
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
