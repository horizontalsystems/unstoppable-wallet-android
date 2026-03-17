package io.horizontalsystems.bankwallet.modules.send

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeModule
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinModule
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinScreen
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinViewModel
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmScreen
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmViewModel
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
import io.horizontalsystems.bankwallet.serializers.BigDecimalSerializer
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import kotlin.reflect.KClass

@Serializable
data class SendScreen(
    val wallet: Wallet,
    val title: String,
    val sendEntryPointDestId: KClass<out HSScreen>,
    val address: Address,
    val riskyAddress: Boolean = false,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal? = null,
    val hideAddress: Boolean = false,
    val memo: String? = null,
) : HSScreen() {
    @Composable
    override fun GetContent(backStack: NavBackStack<HSScreen>) {
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
                    backStack = backStack,
                    viewModel = sendBitcoinViewModel,
                    amountInputModeViewModel = amountInputModeViewModel,
                    sendEntryPointDestId = sendEntryPointDestId,
                    amount = amount,
                    riskyAddress = riskyAddress
                )
            }

            BlockchainType.Zcash -> {
                val factory = SendZCashModule.Factory(wallet, address, hideAddress)
                val sendZCashViewModel = viewModel<SendZCashViewModel>(factory = factory)

                SendZCashScreen(
                    title = title,
                    backStack = backStack,
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
                    backStack = backStack,
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
                    backStack = backStack,
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
                    title = title,
                    backStack = backStack,
                    viewModel = sendTonViewModel,
                    amountInputModeViewModel = amountInputModeViewModel,
                    sendEntryPointDestId = sendEntryPointDestId,
                    amount = amount,
                    riskyAddress = riskyAddress
                )
            }

            BlockchainType.Tron -> {
                val factory = SendTronModule.Factory(wallet, address, hideAddress)
                val sendTronViewModel = viewModel<SendTronViewModel>(factory = factory)
                SendTronScreen(
                    title = title,
                    backStack = backStack,
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
                    title = title,
                    backStack = backStack,
                    viewModel = sendStellarViewModel,
                    amountInputModeViewModel = amountInputModeViewModel,
                    sendEntryPointDestId = sendEntryPointDestId,
                    amount = amount,
                    riskyAddress = riskyAddress
                )
            }

            BlockchainType.Monero -> {
                val factory = SendMoneroModule.Factory(wallet, address, hideAddress)
                val sendMoneroViewModel = viewModel<SendMoneroViewModel>(factory = factory)
                SendMoneroScreen(
                    title = title,
                    backStack = backStack,
                    viewModel = sendMoneroViewModel,
                    amountInputModeViewModel = amountInputModeViewModel,
                    sendEntryPointDestId = sendEntryPointDestId,
                    amount = amount,
                    memo = memo,
                    riskyAddress = riskyAddress
                )
            }


            else -> {}
        }
    }
}

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
