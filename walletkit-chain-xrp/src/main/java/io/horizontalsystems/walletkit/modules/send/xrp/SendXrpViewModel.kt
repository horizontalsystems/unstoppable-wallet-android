package io.horizontalsystems.walletkit.modules.send.xrp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.AppLogger
import io.horizontalsystems.walletkit.core.HSCaution
import io.horizontalsystems.walletkit.core.ISendXrpAdapter
import io.horizontalsystems.walletkit.core.LocalizedException
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.managers.RecentAddressManager
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.amount.SendAmountService
import io.horizontalsystems.walletkit.modules.contacts.ContactsRepository
import io.horizontalsystems.walletkit.modules.send.SendConfirmationData
import io.horizontalsystems.walletkit.modules.send.SendResult
import io.horizontalsystems.walletkit.modules.xrate.XRateService
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException

class SendXrpViewModel(
    val wallet: Wallet,
    private val sendToken: Token,
    val feeToken: Token,
    private val adapter: ISendXrpAdapter,
    val coinMaxAllowedDecimals: Int,
    private val xRateService: XRateService,
    private val address: Address,
    private val showAddressInput: Boolean,
    private val amountService: SendAmountService,
    private val addressService: SendXrpAddressService,
    private val contactsRepo: ContactsRepository,
    private val recentAddressManager: RecentAddressManager,
    private val destinationService: SendXrpDestinationService,
) : ViewModelUiState<SendXrpUiState>() {
    private val fee = adapter.fee

    val blockchainType = wallet.token.blockchainType
    val feeTokenMaxAllowedDecimals = feeToken.decimals
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var destinationState = destinationService.stateFlow.value
    private var memo: String? = null

    var coinRate by mutableStateOf(xRateService.getRate(sendToken.coin.uid))
        private set
    var feeCoinRate by mutableStateOf(xRateService.getRate(feeToken.coin.uid))
        private set
    var sendResult by mutableStateOf<SendResult?>(null)
        private set

    private val logger: AppLogger = AppLogger("send-xrp")

    init {
        viewModelScope.launch(Dispatchers.Default) {
            amountService.stateFlow.collect { handleUpdatedAmountState(it) }
        }
        viewModelScope.launch(Dispatchers.Default) {
            addressService.stateFlow.collect { handleUpdatedAddressState(it) }
        }
        viewModelScope.launch(Dispatchers.Default) {
            destinationService.stateFlow.collect { handleUpdatedDestinationState(it) }
        }
        viewModelScope.launch(Dispatchers.Default) {
            xRateService.getRateFlow(sendToken.coin.uid).collect { coinRate = it }
        }
        viewModelScope.launch(Dispatchers.Default) {
            xRateService.getRateFlow(feeToken.coin.uid).collect { feeCoinRate = it }
        }

        addressService.setAddress(address)
    }

    override fun createState() = SendXrpUiState(
        availableBalance = amountState.availableBalance,
        amountCaution = amountState.amountCaution,
        addressError = addressState.addressError,
        destinationError = destinationState.error,
        destinationTagRequired = destinationState.tagRequired,
        fixedDestinationTag = addressState.xAddressTag,
        canBeSend = amountState.canBeSend && addressState.canBeSend && destinationState.canBeSend,
        showAddressInput = showAddressInput,
        fee = fee,
        address = address,
    )

    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    fun onEnterDestinationTag(input: String) {
        destinationService.setTagInput(input)
    }

    fun onEnterMemo(memo: String) {
        this.memo = memo.ifBlank { null }
    }

    private fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState
        emitState()
    }

    private suspend fun handleUpdatedAddressState(addressState: SendXrpAddressService.State) {
        this.addressState = addressState
        destinationService.setFixedTag(addressState.xAddressTag)
        destinationService.setValidAddress(addressState.validAddress)
        emitState()
    }

    private fun handleUpdatedDestinationState(state: SendXrpDestinationService.State) {
        destinationState = state
        amountService.setMinimumSendAmount(state.minimumAmount)
        emitState()
    }

    val destinationTag: Long? get() = destinationService.destinationTag

    /**
     * Confirmation data for the current input, or null when it isn't available (the pieces
     * do not survive process death; null sends the user back to the form instead of crashing).
     */
    fun getConfirmationData(): SendConfirmationData? {
        val address = addressState.address ?: return null
        val contact = contactsRepo.getContactsFiltered(blockchainType, addressQuery = address.hex).firstOrNull()
        return SendConfirmationData(
            amount = amountState.amount ?: return null,
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
        viewModelScope.launch { send() }
    }

    private suspend fun send() = withContext(Dispatchers.IO) {
        try {
            sendResult = SendResult.Sending
            logger.info("sending tx")

            adapter.send(amountState.amount!!, addressState.address?.hex!!, destinationService.destinationTag, memo)

            sendResult = SendResult.Sent()
            logger.info("success")

            recentAddressManager.setRecentAddress(addressState.address!!, BlockchainType.Xrp)
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

data class SendXrpUiState(
    val availableBalance: BigDecimal?,
    val amountCaution: HSCaution?,
    val addressError: Throwable?,
    val destinationError: Throwable?,
    val destinationTagRequired: Boolean,
    /** Tag packed in the entered X-address; the tag field is locked to it. */
    val fixedDestinationTag: Long?,
    val canBeSend: Boolean,
    val showAddressInput: Boolean,
    val fee: BigDecimal?,
    val address: Address,
)
