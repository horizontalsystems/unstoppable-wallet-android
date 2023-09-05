package io.horizontalsystems.bankwallet.modules.send

import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeModule
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.send.binance.SendBinanceModule
import io.horizontalsystems.bankwallet.modules.send.binance.SendBinanceScreen
import io.horizontalsystems.bankwallet.modules.send.binance.SendBinanceViewModel
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinModule
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinNavHost
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinViewModel
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmScreen
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmViewModel
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.EvmKitWrapperHoldingViewModel
import io.horizontalsystems.bankwallet.modules.send.solana.SendSolanaModule
import io.horizontalsystems.bankwallet.modules.send.solana.SendSolanaScreen
import io.horizontalsystems.bankwallet.modules.send.solana.SendSolanaViewModel
import io.horizontalsystems.bankwallet.modules.send.tron.SendTronModule
import io.horizontalsystems.bankwallet.modules.send.tron.SendTronScreen
import io.horizontalsystems.bankwallet.modules.send.tron.SendTronViewModel
import io.horizontalsystems.bankwallet.modules.send.zcash.SendZCashModule
import io.horizontalsystems.bankwallet.modules.send.zcash.SendZCashScreen
import io.horizontalsystems.bankwallet.modules.send.zcash.SendZCashViewModel
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

        val amountInputModeViewModel by navGraphViewModels<AmountInputModeViewModel>(R.id.sendXFragment) {
            AmountInputModeModule.Factory(wallet.coin.uid)
        }
        when (wallet.token.blockchainType) {
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.ECash,
            BlockchainType.Litecoin,
            BlockchainType.Dash -> {
                val factory = SendBitcoinModule.Factory(wallet)
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
                val factory = SendBinanceModule.Factory(wallet)
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
                val factory = SendZCashModule.Factory(wallet)
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
                val factory = SendEvmModule.Factory(wallet)
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
                val factory = SendSolanaModule.Factory(wallet)
                val sendSolanaViewModel by navGraphViewModels<SendSolanaViewModel>(R.id.sendXFragment) { factory }
                SendSolanaScreen(
                    findNavController(),
                    sendSolanaViewModel,
                    amountInputModeViewModel,
                    sendEntryPointDestId
                )
            }

            BlockchainType.Tron -> {
                val factory = SendTronModule.Factory(wallet)
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

        fun prepareParams(wallet: Wallet) = bundleOf(
            walletKey to wallet
        )

        fun prepareParams(wallet: Wallet, sendEntryPointDestId: Int) = bundleOf(
            walletKey to wallet,
            sendEntryPointDestIdKey to sendEntryPointDestId,
        )
    }
}
