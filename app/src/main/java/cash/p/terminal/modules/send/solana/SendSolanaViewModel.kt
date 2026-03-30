package cash.p.terminal.modules.send.solana

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import cash.p.terminal.R
import cash.p.terminal.core.EvmError
import cash.p.terminal.core.HSCaution
import cash.p.terminal.core.ISendSolanaAdapter
import cash.p.terminal.core.LocalizedException
import cash.p.terminal.core.managers.ConnectivityManager
import cash.p.terminal.core.managers.PendingTransactionRegistrar
import cash.p.terminal.core.managers.RecentAddressManager
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.entities.Address
import cash.p.terminal.entities.PendingTransactionDraft
import cash.p.terminal.modules.amount.SendAmountService
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.send.SendConfirmationData
import cash.p.terminal.modules.send.SendErrorInsufficientBalance
import cash.p.terminal.modules.send.SendResult
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.modules.send.BaseSendViewModel
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.solanakit.SolanaKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import java.math.BigDecimal
import java.net.UnknownHostException
import kotlin.getValue

class SendSolanaViewModel(
    wallet: Wallet,
    val sendToken: Token,
    override val feeToken: Token,
    val solBalance: BigDecimal,
    val adapter: ISendSolanaAdapter,
    xRateService: XRateService,
    private val amountService: SendAmountService,
    private val addressService: SendSolanaAddressService,
    val coinMaxAllowedDecimals: Int,
    private val contactsRepo: ContactsRepository,
    private val showAddressInput: Boolean,
    private val connectivityManager: ConnectivityManager,
    address: Address?,
    private val pendingRegistrar: PendingTransactionRegistrar,
    private val adapterManager: IAdapterManager
) : BaseSendViewModel<SendSolanaModule.SendUiState>(wallet, adapterManager) {
    val blockchainType = wallet.token.blockchainType
    val feeTokenMaxAllowedDecimals = feeToken.decimals
    val fiatMaxAllowedDecimals = AppConfigProvider.fiatDecimal

    private val recentAddressManager: RecentAddressManager by inject(RecentAddressManager::class.java)

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var pendingTxId: String? = null

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

        addressService.setAddress(address)
    }

    override fun createState(): SendSolanaModule.SendUiState {
        val poison = isAddressSuspicious(addressState.address?.hex)
        return SendSolanaModule.SendUiState(
            availableBalance = amountState.availableBalance,
            amountCaution = amountState.amountCaution,
            addressError = addressState.addressError,
            canBeSend = amountState.canBeSend && addressState.canBeSend && (!poison || riskAccepted),
            showAddressInput = showAddressInput,
            address = addressState.address,
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
            fee = SolanaKit.fee,
            address = address,
            contact = contact,
            coin = wallet.coin,
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
        return connectivityManager.isConnected.value
    }

    private suspend fun send() = withContext(Dispatchers.IO) {
        if (!hasConnection()) {
            sendResult = SendResult.Failed(createCaution(UnknownHostException()))
            return@withContext
        }

        try {
            sendResult = SendResult.Sending

            val totalSolAmount = (if (sendToken.type == TokenType.Native) decimalAmount else BigDecimal.ZERO) + SolanaKit.fee

            val availableBalance = adapterManager.getAdjustedBalanceData(wallet)?.available
                ?: solBalance
            if (totalSolAmount > availableBalance)
                throw EvmError.InsufficientBalanceWithFee

            // 1. Create pending transaction draft BEFORE sending
            val sdkBalance = adapterManager.getBalanceAdapterForWallet(wallet)
                ?.balanceData?.available ?: solBalance
            val draft = PendingTransactionDraft(
                wallet = wallet,
                token = sendToken,
                amount = decimalAmount,
                fee = SolanaKit.fee,
                sdkBalanceAtCreation = sdkBalance,
                fromAddress = "",
                toAddress = addressState.address!!.hex
            )

            // 2. Register pending transaction
            pendingTxId = pendingRegistrar.register(draft)

            // 3. Broadcast transaction
            val transaction = adapter.send(decimalAmount, addressState.solanaAddress!!)
            pendingTxId?.let { pendingRegistrar.updateTxId(it, transaction.transaction.hash) }

            onSendSuccess(addressState.address?.hex)
            sendResult = SendResult.Sent()

            recentAddressManager.setRecentAddress(addressState.address!!, BlockchainType.Solana)
        } catch (e: Throwable) {
            pendingTxId?.let { pendingRegistrar.deleteFailed(it) }
            sendResult = SendResult.Failed(createCaution(e))
        }
    }

    private fun createCaution(error: Throwable) = when (error) {
        is UnknownHostException -> HSCaution(TranslatableString.ResString(R.string.Hud_Text_NoInternet))
        is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
        is EvmError.InsufficientBalanceWithFee -> SendErrorInsufficientBalance(feeToken.coin.code, solBalance.toPlainString())
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

}
