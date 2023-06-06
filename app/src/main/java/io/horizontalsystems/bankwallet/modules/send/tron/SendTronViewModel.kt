package cash.p.terminal.modules.send.tron

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.HSCaution
import cash.p.terminal.core.ISendTronAdapter
import cash.p.terminal.core.LocalizedException
import cash.p.terminal.entities.Address
import cash.p.terminal.entities.ViewState
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.send.SendAmountAdvancedService
import cash.p.terminal.modules.send.SendResult
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.tronkit.transaction.Fee
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException
import io.horizontalsystems.tronkit.models.Address as TronAddress

class SendTronViewModel(
    val wallet: Wallet,
    val sendToken: Token,
    val feeToken: Token,
    val adapter: ISendTronAdapter,
    private val xRateService: XRateService,
    private val amountService: SendAmountAdvancedService,
    private val addressService: SendTronAddressService,
    val coinMaxAllowedDecimals: Int,
    private val contactsRepo: ContactsRepository
) : ViewModel() {
    val blockchainType = wallet.token.blockchainType
    val feeTokenMaxAllowedDecimals = feeToken.decimals
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var feeState: FeeState = FeeState.Loading

    var uiState by mutableStateOf(
        SendUiState(
            availableBalance = amountState.availableBalance,
            amountCaution = amountState.amountCaution,
            addressError = addressState.addressError,
            proceedEnabled = amountState.canBeSend && addressState.canBeSend,
            sendEnabled = feeState is FeeState.Success,
            feeViewState = feeState.viewState,
            cautions = listOf()
        )
    )
        private set

    var coinRate by mutableStateOf(xRateService.getRate(sendToken.coin.uid))
        private set
    var feeCoinRate by mutableStateOf(xRateService.getRate(feeToken.coin.uid))
        private set
    var confirmationData by mutableStateOf<SendTronConfirmationData?>(null)
        private set
    var sendResult by mutableStateOf<SendResult?>(null)
        private set

    private val decimalAmount: BigDecimal
        get() = amountState.evmAmount!!.toBigDecimal().movePointLeft(sendToken.decimals)

    init {
        viewModelScope.launch {
            amountService.stateFlow.collect {
                handleUpdatedAmountState(it)
            }
        }
        viewModelScope.launch {
            addressService.stateFlow.collect {
                handleUpdatedAddressState(it)
            }
        }
        viewModelScope.launch {
            xRateService.getRateFlow(sendToken.coin.uid).collect {
                coinRate = it
            }
        }
        viewModelScope.launch {
            xRateService.getRateFlow(feeToken.coin.uid).collect {
                feeCoinRate = it
            }
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

    fun onNavigateToConfirmation() {
        val address = addressState.address!!
        val contact = contactsRepo.getContactsFiltered(
            blockchainType = blockchainType,
            addressQuery = address.hex
        ).firstOrNull()

        confirmationData = SendTronConfirmationData(
            amount = decimalAmount,
            fee = null,
            activationFee = null,
            resourcesConsumed = null,
            address = address,
            contact = contact,
            coin = wallet.coin,
            feeCoin = feeToken.coin,
            isInactiveAddress = addressState.isInactiveAddress
        )

        estimateFee(decimalAmount, address)
    }

    private fun estimateFee(amount: BigDecimal, toAddress: Address) {
        viewModelScope.launch {
            confirmationData = try {
                feeState = FeeState.Loading
                emitState()

                val amountBigInteger = amount.movePointRight(sendToken.decimals).toBigInteger()
                val tronAddress = TronAddress.fromBase58(toAddress.hex)
                val fees = adapter.estimateFee(amountBigInteger, tronAddress)

                var activationFee: BigDecimal? = null
                var bandwidth: String? = null
                var energy: String? = null

                fees.forEach { fee ->
                    when (fee) {
                        is Fee.AccountActivation -> {
                            activationFee = fee.feeInSuns.toBigDecimal().movePointLeft(feeToken.decimals)
                        }

                        is Fee.Bandwidth -> {
                            bandwidth = "${fee.points} Bandwidth"
                        }

                        is Fee.Energy -> {
                            val formattedEnergy = App.numberFormatter.formatNumberShort(fee.required.toBigDecimal() + BigDecimal(10_000), 0)
                            energy = "$formattedEnergy Energy"
                        }
                    }
                }

                val resourcesConsumed = if (bandwidth != null) {
                    bandwidth + (energy?.let { " \n + $it" } ?: "")
                } else {
                    energy
                }

                feeState = FeeState.Success(fees)
                emitState()

                val totalFee = fees.sumOf { it.feeInSuns }.toBigDecimal().movePointLeft(feeToken.decimals)
                confirmationData?.copy(
                    fee = totalFee,
                    activationFee = activationFee,
                    resourcesConsumed = resourcesConsumed
                )


            } catch (error: Throwable) {
                feeState = FeeState.Error(error)
                emitState()

                confirmationData?.copy(fee = null, activationFee = null, resourcesConsumed = null)
            }
        }
    }

    fun onClickSend() {
        viewModelScope.launch {
            send()
        }
    }

    private suspend fun send() = withContext(Dispatchers.IO) {
        try {
            sendResult = SendResult.Sending

            adapter.send(amountState.evmAmount!!, addressState.tronAddress!!, feeState.feeLimit)

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

    private fun handleUpdatedAmountState(amountState: SendAmountAdvancedService.State) {
        this.amountState = amountState

        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendTronAddressService.State) {
        this.addressState = addressState

        emitState()
    }

    private fun emitState() {
        uiState = SendUiState(
            availableBalance = amountState.availableBalance,
            amountCaution = amountState.amountCaution,
            addressError = addressState.addressError,
            proceedEnabled = amountState.canBeSend && addressState.canBeSend,
            sendEnabled = feeState is FeeState.Success,
            feeViewState = feeState.viewState,
            cautions = (feeState as? FeeState.Error)?.let { listOf(createCaution(it.error)) } ?: listOf()
        )
    }
}

sealed class FeeState {
    object Loading : FeeState()
    data class Success(val fees: List<Fee>) : FeeState()
    data class Error(val error: Throwable) : FeeState()

    val viewState: ViewState
        get() = when (this) {
            is Error -> ViewState.Error(error)
            Loading -> ViewState.Loading
            is Success -> ViewState.Success
        }

    val feeLimit: Long?
        get() = when (this) {
            is Error -> null
            Loading -> null
            is Success -> {
                (fees.find { it is Fee.Energy } as? Fee.Energy)?.feeInSuns
            }
        }
}
