package io.horizontalsystems.bankwallet.modules.send.stellar

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
import io.horizontalsystems.bankwallet.core.ISendStellarAdapter
import io.horizontalsystems.bankwallet.core.LocalizedException
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.isNative
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
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException

@HiltViewModel(assistedFactory = SendStellarViewModel.Factory::class)
class SendStellarViewModel @AssistedInject constructor(
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
) : ViewModelUiState<SendStellarUiState>() {

    private val adapter: ISendStellarAdapter
    private val xRateService: XRateService
    private val amountService: SendAmountService
    private val addressService: SendStellarAddressService
    private val minimumAmountService: SendStellarMinimumAmountService
    val feeToken: Token
    val coinMaxAllowedDecimals = wallet.token.decimals
    val fiatMaxAllowedDecimals: Int
    val blockchainType = wallet.token.blockchainType
    val feeTokenMaxAllowedDecimals: Int
    private val sendToken = wallet.token
    private val showAddressInput = !hideAddress
    private val fee: BigDecimal?

    private var amountState: SendAmountService.State
    private var addressState: SendStellarAddressService.State
    private var minimumAmountState: SendStellarMinimumAmountService.State
    private var memo: String? = null

    var coinRate by mutableStateOf<CurrencyValue?>(null)
    var feeCoinRate by mutableStateOf<CurrencyValue?>(null)
    var sendResult by mutableStateOf<SendResult?>(null)
        private set

    private val logger: AppLogger = AppLogger("send-stellar")

    init {
        adapter = adapterManager.getAdapterForWallet<ISendStellarAdapter>(wallet)
            ?: throw IllegalStateException("ISendStellarAdapter is null")
        xRateService = XRateService(marketKit, currencyManager.baseCurrency)
        feeToken = coinManager.getToken(TokenQuery(BlockchainType.Stellar, TokenType.Native))
            ?: throw IllegalArgumentException()
        fiatMaxAllowedDecimals = appConfigProvider.fiatDecimal
        feeTokenMaxAllowedDecimals = feeToken.decimals
        fee = adapter.fee

        val amountValidator = AmountValidator()
        amountService = SendAmountService(
            amountValidator = amountValidator,
            coinCode = wallet.coin.code,
            availableBalance = adapter.maxSendableBalance,
            leaveSomeBalanceForFee = wallet.token.type.isNative
        )
        addressService = SendStellarAddressService()
        minimumAmountService = SendStellarMinimumAmountService(adapter)

        amountState = amountService.stateFlow.value
        addressState = addressService.stateFlow.value
        minimumAmountState = minimumAmountService.stateFlow.value

        coinRate = xRateService.getRate(sendToken.coin.uid)
        feeCoinRate = xRateService.getRate(feeToken.coin.uid)

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

            recentAddressManager.setRecentAddress(addressState.address!!, BlockchainType.Stellar)
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
        fun create(wallet: Wallet, address: Address, hideAddress: Boolean): SendStellarViewModel
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
