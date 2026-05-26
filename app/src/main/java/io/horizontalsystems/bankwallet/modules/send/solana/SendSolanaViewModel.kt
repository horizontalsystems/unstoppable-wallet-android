package io.horizontalsystems.bankwallet.modules.send.solana

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.EvmError
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ISendSolanaAdapter
import io.horizontalsystems.bankwallet.core.LocalizedException
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.adapters.SolanaAdapter
import io.horizontalsystems.bankwallet.core.isNative
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.RecentAddressManager
import io.horizontalsystems.bankwallet.core.managers.SolanaKitManager
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationData
import io.horizontalsystems.bankwallet.modules.send.SendErrorInsufficientBalance
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.solanakit.SolanaKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.UnknownHostException

@HiltViewModel(assistedFactory = SendSolanaViewModel.Factory::class)
class SendSolanaViewModel @AssistedInject constructor(
    @Assisted val wallet: Wallet,
    @Assisted private val address: Address,
    @Assisted private val hideAddress: Boolean,
    adapterManager: IAdapterManager,
    marketKit: MarketKitWrapper,
    currencyManager: CurrencyManager,
    coinManager: ICoinManager,
    solanaKitManager: SolanaKitManager,
    private val contactsRepo: ContactsRepository,
    private val connectivityManager: ConnectivityManager,
    private val recentAddressManager: RecentAddressManager,
    appConfigProvider: AppConfigProvider,
) : ViewModelUiState<SendSolanaUiState>() {

    private val adapter: ISendSolanaAdapter
    private val xRateService: XRateService
    private val amountService: SendAmountService
    private val addressService: SendSolanaAddressService
    val feeToken: Token
    val solBalance: BigDecimal
    val coinMaxAllowedDecimals = wallet.token.decimals
    val fiatMaxAllowedDecimals: Int
    val blockchainType = wallet.token.blockchainType
    val feeTokenMaxAllowedDecimals: Int
    val sendToken = wallet.token
    private val showAddressInput = !hideAddress

    private var amountState: SendAmountService.State
    private var addressState: SendSolanaAddressService.State

    var coinRate by mutableStateOf<CurrencyValue?>(null)
        private set
    var feeCoinRate by mutableStateOf<CurrencyValue?>(null)
        private set
    var sendResult by mutableStateOf<SendResult?>(null)
        private set

    private val decimalAmount: BigDecimal
        get() = amountState.amount!!

    private val logger = AppLogger("send-solana")

    init {
        adapter = adapterManager.getAdapterForWallet<ISendSolanaAdapter>(wallet)
            ?: throw IllegalStateException("SendSolanaAdapter is null")
        xRateService = XRateService(marketKit, currencyManager.baseCurrency)
        feeToken = coinManager.getToken(TokenQuery(BlockchainType.Solana, TokenType.Native))
            ?: throw IllegalArgumentException()
        fiatMaxAllowedDecimals = appConfigProvider.fiatDecimal
        feeTokenMaxAllowedDecimals = feeToken.decimals

        val balance = solanaKitManager.solanaKitWrapper?.solanaKit?.balance ?: 0L
        solBalance = SolanaAdapter.balanceInBigDecimal(balance, feeToken.decimals) - SolanaKit.accountRentAmount

        amountService = SendAmountService(
            AmountValidator(),
            wallet.token.coin.code,
            adapter.availableBalance.setScale(coinMaxAllowedDecimals, RoundingMode.DOWN),
            wallet.token.type.isNative,
        )
        addressService = SendSolanaAddressService()

        amountState = amountService.stateFlow.value
        addressState = addressService.stateFlow.value

        coinRate = xRateService.getRate(sendToken.coin.uid)
        feeCoinRate = xRateService.getRate(feeToken.coin.uid)

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

        addressService.setAddress(address)
    }

    override fun createState() = SendSolanaUiState(
        availableBalance = amountState.availableBalance,
        amountCaution = amountState.amountCaution,
        addressError = addressState.addressError,
        canBeSend = amountState.canBeSend && addressState.canBeSend,
        showAddressInput = showAddressInput,
        address = address,
    )

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
            token = wallet.token,
            feeCoin = feeToken.coin,
            memo = null
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
        if (!hasConnection()) {
            sendResult = SendResult.Failed(createCaution(UnknownHostException()))
            return@withContext
        }

        try {
            sendResult = SendResult.Sending

            val totalSolAmount = (if (sendToken.type == TokenType.Native) decimalAmount else BigDecimal.ZERO) + SolanaKit.fee

            if (totalSolAmount > solBalance)
                throw EvmError.InsufficientBalanceWithFee

            adapter.send(decimalAmount, addressState.solanaAddress!!)

            sendResult = SendResult.Sent()

            recentAddressManager.setRecentAddress(addressState.address!!, BlockchainType.Solana)
        } catch (e: Throwable) {
            sendResult = SendResult.Failed(createCaution(e))
        }
    }

    private fun createCaution(error: Throwable) = when (error) {
        is UnknownHostException -> HSCaution(TranslatableString.ResString(R.string.Hud_Text_NoInternet))
        is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
        is EvmError.InsufficientBalanceWithFee -> SendErrorInsufficientBalance(feeToken.coin.code)
        else -> HSCaution(TranslatableString.PlainString(error.cause?.message ?: error.message ?: ""))
    }

    private fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState
        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendSolanaAddressService.State) {
        this.addressState = addressState
        emitState()
    }

    @AssistedFactory
    interface Factory {
        fun create(wallet: Wallet, address: Address, hideAddress: Boolean): SendSolanaViewModel
    }
}

data class SendSolanaUiState(
    val availableBalance: BigDecimal,
    val amountCaution: HSCaution?,
    val addressError: Throwable?,
    val canBeSend: Boolean,
    val showAddressInput: Boolean,
    val address: Address,
)
