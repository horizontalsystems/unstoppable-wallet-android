package com.quantum.wallet.bankwallet.modules.send.stellar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.AppLogger
import com.quantum.wallet.bankwallet.core.HSCaution
import com.quantum.wallet.bankwallet.core.ISendStellarAdapter
import com.quantum.wallet.bankwallet.core.LocalizedException
import com.quantum.wallet.bankwallet.core.ViewModelUiState
import com.quantum.wallet.bankwallet.core.managers.RecentAddressManager
import com.quantum.wallet.bankwallet.entities.Address
import com.quantum.wallet.bankwallet.entities.Wallet
import com.quantum.wallet.bankwallet.modules.amount.SendAmountService
import com.quantum.wallet.bankwallet.modules.contacts.ContactsRepository
import com.quantum.wallet.bankwallet.modules.send.SendConfirmationData
import com.quantum.wallet.bankwallet.modules.send.SendResult
import com.quantum.wallet.bankwallet.modules.xrate.XRateService
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException

class SendStellarViewModel(
    val wallet: Wallet,
    private val sendToken: Token,
    val feeToken: Token,
    private val adapter: ISendStellarAdapter,
    val coinMaxAllowedDecimals: Int,
    private val xRateService: XRateService,
    private val address: Address,
    private val showAddressInput: Boolean,
    private val amountService: SendAmountService,
    private val addressService: SendStellarAddressService,
    private val contactsRepo: ContactsRepository,
    private val recentAddressManager: RecentAddressManager,
    private val minimumAmountService: SendStellarMinimumAmountService
) : ViewModelUiState<SendStellarUiState>() {
    private val fee = adapter.fee

    val blockchainType = wallet.token.blockchainType
    val feeTokenMaxAllowedDecimals = feeToken.decimals
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var minimumAmountState = minimumAmountService.stateFlow.value
    private var memo: String? = null

    var coinRate by mutableStateOf(xRateService.getRate(sendToken.coin.uid))
        private set
    var feeCoinRate by mutableStateOf(xRateService.getRate(feeToken.coin.uid))
        private set
    var sendResult by mutableStateOf<SendResult?>(null)
        private set

    private val logger: AppLogger = AppLogger("send-stellar")

    init {
//        addCloseable(feeService)

        viewModelScope.launch(Dispatchers.Default) {
            amountService.stateFlow.collect {
                handleUpdatedAmountState(it)
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            addressService.stateFlow.collect {
                handleUpdatedAddressState(it)
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            minimumAmountService.stateFlow.collect {
                handleUpdatedMinimumAmountState(it)
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            xRateService.getRateFlow(sendToken.coin.uid).collect {
                coinRate = it
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            xRateService.getRateFlow(feeToken.coin.uid).collect {
                feeCoinRate = it
            }
        }

        addressService.setAddress(address)
    }

    private fun handleUpdatedMinimumAmountState(state: SendStellarMinimumAmountService.State) {
        minimumAmountState = state

        amountService.setMinimumSendAmount(minimumAmountState.minimumAmount)

        emitState()
    }

    override fun createState() = SendStellarUiState(
        availableBalance = amountState.availableBalance,
        amountCaution = amountState.amountCaution,
        addressError = addressState.addressError,
        minimumAmountError = minimumAmountState.error,
        canBeSend = amountState.canBeSend && addressState.canBeSend && minimumAmountState.canBeSend,
        showAddressInput = showAddressInput,
        fee = fee,
        address = address
    )

    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    fun onEnterMemo(memo: String) {
        this.memo = memo.ifBlank { null }
    }

    private fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState

        emitState()
    }

    private suspend fun handleUpdatedAddressState(addressState: SendStellarAddressService.State) {
        this.addressState = addressState

        minimumAmountService.setValidAddress(addressState.validAddress)

        emitState()
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
            token = wallet.token,
            feeCoin = feeToken.coin,
            memo = memo,
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

            adapter.send(amountState.amount!!, addressState.address?.hex!!, memo)

            sendResult = SendResult.Sent()
            logger.info("success")

            recentAddressManager.setRecentAddress(addressState.address!!, BlockchainType.Ton)
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
}

data class SendStellarUiState(
    val availableBalance: BigDecimal?,
    val amountCaution: HSCaution?,
    val addressError: Throwable?,
    val minimumAmountError: Throwable?,
    val canBeSend: Boolean,
    val showAddressInput: Boolean,
    val fee: BigDecimal?,
    val address: Address,
)

