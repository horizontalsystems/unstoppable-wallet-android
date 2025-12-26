package io.horizontalsystems.bankwallet.modules.send.bitcoin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.LocalizedException
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.adapters.BitcoinFeeInfo
import io.horizontalsystems.bankwallet.core.managers.BtcBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.RecentAddressManager
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationData
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinModule.rbfSupported
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.hodler.LockTimeInterval
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException

class SendBitcoinViewModel(
    val adapter: ISendBitcoinAdapter,
    val wallet: Wallet,
    private val feeRateService: SendBitcoinFeeRateService,
    private val feeService: SendBitcoinFeeService,
    private val amountService: SendBitcoinAmountService,
    private val addressService: SendBitcoinAddressService,
    private val pluginService: SendBitcoinPluginService,
    private val xRateService: XRateService,
    private val btcBlockchainManager: BtcBlockchainManager,
    private val contactsRepo: ContactsRepository,
    private val showAddressInput: Boolean,
    private val localStorage: ILocalStorage,
    private val address: Address,
    private val recentAddressManager: RecentAddressManager
) : ViewModelUiState<SendBitcoinUiState>() {
    val coinMaxAllowedDecimals = wallet.token.decimals
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

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
    private val rbfEnabled = blockchainType.rbfSupported && localStorage.rbfEnabled

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

        viewModelScope.launch {
            feeRateService.start()
        }

        addressService.setAddress(address)
    }

    override fun createState() = SendBitcoinUiState(
        availableBalance = amountState.availableBalance,
        amount = amountState.amount,
        feeRate = feeRateState.feeRate,
        address = address,
        memo = memo,
        fee = fee,
        lockTimeInterval = pluginState.lockTimeInterval,
        addressError = addressState.addressError,
        amountCaution = amountState.amountCaution,
        feeRateCaution = feeRateState.feeRateCaution,
        canBeSend = amountState.canBeSend && addressState.canBeSend && feeRateState.canBeSend,
        showAddressInput = showAddressInput,
        utxoData = if (utxoExpertModeEnabled) utxoData else null,
    )

    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
        addressService.setAddress(address)
    }

    fun onEnterMemo(memoValue: String) {
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
        pluginService.setLockTimeInterval(lockTimeInterval)
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

        amountService.setValidAddress(addressState.validAddress)
        feeService.setValidAddress(addressState.validAddress)

        emitState()
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
            updateUtxoData(info?.unspentOutputs?.size ?: 0)
        }
        emitState()
    }

    fun getConfirmationData(): SendConfirmationData {
        val address = addressState.validAddress!!
        val contact = contactsRepo.getContactsFiltered(
            blockchainType,
            addressQuery = address.hex
        ).firstOrNull()
        return SendConfirmationData(
            amount = amountState.amount!!,
            fee = fee!!,
            address = address,
            contact = contact,
            coin = wallet.token.coin,
            feeCoin = wallet.token.coin,
            lockTimeInterval = pluginState.lockTimeInterval,
            memo = memo,
            rbfEnabled = rbfEnabled
        )
    }

    fun onClickSend() {
        viewModelScope.launch {
            send()
        }
    }

    private suspend fun send() = withContext(Dispatchers.IO) {
        val logger = logger.getScopedUnique()
        logger.info("click")

        try {
            sendResult = SendResult.Sending
            logger.info("sending tx")
            val transactionRecord = adapter.send(
                amountState.amount!!,
                addressState.validAddress!!.hex,
                memo,
                feeRateState.feeRate!!,
                customUnspentOutputs,
                pluginState.pluginData,
                btcBlockchainManager.transactionSortMode(adapter.blockchainType),
                rbfEnabled,
                false,
                UtxoFilters()
            )

            logger.info("success")
            sendResult = SendResult.Sent(transactionRecord)

            recentAddressManager.setRecentAddress(address, blockchainType)
        } catch (e: Throwable) {
            logger.warning("failed", e)
            sendResult = SendResult.Failed(createCaution(e))
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
    val address: Address,
    val memo: String?,
    val lockTimeInterval: LockTimeInterval?,
    val addressError: Throwable?,
    val amountCaution: HSCaution?,
    val feeRateCaution: HSCaution?,
    val canBeSend: Boolean,
    val showAddressInput: Boolean,
    val utxoData: SendBitcoinModule.UtxoData?
)
