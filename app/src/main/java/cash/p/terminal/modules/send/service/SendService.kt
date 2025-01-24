package cash.p.terminal.modules.send.service

import cash.p.terminal.core.App
import cash.p.terminal.core.ISendBinanceAdapter
import cash.p.terminal.modules.amount.AmountValidator
import cash.p.terminal.modules.amount.SendAmountService
import cash.p.terminal.modules.send.binance.SendBinanceAddressService
import cash.p.terminal.modules.send.binance.SendBinanceFeeService
import cash.p.terminal.modules.send.bitcoin.SendBitcoinFeeRateService
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.useCases.WalletUseCase
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import java.math.BigDecimal

class SendService(private val token: Token, private val addressStr: String? = null) {

    private val _sendServiceFlow: MutableStateFlow<SendServiceState?> = MutableStateFlow(null)
    val sendServiceFlow = _sendServiceFlow.asStateFlow()

    private val walletUseCase: WalletUseCase by inject(WalletUseCase::class.java)
    private val wallet: Wallet? by lazy { walletUseCase.createWalletIfNotExists(token) }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var feeRateServiceBitcoin: SendBitcoinFeeRateService? = null

    private var feeServiceBinanceChain: SendBinanceFeeService? = null

    init {
        buildSendTokenServices()
        start()
    }

    private fun start() {
        coroutineScope.launch {
            feeRateServiceBitcoin?.start()
            feeServiceBinanceChain?.start()
        }
    }

    fun stop() {

    }

    fun setAmount(amount: BigDecimal) {
        val wallet = wallet ?: return
        when (wallet.token.blockchainType) {
            is BlockchainType.BinanceChain -> {
                val feeService = feeServiceBinanceChain ?: return
                _sendServiceFlow.value = SendServiceState(
                    feeService.feeToken,
                    feeService.stateFlow.value.fee
                )
            }

            else -> {}
        }
    }

    private fun buildSendTokenServices() {
        val wallet = wallet ?: return
        when (wallet.token.blockchainType) {
            /*BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.ECash,
            BlockchainType.Litecoin,
            BlockchainType.Dash -> {
                val adapter =
                    (App.adapterManager.getAdapterForWallet(wallet) as? ISendBitcoinAdapter)
                        ?: throw IllegalStateException("SendBitcoinAdapter is null")

                val provider = FeeRateProviderFactory.provider(wallet.token.blockchainType)!!
                val feeService = SendBitcoinFeeService(adapter)
                feeRateService = SendBitcoinFeeRateService(provider)
                val amountService =
                    SendBitcoinAmountService(adapter, wallet.coin.code, AmountValidator())
                val addressService = SendBitcoinAddressService(adapter, addressStr)
                val pluginService = SendBitcoinPluginService(wallet.token.blockchainType)
            }*/

            is BlockchainType.BinanceChain -> {
                val adapter =
                    (App.adapterManager.getAdapterForWallet(wallet) as? ISendBinanceAdapter)
                        ?: throw IllegalStateException("SendBinanceAdapter is null")
                val amountValidator = AmountValidator()
                val amountService =
                    SendAmountService(amountValidator, wallet.coin.code, adapter.availableBalance)
                val addressService = SendBinanceAddressService(adapter, addressStr)
                feeServiceBinanceChain =
                    SendBinanceFeeService(adapter, wallet.token, App.feeCoinProvider)
                val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)
            }

            /*BlockchainType.Zcash -> {
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
            BlockchainType.Base,
            BlockchainType.Gnosis,
            BlockchainType.Fantom,
            BlockchainType.ArbitrumOne -> {
                setContent {
                    SendEvmScreen(
                        title,
                        findNavController(),
                        amountInputModeViewModel,
                        prefilledData,
                        wallet,
                        predefinedAddress
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
            }*/

            else -> {}
        }

    }
}