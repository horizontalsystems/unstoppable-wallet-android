package com.quantum.wallet.bankwallet.modules.send

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.BaseFragment
import com.quantum.wallet.bankwallet.core.ISendEthereumAdapter
import com.quantum.wallet.bankwallet.core.requireInput
import com.quantum.wallet.bankwallet.entities.Address
import com.quantum.wallet.bankwallet.entities.Wallet
import com.quantum.wallet.bankwallet.modules.amount.AmountInputModeModule
import com.quantum.wallet.bankwallet.modules.amount.AmountInputModeViewModel
import com.quantum.wallet.bankwallet.modules.send.bitcoin.SendBitcoinModule
import com.quantum.wallet.bankwallet.modules.send.bitcoin.SendBitcoinNavHost
import com.quantum.wallet.bankwallet.modules.send.bitcoin.SendBitcoinViewModel
import com.quantum.wallet.bankwallet.modules.send.evm.SendEvmModule
import com.quantum.wallet.bankwallet.modules.send.evm.SendEvmScreen
import com.quantum.wallet.bankwallet.modules.send.evm.SendEvmViewModel
import com.quantum.wallet.bankwallet.modules.send.monero.SendMoneroModule
import com.quantum.wallet.bankwallet.modules.send.monero.SendMoneroScreen
import com.quantum.wallet.bankwallet.modules.send.monero.SendMoneroViewModel
import com.quantum.wallet.bankwallet.modules.send.solana.SendSolanaModule
import com.quantum.wallet.bankwallet.modules.send.solana.SendSolanaScreen
import com.quantum.wallet.bankwallet.modules.send.solana.SendSolanaViewModel
import com.quantum.wallet.bankwallet.modules.send.stellar.SendStellarModule
import com.quantum.wallet.bankwallet.modules.send.stellar.SendStellarScreen
import com.quantum.wallet.bankwallet.modules.send.stellar.SendStellarViewModel
import com.quantum.wallet.bankwallet.modules.send.ton.SendTonModule
import com.quantum.wallet.bankwallet.modules.send.ton.SendTonScreen
import com.quantum.wallet.bankwallet.modules.send.ton.SendTonViewModel
import com.quantum.wallet.bankwallet.modules.send.tron.SendTronModule
import com.quantum.wallet.bankwallet.modules.send.tron.SendTronScreen
import com.quantum.wallet.bankwallet.modules.send.tron.SendTronViewModel
import com.quantum.wallet.bankwallet.modules.send.zcash.SendZCashModule
import com.quantum.wallet.bankwallet.modules.send.zcash.SendZCashScreen
import com.quantum.wallet.bankwallet.modules.send.zcash.SendZCashViewModel
import com.quantum.wallet.core.findNavController
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
                        val adapter = App.adapterManager.getAdapterForWallet<ISendEthereumAdapter>(wallet) ?: throw IllegalArgumentException("SendEthereumAdapter is null")

                        val sendEvmViewModel by viewModels<SendEvmViewModel> {
                            SendEvmModule.Factory(wallet, address, hideAddress, adapter)
                        }

                        setContent {
                            SendEvmScreen(
                                title = title,
                                navController = findNavController(),
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
