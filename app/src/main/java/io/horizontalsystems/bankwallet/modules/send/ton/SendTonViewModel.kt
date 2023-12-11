package io.horizontalsystems.bankwallet.modules.send.ton

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.ISendTonAdapter
import io.horizontalsystems.bankwallet.core.LocalizedException
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationData
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException

class SendTonViewModel(
    val wallet: Wallet,
    val sendToken: Token,
    val feeToken: Token,
    val adapter: ISendTonAdapter,
    private val xRateService: XRateService,
    private val amountService: SendTonAmountService,
    private val addressService: SendTonAddressService,
    private val feeService: SendTonFeeService,
    val coinMaxAllowedDecimals: Int,
    private val contactsRepo: ContactsRepository,
    private val showAddressInput: Boolean,
): ViewModel() {
    val blockchainType = wallet.token.blockchainType
    val feeTokenMaxAllowedDecimals = feeToken.decimals
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var feeState = feeService.stateFlow.value

    var uiState by mutableStateOf(
        SendTonUiState(
            availableBalance = amountState.availableBalance,
            amountCaution = amountState.amountCaution,
            addressError = addressState.addressError,
            canBeSend = amountState.canBeSend && addressState.canBeSend,
            showAddressInput = showAddressInput,
            fee = feeState.fee
        )
    )
        private set

    var coinRate by mutableStateOf(xRateService.getRate(sendToken.coin.uid))
        private set
    var feeCoinRate by mutableStateOf(xRateService.getRate(feeToken.coin.uid))
        private set
    var sendResult by mutableStateOf<SendResult?>(null)
        private set

    private val logger: AppLogger = AppLogger("send-ton")

    init {
        amountService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAmountState(it)
        }
        addressService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAddressState(it)
        }
        feeService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedFeeState(it)
        }
        xRateService.getRateFlow(sendToken.coin.uid).collectWith(viewModelScope) {
            coinRate = it
        }
        xRateService.getRateFlow(feeToken.coin.uid).collectWith(viewModelScope) {
            feeCoinRate = it
        }

        viewModelScope.launch {
            feeService.start()
        }
    }

    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
        addressService.setAddress(address)
    }

    fun getConfirmationData(): SendConfirmationData {
        val address = addressState.address!!
        val contact = contactsRepo.getContactsFiltered(
            blockchainType,
            addressQuery = address.hex
        ).firstOrNull()
        return SendConfirmationData(
            amount = amountState.amount!!,
            fee = feeState.fee!!,
            address = address,
            contact = contact,
            coin = wallet.coin,
            feeCoin = feeToken.coin
        )
    }

    fun onClickSend() {
        logger.info("click send button")

        viewModelScope.launch {
            send()
        }
    }

    private suspend fun send() = withContext(Dispatchers.IO) {
        try {
            sendResult = SendResult.Sending
            logger.info("sending tx")

            adapter.send(amountState.amount!!, addressState.tonAddress!!)

            sendResult = SendResult.Sent
            logger.info("success")
        } catch (e: Throwable) {
            sendResult = SendResult.Failed(createCaution(e))
            logger.warning("failed", e)
        }
    }

    private fun createCaution(error: Throwable) = when (error) {
        is UnknownHostException -> HSCaution(TranslatableString.ResString(R.string.Hud_Text_NoInternet))
        is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
        else -> HSCaution(TranslatableString.PlainString(error.message ?: ""))
    }

    private fun handleUpdatedAmountState(amountState: SendTonAmountService.State) {
        this.amountState = amountState

        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendTonAddressService.State) {
        this.addressState = addressState

        emitState()
    }

    private fun handleUpdatedFeeState(feeState: SendTonFeeService.State) {
        this.feeState = feeState

        amountService.setFee(feeState.fee)

        emitState()
    }

    private fun emitState() {
        uiState = SendTonUiState(
            availableBalance = amountState.availableBalance,
            amountCaution = amountState.amountCaution,
            addressError = addressState.addressError,
            canBeSend = amountState.canBeSend && addressState.canBeSend,
            showAddressInput = showAddressInput,
            fee = feeState.fee,
        )
    }
}

data class SendTonUiState(
    val availableBalance: BigDecimal?,
    val amountCaution: HSCaution?,
    val addressError: Throwable?,
    val canBeSend: Boolean,
    val showAddressInput: Boolean,
    val fee: BigDecimal?,
)
