package cash.p.terminal.modules.send.evm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.HSCaution
import cash.p.terminal.core.ISendEthereumAdapter
import cash.p.terminal.core.LocalizedException
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.amount.SendAmountService
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionData
import cash.p.terminal.modules.multiswap.sendtransaction.services.SendTransactionServiceEvm
import cash.p.terminal.modules.send.SendConfirmationData
import cash.p.terminal.modules.send.SendResult
import cash.p.terminal.modules.send.SendUiState
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.z.ecc.android.sdk.ext.collectWith
import com.tangem.common.extensions.isZero
import cash.p.terminal.modules.send.BaseSendViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import java.math.BigDecimal
import java.net.UnknownHostException

internal class SendEvmViewModel(
    wallet: Wallet,
    sendToken: Token,
    val adapter: ISendEthereumAdapter,
    private val sendTransactionService: SendTransactionServiceEvm,
    xRateService: XRateService,
    private val amountService: SendAmountService,
    private val addressService: SendEvmAddressService,
    val coinMaxAllowedDecimals: Int,
    private val showAddressInput: Boolean,
    address: Address?,
    adapterManager: IAdapterManager
) : BaseSendViewModel<SendUiState>(wallet, adapterManager) {
    val fiatMaxAllowedDecimals = AppConfigProvider.fiatDecimal
    val blockchainType = wallet.token.blockchainType

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value

    private val contactsRepository: ContactsRepository by inject(ContactsRepository::class.java)
    override val feeToken = App.evmBlockchainManager.getBaseToken(blockchainType)
        ?: throw IllegalArgumentException()
    val feeTokenMaxAllowedDecimals = feeToken.decimals

    var coinRate by mutableStateOf(xRateService.getRate(sendToken.coin.uid))
        private set
    var feeCoinRate by mutableStateOf(xRateService.getRate(feeToken.coin.uid))
        private set
    var sendResult by mutableStateOf<SendResult?>(null)
        private set

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

        sendTransactionService.stateFlow.collectWith(viewModelScope) {
            emitState()
        }

        sendTransactionService.start(viewModelScope)

        amountService.stateFlow.onEach { newAmountState ->
            addressState.address?.let { address ->
                val amount = newAmountState.amount ?: BigDecimal.ZERO
                sendTransactionService.setSendTransactionData(
                    SendTransactionData.Evm(
                        adapter.getTransactionData(
                            amount,
                            io.horizontalsystems.ethereumkit.models.Address(address.hex)
                        ),
                        null,
                        amount = amount
                    )
                )
            }
        }.launchIn(viewModelScope)

    }

    override fun createState(): SendUiState {
        val txState = sendTransactionService.stateFlow.value
        val hasSendData = amountMoreThanZero() && addressState.address != null
        return SendUiState(
            availableBalance = amountState.availableBalance,
            amountCaution = amountState.amountCaution,
            addressError = addressState.addressError,
            canBeSend = amountState.canBeSend && addressState.canBeSend && txState.sendable,
            showAddressInput = showAddressInput,
            address = addressState.address,
            cautions = if (hasSendData) txState.cautions else emptyList(),
            fee = txState.networkFee?.primary?.value,
            feeLoading = hasSendData && txState.loading,
        )
    }

    private fun amountMoreThanZero(): Boolean {
        val amount = amountService.stateFlow.value.amount
        return amount != null && !amount.isZero()
    }

    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
        addressService.setAddress(address)
    }

    fun onClickSend() = viewModelScope.launch(Dispatchers.Default) {
        sendResult = try {
            val sendResult = sendTransactionService.sendTransaction()
            SendResult.Sent(sendResult.getRecordUid())
        } catch (e: Throwable) {
            SendResult.Failed(createCaution(e))
        }
    }

    fun getConfirmationData(): SendConfirmationData {
        val address = requireNotNull(addressState.address)
        val fee = sendTransactionService.stateFlow.value.networkFee?.primary?.value

        val contact = contactsRepository.getContactsFiltered(
            wallet.token.blockchainType,
            addressQuery = address.hex
        ).firstOrNull()
        return SendConfirmationData(
            amount = amountState.amount!!,
            fee = fee,
            address = address,
            contact = contact,
            coin = wallet.token.coin,
            feeCoin = feeToken.coin,
            memo = null,
        )
    }

    private fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState
        updateSendTransactionData()
        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendEvmAddressService.State) {
        this.addressState = addressState
        updateSendTransactionData()
        emitState()
    }

    private fun updateSendTransactionData() {
        val amount = amountState.amount ?: return
        val address = addressState.address ?: return
        viewModelScope.launch {
            sendTransactionService.setSendTransactionData(
                SendTransactionData.Evm(
                    adapter.getTransactionData(
                        amount,
                        io.horizontalsystems.ethereumkit.models.Address(address.hex)
                    ),
                    null
                )
            )
        }
    }

    private fun createCaution(error: Throwable) = when (error) {
        is UnknownHostException -> HSCaution(TranslatableString.ResString(R.string.Hud_Text_NoInternet))
        is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
        else -> HSCaution(TranslatableString.PlainString(error.message ?: ""))
    }
}
