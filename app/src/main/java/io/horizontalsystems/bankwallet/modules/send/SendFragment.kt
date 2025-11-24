package io.horizontalsystems.bankwallet.modules.send

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeModule
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinModule
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinNavHost
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinViewModel
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmScreen
import io.horizontalsystems.bankwallet.modules.send.monero.SendMoneroModule
import io.horizontalsystems.bankwallet.modules.send.monero.SendMoneroScreen
import io.horizontalsystems.bankwallet.modules.send.monero.SendMoneroViewModel
import io.horizontalsystems.bankwallet.modules.send.solana.SendSolanaModule
import io.horizontalsystems.bankwallet.modules.send.solana.SendSolanaScreen
import io.horizontalsystems.bankwallet.modules.send.solana.SendSolanaViewModel
import io.horizontalsystems.bankwallet.modules.send.stellar.SendStellarModule
import io.horizontalsystems.bankwallet.modules.send.stellar.SendStellarScreen
import io.horizontalsystems.bankwallet.modules.send.stellar.SendStellarViewModel
import io.horizontalsystems.bankwallet.modules.send.ton.SendTonModule
import io.horizontalsystems.bankwallet.modules.send.ton.SendTonScreen
import io.horizontalsystems.bankwallet.modules.send.ton.SendTonViewModel
import io.horizontalsystems.bankwallet.modules.send.tron.SendTronModule
import io.horizontalsystems.bankwallet.modules.send.tron.SendTronScreen
import io.horizontalsystems.bankwallet.modules.send.tron.SendTronViewModel
import io.horizontalsystems.bankwallet.modules.send.zcash.SendZCashModule
import io.horizontalsystems.bankwallet.modules.send.zcash.SendZCashScreen
import io.horizontalsystems.bankwallet.modules.send.zcash.SendZCashViewModel
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

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
                val navController = findNavController()
                val input = navController.requireInput<Input>()
                val wallet = input.wallet
                val title = input.title
                val sendEntryPointDestId = input.sendEntryPointDestId
                val address = input.address
                val riskyAddress = input.riskyAddress
                val hideAddress = input.hideAddress
                val amount = input.amount
                val memo = input.memo

                val amountInputModeViewModel by navGraphViewModels<AmountInputModeViewModel>(R.id.sendXFragment) {
                    AmountInputModeModule.Factory(wallet.coin.uid)
                }

                when (wallet.token.blockchainType) {
                    BlockchainType.Bitcoin,
                    BlockchainType.BitcoinCash,
                    BlockchainType.ECash,
                    BlockchainType.Litecoin,
                    BlockchainType.Dash -> {
                        val factory = SendBitcoinModule.Factory(wallet, address, hideAddress)
                        val sendBitcoinViewModel by navGraphViewModels<SendBitcoinViewModel>(R.id.sendXFragment) {
                            factory
                        }
                        setContent {
                            SendBitcoinNavHost(
                                title = title,
                                fragmentNavController = findNavController(),
                                viewModel = sendBitcoinViewModel,
                                amountInputModeViewModel = amountInputModeViewModel,
                                sendEntryPointDestId = sendEntryPointDestId,
                                amount = amount,
                                riskyAddress = riskyAddress
                            )
                        }
                    }

                    BlockchainType.Zcash -> {
                        val factory = SendZCashModule.Factory(wallet, address, hideAddress)
                        val sendZCashViewModel by navGraphViewModels<SendZCashViewModel>(R.id.sendXFragment) {
                            factory
                        }
                        setContent {
                            SendZCashScreen(
                                title = title,
                                navController = findNavController(),
                                viewModel = sendZCashViewModel,
                                amountInputModeViewModel = amountInputModeViewModel,
                                sendEntryPointDestId = sendEntryPointDestId,
                                amount = amount,
                                riskyAddress = riskyAddress
                            )
                        }
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
                        setContent {
                            SendEvmScreen(
                                title = title,
                                navController = findNavController(),
                                amountInputModeViewModel = amountInputModeViewModel,
                                address = address,
                                wallet = wallet,
                                amount = amount,
                                hideAddress = hideAddress,
                                riskyAddress = riskyAddress,
                                sendEntryPointDestId = sendEntryPointDestId
                            )
                        }
                    }

                    BlockchainType.Solana -> {
                        val factory = SendSolanaModule.Factory(wallet, address, hideAddress)
                        val sendSolanaViewModel by navGraphViewModels<SendSolanaViewModel>(R.id.sendXFragment) { factory }
                        setContent {
                            SendSolanaScreen(
                                title = title,
                                navController = findNavController(),
                                viewModel = sendSolanaViewModel,
                                amountInputModeViewModel = amountInputModeViewModel,
                                sendEntryPointDestId = sendEntryPointDestId,
                                amount = amount,
                                riskyAddress = riskyAddress
                            )
                        }
                    }

                    BlockchainType.Ton -> {
                        val factory = SendTonModule.Factory(wallet, address, hideAddress)
                        val sendTonViewModel by navGraphViewModels<SendTonViewModel>(R.id.sendXFragment) { factory }
                        setContent {
                            SendTonScreen(
                                title,
                                findNavController(),
                                sendTonViewModel,
                                amountInputModeViewModel,
                                sendEntryPointDestId,
                                amount,
                                riskyAddress = riskyAddress
                            )
                        }
                    }

                    BlockchainType.Tron -> {
                        val factory = SendTronModule.Factory(wallet, address, hideAddress)
                        val sendTronViewModel by navGraphViewModels<SendTronViewModel>(R.id.sendXFragment) { factory }
                        setContent {
                            SendTronScreen(
                                title = title,
                                navController = findNavController(),
                                viewModel = sendTronViewModel,
                                amountInputModeViewModel = amountInputModeViewModel,
                                sendEntryPointDestId = sendEntryPointDestId,
                                amount = amount,
                                riskyAddress = riskyAddress
                            )
                        }
                    }

                    BlockchainType.Stellar -> {
                        val factory = SendStellarModule.Factory(wallet, address, hideAddress)
                        val sendStellarViewModel by navGraphViewModels<SendStellarViewModel>(R.id.sendXFragment) { factory }
                        setContent {
                            SendStellarScreen(
                                title,
                                findNavController(),
                                sendStellarViewModel,
                                amountInputModeViewModel,
                                sendEntryPointDestId,
                                amount,
                                riskyAddress = riskyAddress
                            )
                        }
                    }

                    BlockchainType.Monero -> {
                        val factory = SendMoneroModule.Factory(wallet, address, hideAddress)
                        val sendMoneroViewModel by navGraphViewModels<SendMoneroViewModel>(R.id.sendXFragment) { factory }
                        setContent {
                            SendMoneroScreen(
                                title,
                                findNavController(),
                                sendMoneroViewModel,
                                amountInputModeViewModel,
                                sendEntryPointDestId,
                                amount,
                                memo,
                                riskyAddress = riskyAddress
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

    @Parcelize
    data class Input(
        val wallet: Wallet,
        val title: String,
        val sendEntryPointDestId: Int,
        val address: Address,
        val riskyAddress: Boolean = false,
        val amount: BigDecimal? = null,
        val hideAddress: Boolean = false,
        val memo: String? = null,
    ) : Parcelable
}
