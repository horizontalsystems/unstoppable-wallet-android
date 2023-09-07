package cash.p.terminal.modules.send

import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.amount.AmountInputModeModule
import cash.p.terminal.modules.amount.AmountInputModeViewModel
import cash.p.terminal.modules.send.binance.SendBinanceModule
import cash.p.terminal.modules.send.binance.SendBinanceScreen
import cash.p.terminal.modules.send.binance.SendBinanceViewModel
import cash.p.terminal.modules.send.bitcoin.SendBitcoinModule
import cash.p.terminal.modules.send.bitcoin.SendBitcoinNavHost
import cash.p.terminal.modules.send.bitcoin.SendBitcoinViewModel
import cash.p.terminal.modules.send.evm.SendEvmModule
import cash.p.terminal.modules.send.evm.SendEvmScreen
import cash.p.terminal.modules.send.evm.SendEvmViewModel
import cash.p.terminal.modules.send.evm.confirmation.EvmKitWrapperHoldingViewModel
import cash.p.terminal.modules.send.solana.SendSolanaModule
import cash.p.terminal.modules.send.solana.SendSolanaScreen
import cash.p.terminal.modules.send.solana.SendSolanaViewModel
import cash.p.terminal.modules.send.tron.SendTronModule
import cash.p.terminal.modules.send.tron.SendTronScreen
import cash.p.terminal.modules.send.tron.SendTronViewModel
import cash.p.terminal.modules.send.zcash.SendZCashModule
import cash.p.terminal.modules.send.zcash.SendZCashScreen
import cash.p.terminal.modules.send.zcash.SendZCashViewModel
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.parcelable
import io.horizontalsystems.marketkit.models.BlockchainType

class SendFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        val argumentsNonNull = arguments
        if (argumentsNonNull == null) {
            findNavController().popBackStack()
            return
        }
        val wallet = argumentsNonNull.parcelable<Wallet>(walletKey)
        if (wallet == null) {
            findNavController().popBackStack()
            return
        }
        val sendEntryPointDestId = argumentsNonNull.getInt(sendEntryPointDestIdKey)
        val predefinedAddress = argumentsNonNull.getString(predefinedAddressKey)

        val amountInputModeViewModel by navGraphViewModels<AmountInputModeViewModel>(R.id.sendXFragment) {
            AmountInputModeModule.Factory(wallet.coin.uid)
        }
        when (wallet.token.blockchainType) {
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.ECash,
            BlockchainType.Litecoin,
            BlockchainType.Dash -> {
                val factory = SendBitcoinModule.Factory(wallet, predefinedAddress)
                val sendBitcoinViewModel by navGraphViewModels<SendBitcoinViewModel>(R.id.sendXFragment) {
                    factory
                }
                SendBitcoinNavHost(
                    findNavController(),
                    sendBitcoinViewModel,
                    amountInputModeViewModel,
                    sendEntryPointDestId
                )
            }

            is BlockchainType.BinanceChain -> {
                val factory = SendBinanceModule.Factory(wallet, predefinedAddress)
                val sendBinanceViewModel by navGraphViewModels<SendBinanceViewModel>(R.id.sendXFragment) {
                    factory
                }
                SendBinanceScreen(
                    findNavController(),
                    sendBinanceViewModel,
                    amountInputModeViewModel,
                    sendEntryPointDestId
                )
            }

            BlockchainType.Zcash -> {
                val factory = SendZCashModule.Factory(wallet, predefinedAddress)
                val sendZCashViewModel by navGraphViewModels<SendZCashViewModel>(R.id.sendXFragment) {
                    factory
                }
                SendZCashScreen(
                    findNavController(),
                    sendZCashViewModel,
                    amountInputModeViewModel,
                    sendEntryPointDestId
                )
            }

            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Avalanche,
            BlockchainType.Optimism,
            BlockchainType.Gnosis,
            BlockchainType.Fantom,
            BlockchainType.ArbitrumOne -> {
                val factory = SendEvmModule.Factory(wallet, predefinedAddress)
                val evmKitWrapperViewModel by navGraphViewModels<EvmKitWrapperHoldingViewModel>(
                    R.id.sendXFragment
                ) { factory }
                val initiateLazyViewModel = evmKitWrapperViewModel //needed in SendEvmConfirmationFragment
                val sendEvmViewModel by navGraphViewModels<SendEvmViewModel>(R.id.sendXFragment) { factory }
                SendEvmScreen(
                    findNavController(),
                    sendEvmViewModel,
                    amountInputModeViewModel,
                    sendEntryPointDestId
                )
            }

            BlockchainType.Solana -> {
                val factory = SendSolanaModule.Factory(wallet, predefinedAddress)
                val sendSolanaViewModel by navGraphViewModels<SendSolanaViewModel>(R.id.sendXFragment) { factory }
                SendSolanaScreen(
                    findNavController(),
                    sendSolanaViewModel,
                    amountInputModeViewModel,
                    sendEntryPointDestId
                )
            }

            BlockchainType.Tron -> {
                val factory = SendTronModule.Factory(wallet, predefinedAddress)
                val sendTronViewModel by navGraphViewModels<SendTronViewModel>(R.id.sendXFragment) { factory }
                SendTronScreen(
                    findNavController(),
                    sendTronViewModel,
                    amountInputModeViewModel,
                    sendEntryPointDestId
                )
            }

            else -> {}
        }
    }

    companion object {
        private const val walletKey = "walletKey"
        private const val sendEntryPointDestIdKey = "sendEntryPointDestIdKey"
        private const val predefinedAddressKey = "predefinedAddressKey"

        fun prepareParams(wallet: Wallet) = bundleOf(
            walletKey to wallet
        )

        fun prepareParams(wallet: Wallet, sendEntryPointDestId: Int, predefinedAddress: String? = null) = bundleOf(
            walletKey to wallet,
            sendEntryPointDestIdKey to sendEntryPointDestId,
            predefinedAddressKey to predefinedAddress
        )
    }
}
