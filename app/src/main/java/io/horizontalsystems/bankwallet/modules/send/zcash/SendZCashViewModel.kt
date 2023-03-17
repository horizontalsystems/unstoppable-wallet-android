package io.horizontalsystems.bankwallet.modules.send.zcash

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationData
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException

class SendZCashViewModel(
    private val adapter: ISendZcashAdapter,
    val wallet: Wallet,
    private val xRateService: XRateService,
    private val amountService: SendAmountService,
    private val addressService: SendZCashAddressService,
    private val memoService: SendZCashMemoService,
    private val contactsRepo: ContactsRepository
) : ViewModel() {
    val blockchainType = wallet.token.blockchainType
    val coinMaxAllowedDecimals = wallet.token.decimals
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal
    val memoMaxLength by memoService::memoMaxLength

    private val fee = adapter.fee
    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var memoState = memoService.stateFlow.value

    var uiState by mutableStateOf(
        SendZCashUiState(
            fee = fee,
            availableBalance = amountState.availableBalance,
            addressError = addressState.addressError,
            amountCaution = amountState.amountCaution,
            memoIsAllowed = memoState.memoIsAllowed,
            canBeSend = amountState.canBeSend && addressState.canBeSend,
        )
    )
        private set

    var coinRate by mutableStateOf(xRateService.getRate(wallet.coin.uid))
        private set
    var sendResult by mutableStateOf<SendResult?>(null)
        private set

    private val logger = AppLogger("Send-${wallet.coin.code}")

    init {
        xRateService.getRateFlow(wallet.coin.uid).collectWith(viewModelScope) {
            coinRate = it
        }
        amountService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAmountState(it)
        }
        addressService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAddressState(it)
        }
        memoService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedMemoState(it)
        }
    }

    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
        viewModelScope.launch {
            addressService.setAddress(address)
        }
    }

    fun onEnterMemo(memo: String) {
        memoService.setMemo(memo)
    }

    private fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState

        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendZCashAddressService.State) {
        this.addressState = addressState

        memoService.setAddressType(addressState.addressType)

        emitState()
    }

    private fun handleUpdatedMemoState(memoState: SendZCashMemoService.State) {
        this.memoState = memoState

        emitState()
    }

    private fun emitState() {
        uiState = SendZCashUiState(
            availableBalance = amountState.availableBalance,
            fee = fee,
            addressError = addressState.addressError,
            amountCaution = amountState.amountCaution,
            memoIsAllowed = memoState.memoIsAllowed,
            canBeSend = amountState.canBeSend && addressState.canBeSend,
        )
    }

    fun getConfirmationData(): SendConfirmationData {
        val address = addressState.address!!
        val contact = contactsRepo.getContactsFiltered(
            blockchainType,
            addressQuery = address.hex
        ).firstOrNull()
        return SendConfirmationData(
            amount = amountState.amount!!,
            fee = fee,
            address = address,
            contact = contact,
            coin = wallet.coin,
            feeCoin = wallet.coin,
            memo = memoState.memo
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

            val send = adapter.send(
                amountState.amount!!,
                addressState.address!!.hex,
                memoState.memo,
                logger
            ).blockingGet()

            logger.info("success")
            sendResult = SendResult.Sent
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

data class SendZCashUiState(
    val fee: BigDecimal,
    val availableBalance: BigDecimal,
    val addressError: Throwable?,
    val amountCaution: HSCaution?,
    val memoIsAllowed: Boolean,
    val canBeSend: Boolean
)
