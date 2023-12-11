package io.horizontalsystems.bankwallet.modules.send.solana

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.ISendSolanaAdapter
import io.horizontalsystems.bankwallet.core.LocalizedException
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationData
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.modules.send.SendUiState
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.solanakit.SolanaKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException

class SendSolanaViewModel(
    val wallet: Wallet,
    val sendToken: Token,
    val feeToken: Token,
    val adapter: ISendSolanaAdapter,
    private val xRateService: XRateService,
    private val amountService: SendAmountService,
    private val addressService: SendSolanaAddressService,
    val coinMaxAllowedDecimals: Int,
    private val contactsRepo: ContactsRepository,
    private val showAddressInput: Boolean,
    private val connectivityManager: ConnectivityManager,
) : ViewModel() {
    val blockchainType = wallet.token.blockchainType
    val feeTokenMaxAllowedDecimals = feeToken.decimals
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value

    var uiState by mutableStateOf(
        SendUiState(
            availableBalance = amountState.availableBalance,
            amountCaution = amountState.amountCaution,
            addressError = addressState.addressError,
            canBeSend = amountState.canBeSend && addressState.canBeSend,
            showAddressInput = showAddressInput,
        )
    )
        private set

    var coinRate by mutableStateOf(xRateService.getRate(sendToken.coin.uid))
        private set
    var feeCoinRate by mutableStateOf(xRateService.getRate(feeToken.coin.uid))
        private set
    var sendResult by mutableStateOf<SendResult?>(null)
        private set
    private val decimalAmount: BigDecimal
        get() = amountState.amount!!

    init {
        amountService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAmountState(it)
        }
        addressService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAddressState(it)
        }
        xRateService.getRateFlow(sendToken.coin.uid).collectWith(viewModelScope) {
            coinRate = it
        }
        xRateService.getRateFlow(feeToken.coin.uid).collectWith(viewModelScope) {
            feeCoinRate = it
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
            amount = decimalAmount,
            fee = SolanaKit.fee,
            address = address,
            contact = contact,
            coin = wallet.coin,
            feeCoin = feeToken.coin
        )
    }

    fun onClickSend() {
        viewModelScope.launch {
            send()
        }
    }

    fun hasConnection(): Boolean {
        return connectivityManager.isConnected
    }

    private suspend fun send() = withContext(Dispatchers.IO) {
        if (!hasConnection()){
            sendResult = SendResult.Failed(createCaution(UnknownHostException()))
            return@withContext
        }

        try {
            sendResult = SendResult.Sending

            adapter.send(decimalAmount, addressState.evmAddress!!)

            sendResult = SendResult.Sent
        } catch (e: Throwable) {
            sendResult = SendResult.Failed(createCaution(e))
        }
    }

    private fun createCaution(error: Throwable) = when (error) {
        is UnknownHostException -> HSCaution(TranslatableString.ResString(R.string.Hud_Text_NoInternet))
        is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
        else -> HSCaution(TranslatableString.PlainString(error.message ?: ""))
    }

    private fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState

        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendSolanaAddressService.State) {
        this.addressState = addressState

        emitState()
    }

    private fun emitState() {
        uiState = SendUiState(
            availableBalance = amountState.availableBalance,
            amountCaution = amountState.amountCaution,
            addressError = addressState.addressError,
            canBeSend = amountState.canBeSend && addressState.canBeSend,
            showAddressInput = showAddressInput,
        )
    }

}
