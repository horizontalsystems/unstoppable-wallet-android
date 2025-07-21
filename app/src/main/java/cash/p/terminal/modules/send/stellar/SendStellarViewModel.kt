package cash.p.terminal.modules.send.stellar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.HSCaution
import cash.p.terminal.core.ISendStellarAdapter
import cash.p.terminal.core.LocalizedException
import cash.p.terminal.core.managers.RecentAddressManager
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.amount.SendAmountService
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.send.SendConfirmationData
import cash.p.terminal.modules.send.SendResult
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.ViewModelUiState
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.logger.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import cash.p.terminal.R
import org.koin.java.KoinJavaComponent.inject
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
    private val minimumAmountService: SendStellarMinimumAmountService
) : ViewModelUiState<SendStellarUiState>() {
    private val fee = adapter.fee

    private val recentAddressManager: RecentAddressManager by inject(RecentAddressManager::class.java)

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
            coin = wallet.coin,
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

