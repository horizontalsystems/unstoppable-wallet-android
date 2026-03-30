package cash.p.terminal.modules.send.monero

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.EvmError
import cash.p.terminal.core.HSCaution
import cash.p.terminal.core.ISendMoneroAdapter
import cash.p.terminal.core.LocalizedException
import cash.p.terminal.core.ethereum.CautionViewItem
import cash.p.terminal.core.ethereum.toCautionViewItem
import cash.p.terminal.core.managers.ConnectivityManager
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.amount.SendAmountService
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.send.SendConfirmationData
import cash.p.terminal.modules.send.SendErrorInsufficientBalance
import cash.p.terminal.modules.send.SendResult
import cash.p.terminal.modules.send.SendUiState
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.TokenType
import cash.z.ecc.android.sdk.ext.collectWith
import cash.p.terminal.modules.send.BaseSendViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.net.UnknownHostException

class SendMoneroViewModel(
    wallet: Wallet,
    val sendToken: Token,
    val adapter: ISendMoneroAdapter,
    xRateService: XRateService,
    private val amountService: SendAmountService,
    private val addressService: SendMoneroAddressService,
    val coinMaxAllowedDecimals: Int,
    private val showAddressInput: Boolean,
    private val contactsRepo: ContactsRepository,
    private val connectivityManager: ConnectivityManager,
    private val address: Address?,
    private val adapterManager: IAdapterManager,
) : BaseSendViewModel<SendUiState>(wallet, adapterManager) {
    val blockchainType = wallet.token.blockchainType
    val feeTokenMaxAllowedDecimals = sendToken.decimals
    val fiatMaxAllowedDecimals = AppConfigProvider.fiatDecimal

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value

    var coinRate by mutableStateOf(xRateService.getRate(sendToken.coin.uid))
        private set
    var feeCoinRate by mutableStateOf(xRateService.getRate(sendToken.coin.uid))
        private set
    var sendResult by mutableStateOf<SendResult?>(null)
        private set

    var memo by mutableStateOf<String?>(null)
        private set
    var feeInProgress by mutableStateOf<Boolean>(false)
        private set
    var fee by mutableStateOf<BigDecimal?>(null)
        private set

    var cautions by mutableStateOf<List<CautionViewItem>>(emptyList())
        private set

    private val decimalAmount: BigDecimal
        get() = amountState.amount ?: BigDecimal.ZERO

    init {
        amountService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAmountState(it)
            recalculateFee()
        }
        addressService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAddressState(it)
            recalculateFee()
        }
        xRateService.getRateFlow(sendToken.coin.uid).collectWith(viewModelScope) {
            coinRate = it
        }
        xRateService.getRateFlow(sendToken.coin.uid).collectWith(viewModelScope) {
            feeCoinRate = it
        }
        viewModelScope.launch {
            addressService.setAddress(address)
        }
    }

    override fun createState(): SendUiState {
        val poison = isAddressSuspicious(addressState.address?.hex)
        return SendUiState(
            availableBalance = amountState.availableBalance,
            addressError = addressState.addressError,
            amountCaution = amountState.amountCaution,
            canBeSend = amountState.canBeSend && addressState.canBeSend && (!poison || riskAccepted),
            showAddressInput = showAddressInput,
            address = addressState.address,
            cautions = cautions,
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

    fun getConfirmationData(): SendConfirmationData {
        val address = addressState.address!!
        val contact = contactsRepo.getContactsFiltered(
            blockchainType,
            addressQuery = address.hex
        ).firstOrNull()
        return SendConfirmationData(
            amount = decimalAmount,
            fee = fee,
            address = address,
            contact = contact,
            coin = wallet.coin,
            feeCoin = sendToken.coin,
            memo = null
        )
    }

    fun onEnterMemo(memoNew: String) {
        memo = memoNew.ifBlank { null }
        recalculateFee()
    }

    fun onClickSend() = viewModelScope.launch(Dispatchers.IO) {
        if (!hasConnection()) {
            sendResult = SendResult.Failed(createCaution(UnknownHostException()))
            return@launch
        }

        try {
            sendResult = SendResult.Sending
            val fee = adapter.estimateFee(decimalAmount, addressState.address!!.hex, null)
            val totalSolAmount =
                (if (sendToken.type == TokenType.Native) decimalAmount else BigDecimal.ZERO) + fee

            val availableBalance = adapterManager.getAdjustedBalanceData(wallet)?.available
                ?: adapter.balanceData.available
            if (totalSolAmount > availableBalance)
                throw EvmError.InsufficientBalanceWithFee

            adapter.send(decimalAmount, addressState.address!!.hex, null)

            onSendSuccess(addressState.address?.hex)
            sendResult = SendResult.Sent()
        } catch (e: Throwable) {
            sendResult = SendResult.Failed(createCaution(e))
        }
    }

    fun hasConnection(): Boolean {
        return connectivityManager.isConnected.value
    }

    private fun recalculateFee() {
        if (addressState.address?.hex == null || decimalAmount == BigDecimal.ZERO) {
            cautions = emptyList()
            return
        }

        feeInProgress = true
        viewModelScope.launch(Dispatchers.Default + CoroutineExceptionHandler { _, error ->
            fee = null
            cautions = listOf(createCaution(error).toCautionViewItem())
            feeInProgress = false
        }) {
            fee = adapter.estimateFee(decimalAmount, addressState.address!!.hex, memo)
            cautions = emptyList()
            feeInProgress = false
        }
    }

    private fun createCaution(error: Throwable) = when (error) {
        is UnknownHostException -> HSCaution(TranslatableString.ResString(R.string.Hud_Text_NoInternet))
        is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
        is EvmError.InsufficientBalanceWithFee -> SendErrorInsufficientBalance(sendToken.coin.code, amountState.availableBalance.toPlainString())
        else -> HSCaution(
            TranslatableString.PlainString(
                error.cause?.message ?: error.message ?: ""
            )
        )
    }

    private fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState

        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendMoneroAddressService.State) {
        this.addressState = addressState

        emitState()
    }

}
