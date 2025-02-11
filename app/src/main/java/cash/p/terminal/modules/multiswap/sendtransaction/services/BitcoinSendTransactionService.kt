package cash.p.terminal.modules.multiswap.sendtransaction.services

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import cash.p.terminal.core.App
import cash.p.terminal.core.AppLogger
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.ISendBitcoinAdapter
import cash.p.terminal.core.adapters.BitcoinFeeInfo
import cash.p.terminal.core.ethereum.CautionViewItem
import cash.p.terminal.core.factories.FeeRateProviderFactory
import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.entities.Address
import cash.p.terminal.entities.CoinValue
import cash.p.terminal.modules.amount.AmountInputModeViewModel
import cash.p.terminal.modules.amount.AmountValidator
import cash.p.terminal.modules.multiswap.sendtransaction.ISendTransactionService
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionData
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionResult
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionServiceState
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionSettings
import cash.p.terminal.modules.multiswap.ui.DataField
import cash.p.terminal.modules.send.SendModule
import cash.p.terminal.modules.send.SendResult
import cash.p.terminal.modules.send.bitcoin.SendBitcoinAddressService
import cash.p.terminal.modules.send.bitcoin.SendBitcoinAmountService
import cash.p.terminal.modules.send.bitcoin.SendBitcoinFeeRateService
import cash.p.terminal.modules.send.bitcoin.SendBitcoinFeeService
import cash.p.terminal.modules.send.bitcoin.SendBitcoinModule
import cash.p.terminal.modules.send.bitcoin.SendBitcoinPluginService
import cash.p.terminal.modules.send.bitcoin.SendBitcoinViewModel
import cash.p.terminal.modules.send.bitcoin.advanced.SendBtcAdvancedSettingsScreen
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Token
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.core.entities.CurrencyValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

class BitcoinSendTransactionService(
    token: Token
) : ISendTransactionService<ISendBitcoinAdapter>(token) {

    private val provider = FeeRateProviderFactory.provider(token.blockchainType)!!
    private val feeRateService = SendBitcoinFeeRateService(provider)
    private val amountService =
        SendBitcoinAmountService(adapter, wallet.coin.code, AmountValidator())
    private val addressService = SendBitcoinAddressService(adapter, null)
    private val feeService = SendBitcoinFeeService(adapter)
    private val localStorage: ILocalStorage by inject(ILocalStorage::class.java)
    private val pluginService = SendBitcoinPluginService(wallet.token.blockchainType)
    private val marketKit: MarketKitWrapper by inject(MarketKitWrapper::class.java)
    private val xRateService = XRateService(marketKit, App.currencyManager.baseCurrency)

    private val _sendTransactionSettingsFlow = MutableStateFlow(
        SendTransactionSettings.Common
    )
    override val sendTransactionSettingsFlow: StateFlow<SendTransactionSettings> =
        _sendTransactionSettingsFlow.asStateFlow()

    private var feeAmountData: SendModule.AmountData? = null
    private var cautions: List<CautionViewItem> = listOf()
    private var sendable = false
    private var loading = true
    private var fields = listOf<DataField>()

    override fun createState() = SendTransactionServiceState(
        networkFee = feeAmountData,
        cautions = cautions,
        sendable = sendable,
        loading = loading,
        fields = fields
    )

    val coinMaxAllowedDecimals = wallet.token.decimals
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    val blockchainType by adapter::blockchainType

    private var utxoExpertModeEnabled by localStorage::utxoExpertModeEnabled
    private var feeRateState = feeRateService.stateFlow.value
    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var pluginState = pluginService.stateFlow.value
    private val btcBlockchainManager: BtcBlockchainManager by inject(BtcBlockchainManager::class.java)
    private var utxoData = SendBitcoinModule.UtxoData()

    private var customUnspentOutputs: List<UnspentOutputInfo>? = null

    private var coinRate by mutableStateOf(xRateService.getRate(wallet.coin.uid))
    private val logger = AppLogger("ISendTransactionService")

    override fun start(coroutineScope: CoroutineScope) {
        feeRateService.stateFlow.collectWith(coroutineScope) {
            handleUpdatedFeeRateState(it)
        }
        amountService.stateFlow.collectWith(coroutineScope) {
            handleUpdatedAmountState(it)
        }
        addressService.stateFlow.collectWith(coroutineScope) {
            handleUpdatedAddressState(it)
        }
        pluginService.stateFlow.collectWith(coroutineScope) {
            handleUpdatedPluginState(it)
        }
        feeService.bitcoinFeeInfoFlow.collectWith(coroutineScope) {
            handleUpdatedFeeInfo(it)
        }
        xRateService.getRateFlow(wallet.coin.uid).collectWith(coroutineScope) {
            coinRate = it
        }
        localStorage.utxoExpertModeEnabledFlow.collectWith(coroutineScope) { enabled ->
            utxoExpertModeEnabled = enabled
            emitState()
        }

        coroutineScope.launch {
            feeRateService.start()
        }
    }

    private fun updateUtxoData(usedUtxosSize: Int) {
        utxoData = SendBitcoinModule.UtxoData(
            type = if (customUnspentOutputs == null) SendBitcoinModule.UtxoType.Auto else SendBitcoinModule.UtxoType.Manual,
            value = "$usedUtxosSize / ${adapter.unspentOutputs.size}"
        )
    }

    private fun handleUpdatedAmountState(amountState: SendBitcoinAmountService.State) {
        this.amountState = amountState

        feeService.setAmount(amountState.amount)

        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendBitcoinAddressService.State) {
        this.addressState = addressState

        amountService.setValidAddress(addressState.validAddress)
        feeService.setValidAddress(addressState.validAddress)

        emitState()
    }

    private fun handleUpdatedFeeRateState(feeRateState: SendBitcoinFeeRateService.State) {
        this.feeRateState = feeRateState
        sendable = feeRateState.canBeSend

        feeService.setFeeRate(feeRateState.feeRate)
        amountService.setFeeRate(feeRateState.feeRate)
        loading = false

        emitState()
    }

    private fun handleUpdatedPluginState(pluginState: SendBitcoinPluginService.State) {
        this.pluginState = pluginState

        feeService.setPluginData(pluginState.pluginData)
        amountService.setPluginData(pluginState.pluginData)
        addressService.setPluginData(pluginState.pluginData)

        emitState()
    }

    private fun handleUpdatedFeeInfo(info: BitcoinFeeInfo?) {
        info?.fee?.let { fee ->
            val coinValue = CoinValue(token, fee)
            val primaryAmountInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)
            val secondaryAmountInfo = rate?.let {
                SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(it.currency, it.value * fee))
            }
            feeAmountData = SendModule.AmountData(primaryAmountInfo, secondaryAmountInfo)
        }

        if (info == null && customUnspentOutputs == null) {
            utxoData = SendBitcoinModule.UtxoData()
        } else if (customUnspentOutputs == null) {
            //set unspent outputs as auto
            updateUtxoData(info?.unspentOutputs?.size ?: 0)
        }
        emitState()
    }

    override fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Common)
        addressService.setAddress(
            Address(
                hex = data.address,
                blockchainType = adapter.blockchainType
            )
        )
        amountService.setAmount(data.amount)
    }

    @Composable
    override fun GetSettingsContent(navController: NavController) {
        val navHostController = navController as? NavHostController ?: return
        val sendBitcoinViewModel = SendBitcoinViewModel(
            adapter = adapter,
            wallet = wallet,
            feeRateService = feeRateService,
            feeService = feeService,
            amountService = amountService,
            addressService = addressService,
            pluginService = pluginService,
            xRateService = xRateService,
            btcBlockchainManager = btcBlockchainManager,
            contactsRepo = App.contactsRepository,
            showAddressInput = true,
            localStorage = localStorage
        )
        val amountInputModeViewModel = AmountInputModeViewModel(
            localStorage = localStorage,
            xRateService = xRateService,
            coinUid = wallet.coin.uid
        )
        SendBtcAdvancedSettingsScreen(
            fragmentNavController = navController,
            navController = navHostController,
            sendBitcoinViewModel = sendBitcoinViewModel,
            amountInputType = amountInputModeViewModel.inputType,
        )
    }

    @SuppressLint("CheckResult")
    override suspend fun sendTransaction(): SendTransactionResult = withContext(Dispatchers.IO) {
        try {
            val recordUid = adapter.send(
                amount = amountState.amount!!,
                address = addressState.validAddress!!.hex,
                memo = null,
                feeRate = feeRateState.feeRate!!,
                unspentOutputs = customUnspentOutputs,
                pluginData = pluginState.pluginData,
                transactionSorting = btcBlockchainManager.transactionSortMode(adapter.blockchainType),
                rbfEnabled = localStorage.rbfEnabled,
                logger = logger
            ).blockingGet()
            SendTransactionResult.Common(SendResult.Sent(recordUid))
        } catch (e: Throwable) {
            cautions = listOf(createCaution(e))
            emitState()
            throw e
        }
    }
}