package io.horizontalsystems.walletkit.modules.send.zano

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.AppLogger
import io.horizontalsystems.walletkit.core.coinCodeWithNetwork
import io.horizontalsystems.walletkit.core.HSCaution
import io.horizontalsystems.walletkit.core.ISendZanoAdapter
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
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import io.horizontalsystems.zanokit.NodeUnreachableException
import java.net.UnknownHostException

class SendZanoViewModel(
    val wallet: Wallet,
    private val sendToken: Token,
    val feeToken: Token,
    private val adapter: ISendZanoAdapter,
    val coinMaxAllowedDecimals: Int,
    private val xRateService: XRateService,
    private val address: Address,
    private val showAddressInput: Boolean,
    private val amountService: SendAmountService,
    private val addressService: SendZanoAddressService,
    private val feeService: SendZanoFeeService,
    private val contactsRepo: ContactsRepository,
    private val recentAddressManager: RecentAddressManager,
) : ViewModelUiState<SendZanoUiState>() {
    val blockchainType = wallet.token.blockchainType
    val feeTokenMaxAllowedDecimals = feeToken.decimals
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var feeState = feeService.stateFlow.value
    private var memo: String? = null

    var coinRate by mutableStateOf(xRateService.getRate(sendToken.coin.uid))
        private set
    var feeCoinRate by mutableStateOf(xRateService.getRate(feeToken.coin.uid))
        private set
    var sendResult by mutableStateOf<SendResult?>(null)
        private set

    private val logger: AppLogger = AppLogger("send-zano")

    init {
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

    override fun createState(): SendZanoUiState {
        val feeCaution = feeCaution()
        return SendZanoUiState(
            availableBalance = amountState.availableBalance,
            amountCaution = amountState.amountCaution,
            feeCaution = feeCaution,
            addressError = addressState.addressError,
            canBeSend = amountState.canBeSend && addressState.canBeSend && feeState.fee != null && feeCaution == null,
            showAddressInput = showAddressInput,
            fee = feeState.fee,
            feeInProgress = feeState.inProgress,
            address = address
        )
    }

    // The fee is always paid in native ZANO. For confidential assets the sent amount
    // and the fee come from different balances, so the amount validator alone can't
    // catch an unaffordable fee.
    private fun feeCaution(): HSCaution? {
        val fee = feeState.fee ?: return null
        val insufficient = if (adapter.isNativeAsset) {
            val amount = amountState.amount ?: return null
            amount + fee > adapter.nativeAvailableBalance
        } else {
            fee > adapter.nativeAvailableBalance
        }

        if (!insufficient) return null

        return HSCaution(
            s = TranslatableString.ResString(R.string.EthereumTransaction_Error_InsufficientBalance_Title),
            type = HSCaution.Type.Error,
            description = TranslatableString.ResString(
                R.string.EthereumTransaction_Error_InsufficientBalanceForFee,
                feeToken.coinCodeWithNetwork
            )
        )
    }

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
        is NodeUnreachableException -> HSCaution(TranslatableString.ResString(R.string.Send_Error_NodeUnreachable))
        is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
        else -> HSCaution(TranslatableString.PlainString(error.message ?: ""))
    }
}

data class SendZanoUiState(
    val availableBalance: BigDecimal?,
    val amountCaution: HSCaution?,
    val feeCaution: HSCaution?,
    val addressError: Throwable?,
    val canBeSend: Boolean,
    val showAddressInput: Boolean,
    val fee: BigDecimal?,
    val feeInProgress: Boolean,
    val address: Address,
)
