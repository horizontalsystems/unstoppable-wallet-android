package cash.p.terminal.modules.send

import android.os.Parcelable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.navigation.NavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import cash.p.terminal.MainGraphDirections
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.ISendBitcoinAdapter
import cash.p.terminal.core.ISendEthereumAdapter
import cash.p.terminal.core.ISendTonAdapter
import cash.p.terminal.core.authorizedAction
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.amount.AmountInputModeModule
import cash.p.terminal.modules.amount.AmountInputModeViewModel
import cash.p.terminal.modules.pin.ConfirmPinFragment
import cash.p.terminal.modules.pin.PinType
import cash.p.terminal.modules.send.SendConfirmationFragment.Type
import cash.p.terminal.modules.send.address.AddressCheckerControl
import cash.p.terminal.modules.send.address.isSmartContractCheckSupported
import cash.p.terminal.modules.send.bitcoin.SendBitcoinModule
import cash.p.terminal.modules.send.bitcoin.SendBitcoinNavHost
import cash.p.terminal.modules.send.bitcoin.SendBitcoinViewModel
import cash.p.terminal.modules.send.evm.SendEvmModule
import cash.p.terminal.modules.send.evm.SendEvmScreen
import cash.p.terminal.modules.send.evm.SendEvmViewModel
import cash.p.terminal.modules.send.monero.SendMoneroModule
import cash.p.terminal.modules.send.monero.SendMoneroScreen
import cash.p.terminal.modules.send.monero.SendMoneroViewModel
import cash.p.terminal.modules.send.securitycheck.SecurityCheckFragment
import cash.p.terminal.modules.send.solana.SendSolanaModule
import cash.p.terminal.modules.send.solana.SendSolanaScreen
import cash.p.terminal.modules.send.solana.SendSolanaViewModel
import cash.p.terminal.modules.send.stellar.SendStellarModule
import cash.p.terminal.modules.send.stellar.SendStellarScreen
import cash.p.terminal.modules.send.stellar.SendStellarViewModel
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
import cash.p.terminal.navigation.slideFromBottomForResult
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.components.HudHelper
import cash.p.terminal.ui_compose.findNavController
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.parcelize.Parcelize
import cash.p.terminal.core.getKoinInstance
import cash.p.terminal.core.managers.PoisonAddressManager
import org.koin.java.KoinJavaComponent.inject
import java.math.BigDecimal

class SendFragment : BaseComposeFragment() {

    private val addressCheckerControl: AddressCheckerControl by inject(AddressCheckerControl::class.java)
    private val poisonAddressManager: PoisonAddressManager by lazy { getKoinInstance() }
    private val args: SendFragmentArgs by navArgs()

    @Composable
    override fun GetContent(navController: NavController) {
        val navGraphOnBackStack = remember(navController.currentBackStackEntry) {
            try {
                navController.getBackStackEntry(R.id.sendXFragment)
                true
            } catch (_: IllegalArgumentException) {
                false
            }
        }
        if (!navGraphOnBackStack) {
            navController.navigateUp()
            return
        }

        val keyboardController = LocalSoftwareKeyboardController.current
        val wallet = args.input.wallet
        val title = args.input.title
        val address = args.input.address
        val prefilledData = PrefilledData(address?.hex, args.input.amount)
        val hideAddress = args.input.hideAddress
        val amount = args.input.amount

        val amountInputModeViewModel by navGraphViewModels<AmountInputModeViewModel>(R.id.sendXFragment) {
            AmountInputModeModule.Factory(wallet.coin.uid)
        }

        when (wallet.token.blockchainType) {
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.ECash,
            BlockchainType.Litecoin,
            BlockchainType.Dogecoin,
            BlockchainType.PirateCash,
            BlockchainType.Cosanta,
            BlockchainType.Dash -> {
                val adapter: ISendBitcoinAdapter? = App.adapterManager.getAdapterForWallet(wallet)
                if (adapter == null) {
                    HudHelper.showErrorMessage(
                        LocalView.current,
                        "No adapter for wallet $wallet"
                    )
                    navController.navigateUp()
                    return
                }
                val factory = SendBitcoinModule.Factory(wallet, address, hideAddress, adapter)
                val sendBitcoinViewModel by navGraphViewModels<SendBitcoinViewModel>(R.id.sendXFragment) {
                    factory
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    val navController = findNavController()
                    SendBitcoinNavHost(
                        title = title,
                        fragmentNavController = navController,
                        viewModel = sendBitcoinViewModel,
                        amountInputModeViewModel = amountInputModeViewModel,
                        prefilledData = prefilledData,
                        addressCheckerControl = addressCheckerControl,
                        onNextClick = {
                            navController.handleProceedAction(it, keyboardController)
                        }
                    )
                }
            }

            BlockchainType.Zcash -> {
                val factory = SendZCashModule.Factory(wallet, address, hideAddress)
                val sendZCashViewModel by navGraphViewModels<SendZCashViewModel>(R.id.sendXFragment) {
                    factory
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    SendZCashScreen(
                        title = title,
                        navController = findNavController(),
                        viewModel = sendZCashViewModel,
                        amountInputModeViewModel = amountInputModeViewModel,
                        prefilledData = prefilledData,
                        addressCheckerControl = addressCheckerControl,
                        onNextClick = {
                            navController.handleProceedAction(it, keyboardController)
                        }
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
                val adapter: ISendEthereumAdapter? = App.adapterManager.getAdapterForWallet(wallet)
                if (adapter == null) {
                    HudHelper.showErrorMessage(
                        LocalView.current,
                        "No adapter for wallet $wallet"
                    )
                    navController.navigateUp()
                    return
                }
                val factory = SendEvmModule.Factory(wallet, address, hideAddress, adapter)
                val viewModel by navGraphViewModels<SendEvmViewModel>(R.id.sendXFragment) {
                    factory
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    SendEvmScreen(
                        title = title,
                        navController = findNavController(),
                        viewModel = viewModel,
                        amountInputModeViewModel = amountInputModeViewModel,
                        wallet = wallet,
                        amount = amount,
                        addressCheckerControl = addressCheckerControl,
                        onNextClick = {
                            navController.handleProceedAction(it, keyboardController)
                        }
                    )
                }
            }

            BlockchainType.Solana -> {
                val factory = SendSolanaModule.Factory(wallet, address, hideAddress)
                val sendSolanaViewModel by navGraphViewModels<SendSolanaViewModel>(R.id.sendXFragment) { factory }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    SendSolanaScreen(
                        title = title,
                        navController = findNavController(),
                        viewModel = sendSolanaViewModel,
                        amountInputModeViewModel = amountInputModeViewModel,
                        prefilledData = prefilledData,
                        addressCheckerControl = addressCheckerControl,
                        onNextClick = {
                            navController.handleProceedAction(it, keyboardController)
                        }
                    )
                }
            }

            BlockchainType.Ton -> {
                val adapter: ISendTonAdapter? = App.adapterManager.getAdapterForWallet(wallet)
                if (adapter == null) {
                    HudHelper.showErrorMessage(
                        LocalView.current,
                        "No adapter for wallet $wallet"
                    )
                    navController.navigateUp()
                    return
                }
                val factory = SendTonModule.Factory(wallet, address, hideAddress, adapter)
                val sendTonViewModel by navGraphViewModels<SendTonViewModel>(R.id.sendXFragment) { factory }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    SendTonScreen(
                        title = title,
                        navController = findNavController(),
                        viewModel = sendTonViewModel,
                        amountInputModeViewModel = amountInputModeViewModel,
                        prefilledData = prefilledData,
                        addressCheckerControl = addressCheckerControl,
                        onNextClick = {
                            navController.handleProceedAction(it, keyboardController)
                        }
                    )
                }
            }

            BlockchainType.Tron -> {
                val factory = SendTronModule.Factory(wallet, address, hideAddress)
                val sendTronViewModel by navGraphViewModels<SendTronViewModel>(R.id.sendXFragment) { factory }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    SendTronScreen(
                        title = title,
                        navController = findNavController(),
                        viewModel = sendTronViewModel,
                        amountInputModeViewModel = amountInputModeViewModel,
                        prefilledData = prefilledData,
                        addressCheckerControl = addressCheckerControl,
                        onNextClick = {
                            navController.handleProceedAction(it, keyboardController)
                        }
                    )
                }
            }

            BlockchainType.Monero -> {
                val factory = SendMoneroModule.Factory(wallet, address, hideAddress)
                val sendMoneroViewModel by navGraphViewModels<SendMoneroViewModel>(R.id.sendXFragment) { factory }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    SendMoneroScreen(
                        title = title,
                        navController = findNavController(),
                        viewModel = sendMoneroViewModel,
                        amountInputModeViewModel = amountInputModeViewModel,
                        prefilledData = prefilledData,
                        addressCheckerControl = addressCheckerControl,
                        onNextClick = {
                            navController.handleProceedAction(it, keyboardController)
                        }
                    )
                }
            }

            BlockchainType.Stellar -> {
                val factory = SendStellarModule.Factory(wallet, address, hideAddress)
                val sendStellarViewModel by navGraphViewModels<SendStellarViewModel>(R.id.sendXFragment) { factory }
                SendStellarScreen(
                    title = title,
                    navController = findNavController(),
                    viewModel = sendStellarViewModel,
                    amountInputModeViewModel = amountInputModeViewModel,
                    amount = amount,
                    addressCheckerControl = addressCheckerControl,
                    onNextClick = {
                        navController.handleProceedAction(it, keyboardController)
                    }
                )
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    Text(
                        text = "Unsupported yet",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    private fun NavController.handleProceedAction(
        data: ProceedActionData,
        keyboardController: SoftwareKeyboardController?
    ) {
        val smartContractCheckEnabledForToken =
            addressCheckerControl.uiState.addressCheckSmartContractEnabled &&
                    isSmartContractCheckSupported(data.wallet.token)

        if (addressCheckerControl.uiState.addressCheckByBaseEnabled ||
            smartContractCheckEnabledForToken
        ) {
            data.address?.let {
                slideFromRight(
                    MainGraphDirections.actionGlobalToSecurityCheck(
                        SecurityCheckFragment.SecurityCheckInput(
                            address = it,
                            wallet = data.wallet,
                            type = data.type,
                            sendEntryPointDestId = args.input.sendEntryPointDestId
                        )
                    )
                )
            }
        } else {
            openConfirm(
                type = data.type,
                riskyAddress = args.input.riskyAddress,
                poisonAddress = isAddressSuspicious(data.address),
                keyboardController = keyboardController,
                sendEntryPointDestId = args.input.sendEntryPointDestId
            )
        }
    }

    private fun isAddressSuspicious(address: String?): Boolean {
        if (address == null) return false
        return poisonAddressManager.isAddressSuspicious(address, args.input.wallet.token.blockchainType, args.input.wallet.account.id)
    }

    @Parcelize
    data class Input(
        val wallet: Wallet,
        val title: String,
        val sendEntryPointDestId: Int = 0,
        val address: Address?,
        val riskyAddress: Boolean = false,
        val amount: BigDecimal? = null,
        val hideAddress: Boolean = false
    ) : Parcelable

    data class ProceedActionData(
        val address: String?,
        val wallet: Wallet,
        val type: Type
    )
}

internal fun NavController.openConfirm(
    type: Type,
    riskyAddress: Boolean,
    keyboardController: SoftwareKeyboardController?,
    sendEntryPointDestId: Int,
    poisonAddress: Boolean = false,
) {
    if (riskyAddress) {
        keyboardController?.hide()
        slideFromBottomForResult<AddressRiskyBottomSheetAlert.Result>(
            R.id.addressRiskyBottomSheetAlert,
            AddressRiskyBottomSheetAlert.Input(
                alertText = Translator.getString(R.string.Send_RiskyAddress_AlertText)
            )
        ) {
            openConfirm(type, sendEntryPointDestId)
        }
    } else if (poisonAddress) {
        keyboardController?.hide()
        slideFromBottomForResult<AddressRiskyBottomSheetAlert.Result>(
            R.id.addressRiskyBottomSheetAlert,
            AddressRiskyBottomSheetAlert.Input(
                alertText = Translator.getString(R.string.send_poison_address_alert)
            )
        ) {
            openConfirm(type, sendEntryPointDestId)
        }
    } else {
        openConfirm(type, sendEntryPointDestId)
    }
}

private fun NavController.openConfirm(
    type: Type,
    sendEntryPointDestId: Int
) {
    authorizedAction(
        ConfirmPinFragment.InputConfirm(
            descriptionResId = R.string.Unlock_EnterPasscode,
            pinType = PinType.TRANSFER
        )
    ) {
        navigate(
            MainGraphDirections.actionGlobalToSendConfirmationFragment(
                type,
                sendEntryPointDestId
            )
        )
    }
}
