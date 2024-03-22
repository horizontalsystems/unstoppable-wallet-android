package cash.p.terminal.modules.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.requireInput
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
import cash.p.terminal.modules.send.ton.SendTonModule
import cash.p.terminal.modules.send.ton.SendTonScreen
import cash.p.terminal.modules.send.ton.SendTonViewModel
import cash.p.terminal.modules.send.tron.SendTronModule
import cash.p.terminal.modules.send.tron.SendTronScreen
import cash.p.terminal.modules.send.tron.SendTronViewModel
import cash.p.terminal.modules.send.zcash.SendZCashModule
import cash.p.terminal.modules.send.zcash.SendZCashScreen
import cash.p.terminal.modules.send.zcash.SendZCashViewModel
import cash.p.terminal.modules.sendtokenselect.PrefilledData
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.parcelable
import io.horizontalsystems.marketkit.models.BlockchainType

class SendFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            try {
                val arguments = requireArguments()
                val wallet = arguments.parcelable<Wallet>(walletKey) ?: throw IllegalStateException("Wallet is Null!")
                val title = arguments.getString(titleKey) ?: ""
                val sendEntryPointDestId = arguments.getInt(sendEntryPointDestIdKey)
                val predefinedAddress = arguments.getString(predefinedAddressKey)
                val prefilledData = arguments.getParcelable<PrefilledData>(prefilledAddressDataKey)

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
                        setContent {
                            SendBitcoinNavHost(
                                title,
                                findNavController(),
                                sendBitcoinViewModel,
                                amountInputModeViewModel,
                                sendEntryPointDestId,
                                prefilledData,
                            )
                        }
                    }

                    is BlockchainType.BinanceChain -> {
                        val factory = SendBinanceModule.Factory(wallet, predefinedAddress)
                        val sendBinanceViewModel by navGraphViewModels<SendBinanceViewModel>(R.id.sendXFragment) {
                            factory
                        }
                        setContent {
                            SendBinanceScreen(
                                title,
                                findNavController(),
                                sendBinanceViewModel,
                                amountInputModeViewModel,
                                sendEntryPointDestId,
                                prefilledData,
                            )
                        }
                    }

                    BlockchainType.Zcash -> {
                        val factory = SendZCashModule.Factory(wallet, predefinedAddress)
                        val sendZCashViewModel by navGraphViewModels<SendZCashViewModel>(R.id.sendXFragment) {
                            factory
                        }
                        setContent {
                            SendZCashScreen(
                                title,
                                findNavController(),
                                sendZCashViewModel,
                                amountInputModeViewModel,
                                sendEntryPointDestId,
                                prefilledData,
                            )
                        }
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
                        setContent {
                            SendEvmScreen(
                                title,
                                findNavController(),
                                sendEvmViewModel,
                                amountInputModeViewModel,
                                sendEntryPointDestId,
                                prefilledData,
                            )
                        }
                    }

                    BlockchainType.Solana -> {
                        val factory = SendSolanaModule.Factory(wallet, predefinedAddress)
                        val sendSolanaViewModel by navGraphViewModels<SendSolanaViewModel>(R.id.sendXFragment) { factory }
                        setContent {
                            SendSolanaScreen(
                                title,
                                findNavController(),
                                sendSolanaViewModel,
                                amountInputModeViewModel,
                                sendEntryPointDestId,
                                prefilledData,
                            )
                        }
                    }

                    BlockchainType.Ton -> {
                        val factory = SendTonModule.Factory(wallet, predefinedAddress)
                        val sendTonViewModel by navGraphViewModels<SendTonViewModel>(R.id.sendXFragment) { factory }
                        setContent {
                            SendTonScreen(
                                title,
                                findNavController(),
                                sendTonViewModel,
                                amountInputModeViewModel,
                                sendEntryPointDestId,
                                prefilledData,
                            )
                        }
                    }

                    BlockchainType.Tron -> {
                        val factory = SendTronModule.Factory(wallet, predefinedAddress)
                        val sendTronViewModel by navGraphViewModels<SendTronViewModel>(R.id.sendXFragment) { factory }
                        setContent {
                            SendTronScreen(
                                title,
                                findNavController(),
                                sendTronViewModel,
                                amountInputModeViewModel,
                                sendEntryPointDestId,
                                prefilledData,
                            )
                        }
                    }

                    else -> {}
                }
            } catch (t: Throwable) {
                findNavController().popBackStack()
            }
        }
    }

    companion object {
        private const val walletKey = "walletKey"
        private const val sendEntryPointDestIdKey = "sendEntryPointDestIdKey"
        private const val titleKey = "titleKey"
        private const val predefinedAddressKey = "predefinedAddressKey"
        private const val prefilledAddressDataKey = "predefilledAddressDataKey"

        fun prepareParams(wallet: Wallet, title: String) = bundleOf(
            walletKey to wallet,
            titleKey to title
        )

        fun prepareParams(
            wallet: Wallet,
            sendEntryPointDestId: Int,
            title: String,
            predefinedAddress: String? = null,
            prefilledAddressData: PrefilledData? = null,
        ) = bundleOf(
            walletKey to wallet,
            sendEntryPointDestIdKey to sendEntryPointDestId,
            titleKey to title,
            predefinedAddressKey to predefinedAddress,
            prefilledAddressDataKey to prefilledAddressData
        )
    }
}
