package io.horizontalsystems.bankwallet.modules.send.zano

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ISendZanoAdapter
import io.horizontalsystems.bankwallet.core.LocalizedException
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.RecentAddressManager
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationData
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException

@HiltViewModel(assistedFactory = SendZanoViewModel.Factory::class)
class SendZanoViewModel @AssistedInject constructor(
    @Assisted val wallet: Wallet,
    @Assisted private val address: Address,
    @Assisted private val hideAddress: Boolean,
    adapterManager: IAdapterManager,
    marketKit: MarketKitWrapper,
    currencyManager: CurrencyManager,
    coinManager: ICoinManager,
    private val contactsRepo: ContactsRepository,
    private val recentAddressManager: RecentAddressManager,
    appConfigProvider: AppConfigProvider,
) : ViewModelUiState<SendZanoUiState>() {

    private val adapter: ISendZanoAdapter
    private val xRateService: XRateService
    private val amountService: SendAmountService
    private val addressService: SendZanoAddressService
    private val feeService: SendZanoFeeService
    val feeToken: Token
    val coinMaxAllowedDecimals = wallet.token.decimals
    val fiatMaxAllowedDecimals: Int
    val blockchainType = wallet.token.blockchainType
    val feeTokenMaxAllowedDecimals: Int
    private val sendToken = wallet.token
    private val showAddressInput = !hideAddress

    private var amountState: SendAmountService.State
    private var addressState: SendZanoAddressService.State
    private var feeState: SendZanoFeeService.State
    private var memo: String? = null

    var coinRate by mutableStateOf<CurrencyValue?>(null)
        private set
    var feeCoinRate by mutableStateOf<CurrencyValue?>(null)
        private set
    var sendResult by mutableStateOf<SendResult?>(null)
        private set

    private val logger: AppLogger = AppLogger("send-zano")

    init {
        adapter = adapterManager.getAdapterForWallet<ISendZanoAdapter>(wallet)
            ?: throw IllegalStateException("ISendZanoAdapter is null")
        xRateService = XRateService(marketKit, currencyManager.baseCurrency)
        feeToken = coinManager.getToken(TokenQuery(wallet.token.blockchainType, TokenType.Native))
            ?: throw IllegalArgumentException("Missing token for fee: ${TokenQuery(wallet.token.blockchainType, TokenType.Native)}")
        fiatMaxAllowedDecimals = appConfigProvider.fiatDecimal
        feeTokenMaxAllowedDecimals = feeToken.decimals

        amountService = SendAmountService(
            amountValidator = AmountValidator(),
            coinCode = wallet.coin.code,
            availableBalance = adapter.balanceData.available
        )
        addressService = SendZanoAddressService()
        feeService = SendZanoFeeService(adapter)

        amountState = amountService.stateFlow.value
        addressState = addressService.stateFlow.value
        feeState = feeService.stateFlow.value

        coinRate = xRateService.getRate(sendToken.coin.uid)
        feeCoinRate = xRateService.getRate(feeToken.coin.uid)

        addCloseable(feeService)

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
            feeService.stateFlow.collect {
                handleUpdatedFeeState(it)
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

    override fun createState() = SendZanoUiState(
        availableBalance = amountState.availableBalance,
        amountCaution = amountState.amountCaution,
        addressError = addressState.addressError,
        canBeSend = amountState.canBeSend && addressState.canBeSend && feeState.fee != null,
        showAddressInput = showAddressInput,
        fee = feeState.fee,
        feeInProgress = feeState.inProgress,
        address = address
    )

    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    fun onEnterMemo(memo: String) {
        this.memo = memo.ifBlank { null }
        feeService.setMemo(this.memo)
    }

    private fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState
        feeService.setAmount(amountState.amount)
        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendZanoAddressService.State) {
        this.addressState = addressState
        feeService.setAddress(addressState.address)
        emitState()
    }

    private fun handleUpdatedFeeState(feeState: SendZanoFeeService.State) {
        this.feeState = feeState
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
            fee = feeState.fee!!,
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

            recentAddressManager.setRecentAddress(addressState.address!!, wallet.token.blockchainType)
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

    @AssistedFactory
    interface Factory {
        fun create(wallet: Wallet, address: Address, hideAddress: Boolean): SendZanoViewModel
    }
}

data class SendZanoUiState(
    val availableBalance: BigDecimal?,
    val amountCaution: HSCaution?,
    val addressError: Throwable?,
    val canBeSend: Boolean,
    val showAddressInput: Boolean,
    val fee: BigDecimal?,
    val feeInProgress: Boolean,
    val address: Address,
)
