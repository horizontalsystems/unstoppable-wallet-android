package cash.p.terminal.modules.send.bitcoin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.HSCaution
import cash.p.terminal.core.IMwebAddressValidator
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.ISendBitcoinAdapter
import cash.p.terminal.core.LocalizedException
import cash.p.terminal.core.adapters.BitcoinFeeInfo
import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.core.managers.PendingTransactionRegistrar
import cash.p.terminal.core.managers.RecentAddressManager
import cash.p.terminal.entities.PendingTransactionDraft
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.send.SendConfirmationData
import cash.p.terminal.modules.send.SendResult
import cash.p.terminal.modules.send.bitcoin.SendBitcoinModule.rbfSupported
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IBalanceAdapter
import cash.p.terminal.wallet.Wallet
import cash.z.ecc.android.sdk.ext.collectWith
import cash.p.terminal.trezor.domain.TrezorCancelledException
import com.tangem.common.core.TangemSdkError
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import cash.p.terminal.modules.send.BaseSendViewModel
import cash.p.terminal.wallet.isLitecoinMweb
import io.horizontalsystems.core.DispatcherProvider
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.logger.AppLogger
import io.horizontalsystems.hodler.LockTimeInterval
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import java.math.BigDecimal
import java.net.UnknownHostException
import kotlin.getValue

class SendBitcoinViewModel(
    val adapter: ISendBitcoinAdapter,
    wallet: Wallet,
    private val feeRateService: SendBitcoinFeeRateService,
    private val feeService: SendBitcoinFeeService,
    private val amountService: SendBitcoinAmountService,
    private val addressService: SendBitcoinAddressService,
    private val pluginService: SendBitcoinPluginService,
    xRateService: XRateService,
    private val btcBlockchainManager: BtcBlockchainManager,
    private val contactsRepo: ContactsRepository,
    private val showAddressInput: Boolean,
    private val localStorage: ILocalStorage,
    private val address: Address?,
    private val pendingRegistrar: PendingTransactionRegistrar,
    private val adapterManager: IAdapterManager,
    private val dispatcherProvider: DispatcherProvider
) : BaseSendViewModel<SendBitcoinUiState>(wallet, adapterManager) {
    private companion object {
        val BLOCKCHAINS_NOT_SUPPORTING_EXTRA_SETTINGS = listOf(
            BlockchainType.Dogecoin,
            BlockchainType.Cosanta,
            BlockchainType.PirateCash
        )
    }

    private data class SendRequest(
        val address: Address,
        val amount: BigDecimal,
        val feeRate: Int,
        val sdkBalance: BigDecimal
    )

    private val recentAddressManager: RecentAddressManager by inject(RecentAddressManager::class.java)

    val coinMaxAllowedDecimals = wallet.token.decimals
    val fiatMaxAllowedDecimals = AppConfigProvider.fiatDecimal

    val blockchainType by adapter::blockchainType
    val feeRateChangeable by feeRateService::feeRateChangeable
    val isLockTimeEnabled by pluginService::isLockTimeEnabled
    val lockTimeIntervals by pluginService::lockTimeIntervals

    private var utxoExpertModeEnabled by localStorage::utxoExpertModeEnabled
    private var feeRateState = feeRateService.stateFlow.value
    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var pluginState = pluginService.stateFlow.value
    private var fee: BigDecimal? = feeService.bitcoinFeeInfoFlow.value?.fee
    private var utxoData = SendBitcoinModule.UtxoData()
    private var memo: String? = null
    private var pendingTxId: String? = null
    private val isMweb = wallet.token.isLitecoinMweb
    private var isMemoAvailable: Boolean = false
    private var isAdvancedSettingsAvailable: Boolean = false

    private val logger = AppLogger("Send-${wallet.coin.code}")

    var customUnspentOutputs: List<UnspentOutputInfo>? = null
        private set

    var sendResult by mutableStateOf<SendResult?>(null)

    var coinRate by mutableStateOf(xRateService.getRate(wallet.coin.uid))
        private set

    init {
        feeRateService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedFeeRateState(it)
        }
        amountService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAmountState(it)
        }
        addressService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAddressState(it)
        }
        pluginService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedPluginState(it)
        }
        feeService.bitcoinFeeInfoFlow.collectWith(viewModelScope) {
            handleUpdatedFeeInfo(it)
        }
        xRateService.getRateFlow(wallet.coin.uid).collectWith(viewModelScope) {
            coinRate = it
        }
        localStorage.utxoExpertModeEnabledFlow.collectWith(viewModelScope) { enabled ->
            utxoExpertModeEnabled = enabled
            emitState()
        }
        (adapter as? IBalanceAdapter)?.balanceUpdatedFlow?.let { balanceUpdatedFlow ->
            viewModelScope.launch {
                balanceUpdatedFlow.collectLatest {
                    withContext(dispatcherProvider.default) {
                        refreshBalanceDependentServices()
                    }
                }
            }
        }

        refreshFeatureAvailability()
        viewModelScope.launch {
            feeRateService.start()
        }

        addressService.setAddress(address)
    }

    private fun refreshBalanceDependentServices() {
        amountService.refreshAvailableBalance()
        feeService.refresh()
    }

    override fun createState(): SendBitcoinUiState {
        val poison = isAddressSuspicious(addressState.validAddress?.hex)
        return SendBitcoinUiState(
            availableBalance = amountState.availableBalance,
            amount = amountState.amount,
            feeRate = feeRateState.feeRate,
            address = addressState.validAddress,
            memo = memo,
            isMemoAvailable = isMemoAvailable,
            fee = fee,
            lockTimeInterval = pluginState.lockTimeInterval,
            addressError = addressState.addressError,
            amountCaution = amountState.amountCaution,
            feeRateCaution = feeRateState.feeRateCaution,
            canBeSend = amountState.canBeSend && addressState.canBeSend && feeRateState.canBeSend && (!poison || riskAccepted),
            showAddressInput = showAddressInput,
            utxoData = if (utxoExpertModeEnabled && isAdvancedSettingsAvailable) utxoData else null,
            isAdvancedSettingsAvailable = isAdvancedSettingsAvailable,
            isPoisonAddress = poison,
            riskAccepted = riskAccepted,
        )
    }

    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
        resetRiskAccepted()
        addressService.setAddress(address)
    }

    fun onEnterMemo(memoValue: String) {
        if (!isMemoAvailable) return

        val memo = memoValue.ifBlank { null }

        this.memo = memo

        amountService.setMemo(memo)
        feeService.setMemo(memo)

        emitState()
    }

    fun reset() {
        feeRateService.reset()
        onEnterLockTimeInterval(null)
    }

    fun updateFeeRate(value: Int) {
        feeRateService.setFeeRate(value)
    }

    fun incrementFeeRate() {
        val incremented = (feeRateState.feeRate ?: 0) + 1
        updateFeeRate(incremented)
    }

    fun decrementFeeRate() {
        var incremented = (feeRateState.feeRate ?: 0) - 1
        if (incremented < 0) {
            incremented = 0
        }
        updateFeeRate(incremented)
    }

    fun onEnterLockTimeInterval(lockTimeInterval: LockTimeInterval?) {
        pluginService.setLockTimeInterval(lockTimeInterval.takeIf { isAdvancedSettingsAvailable })
    }

    fun updateCustomUnspentOutputs(customUnspentOutputs: List<UnspentOutputInfo>) {
        val unspentOutputs = customUnspentOutputs.ifEmpty { null }
        this.customUnspentOutputs = unspentOutputs
        fee = null
        updateUtxoData(customUnspentOutputs.size)
        feeService.setCustomUnspentOutputs(unspentOutputs)
        amountService.setCustomUnspentOutputs(unspentOutputs)
        emitState()
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

        refreshFeatureAvailability()
        amountService.setValidAddress(addressState.validAddress)
        feeService.setValidAddress(addressState.validAddress)

        emitState()
    }

    private fun refreshFeatureAvailability() {
        val mwebTransaction = isMwebTransaction(addressState.validAddress)
        isMemoAvailable = !mwebTransaction && blockchainType !in BLOCKCHAINS_NOT_SUPPORTING_EXTRA_SETTINGS
        isAdvancedSettingsAvailable = !mwebTransaction && blockchainType !in BLOCKCHAINS_NOT_SUPPORTING_EXTRA_SETTINGS

        if (mwebTransaction) {
            if (memo != null) {
                memo = null
                amountService.setMemo(null, forceEmit = false)
                feeService.setMemo(null, forceEmit = false)
            }
            if (pluginState.lockTimeInterval != null) {
                pluginService.setLockTimeInterval(null)
            }
        }
    }

    private fun isMwebTransaction(address: Address?): Boolean {
        val destination = address?.hex ?: return isMweb
        val mwebAddressValidator = adapter as? IMwebAddressValidator
        return isMweb || mwebAddressValidator?.isMwebAddress(destination) == true
    }

    private fun handleUpdatedFeeRateState(feeRateState: SendBitcoinFeeRateService.State) {
        this.feeRateState = feeRateState

        feeService.setFeeRate(feeRateState.feeRate)
        amountService.setFeeRate(feeRateState.feeRate)

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
        fee = info?.fee
        if (info == null && customUnspentOutputs == null) {
            utxoData = SendBitcoinModule.UtxoData()
        } else if (customUnspentOutputs == null) {
            //set unspent outputs as auto
            updateUtxoData(info?.selectedUtxoCount ?: 0)
        }
        emitState()
    }

    fun getConfirmationData(): SendConfirmationData {
        val address = addressState.validAddress
            ?: throw LocalizedException(R.string.send_error_address_unavailable)
        val amount = amountState.amount
            ?: throw LocalizedException(R.string.send_error_amount_unavailable)
        val contact = contactsRepo.getContactsFiltered(
            blockchainType,
            addressQuery = address.hex
        ).firstOrNull()
        return SendConfirmationData(
            amount = amount,
            fee = fee,
            address = address,
            contact = contact,
            coin = wallet.token.coin,
            feeCoin = wallet.token.coin,
            lockTimeInterval = pluginState.lockTimeInterval,
            memo = memo,
            rbfEnabled = localStorage.rbfEnabled
        )
    }

    fun onClickSend() {
        viewModelScope.launch {
            send()
        }
    }

    private suspend fun send() = withContext(dispatcherProvider.io) {
        val logger = logger.getScopedUnique()
        logger.info("click")
        try {
            sendResult = SendResult.Sending
            logger.info("sending tx")
            val request = sendRequest()
            if (isMwebTransaction(request.address)) {
                logger.info("mweb send")
            }

            pendingTxId = pendingRegistrar.register(pendingTransactionDraft(request))
            val transactionHash = broadcastTransaction(request)
            pendingTxId?.let { pendingRegistrar.updateTxId(it, transactionHash) }
            completeSuccessfulSend(request.address, transactionHash, logger)
        } catch (e: TangemSdkError.UserCancelled) {
            pendingTxId?.let { pendingRegistrar.deleteFailed(it) }
            sendResult = null
            logger.info("user cancelled")
        } catch (e: TrezorCancelledException) {
            pendingTxId?.let { pendingRegistrar.deleteFailed(it) }
            sendResult = null
            logger.info("trezor user cancelled")
        } catch (e: Throwable) {
            pendingTxId?.let { pendingRegistrar.deleteFailed(it) }
            if (e is TangemSdkError) {
                sendResult = null
                return@withContext
            }
            logger.warning("failed", e)
            sendResult = SendResult.Failed(createCaution(e))
        }
    }

    private fun sendRequest(): SendRequest {
        val validAddress = addressState.validAddress
            ?: throw LocalizedException(R.string.send_error_address_unavailable)
        val amount = amountState.amount
            ?: throw LocalizedException(R.string.send_error_amount_unavailable)
        val feeRate = feeRateState.feeRate
            ?: throw LocalizedException(R.string.send_error_fee_rate_unavailable)
        val sdkBalance = adapterManager.getBalanceAdapterForWallet(wallet)
            ?.balanceData?.available ?: amountState.availableBalance
            ?: throw LocalizedException(R.string.send_error_balance_unavailable)

        return SendRequest(validAddress, amount, feeRate, sdkBalance)
    }

    private fun pendingTransactionDraft(request: SendRequest): PendingTransactionDraft {
        return PendingTransactionDraft(
            wallet = wallet,
            token = wallet.token,
            amount = request.amount,
            fee = fee,
            sdkBalanceAtCreation = request.sdkBalance,
            fromAddress = "",
            toAddress = request.address.hex,
            memo = memo
        )
    }

    private suspend fun broadcastTransaction(request: SendRequest): String {
        return adapter.send(
            amount = request.amount,
            address = request.address.hex,
            memo = memo,
            feeRate = request.feeRate,
            unspentOutputs = customUnspentOutputs,
            pluginData = pluginState.pluginData,
            transactionSorting = btcBlockchainManager.transactionSortMode(adapter.blockchainType),
            rbfEnabled = !isMweb && blockchainType.rbfSupported && localStorage.rbfEnabled,
            changeToFirstInput = false,
            utxoFilters = UtxoFilters()
        )
    }

    private fun completeSuccessfulSend(address: Address, transactionHash: String, logger: AppLogger) {
        val isQueued = adapter.isTransactionInSendQueue(transactionHash)

        logger.info("success, queued=$isQueued")
        onSendSuccess(address.hex)
        sendResult = if (isQueued) {
            SendResult.SentButQueued(transactionHash)
        } else {
            SendResult.Sent(transactionHash)
        }

        this.address?.let {
            recentAddressManager.setRecentAddress(it, blockchainType)
        }
    }

    private fun createCaution(error: Throwable) = when (error) {
        is UnknownHostException -> HSCaution(TranslatableString.ResString(R.string.Hud_Text_NoInternet))
        is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
        else -> HSCaution(TranslatableString.PlainString(error.message ?: ""))
    }

}

data class SendBitcoinUiState(
    val availableBalance: BigDecimal?,
    val amount: BigDecimal?,
    val fee: BigDecimal?,
    val feeRate: Int?,
    val address: Address?,
    val memo: String?,
    val isMemoAvailable: Boolean,
    val lockTimeInterval: LockTimeInterval?,
    val addressError: Throwable?,
    val amountCaution: HSCaution?,
    val feeRateCaution: HSCaution?,
    val canBeSend: Boolean,
    val showAddressInput: Boolean,
    val utxoData: SendBitcoinModule.UtxoData?,
    val isAdvancedSettingsAvailable: Boolean,
    val isPoisonAddress: Boolean = false,
    val riskAccepted: Boolean = false,
)
