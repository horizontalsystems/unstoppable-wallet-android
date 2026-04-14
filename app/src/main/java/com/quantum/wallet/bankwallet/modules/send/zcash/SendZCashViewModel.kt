package com.quantum.wallet.bankwallet.modules.send.zcash

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.AppLogger
import com.quantum.wallet.bankwallet.core.HSCaution
import com.quantum.wallet.bankwallet.core.ISendZcashAdapter
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException

class SendZCashViewModel(
    private val adapter: ISendZcashAdapter,
    val wallet: Wallet,
    private val xRateService: XRateService,
    private val amountService: SendAmountService,
    private val addressService: SendZCashAddressService,
    private val memoService: SendZCashMemoService,
    private val contactsRepo: ContactsRepository,
    private val showAddressInput: Boolean,
    private val address: Address,
    private val recentAddressManager: RecentAddressManager
) : ViewModelUiState<SendZCashUiState>() {
    private val feeService = SendZcashFeeService(adapter, wallet.coin.code)

    val blockchainType = wallet.token.blockchainType
    val coinMaxAllowedDecimals = wallet.token.decimals
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal
    val memoMaxLength by memoService::memoMaxLength

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var memoState = memoService.stateFlow.value
    private var feeState = feeService.stateFlow.value

    var coinRate by mutableStateOf(xRateService.getRate(wallet.coin.uid))
        private set
    var sendResult by mutableStateOf<SendResult?>(null)
        private set

    private val logger = AppLogger("Send-${wallet.coin.code}")

    init {
        xRateService.getRateFlow(wallet.coin.uid).collectWith(viewModelScope) {
            coinRate = it
        }
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
            memoService.stateFlow.collect {
                handleUpdatedMemoState(it)
            }
        }
        viewModelScope.launch {
            feeService.stateFlow.collect {
                handleUpdatedFeeState(it)
            }
        }

        viewModelScope.launch {
            addressService.setAddress(address)
        }
    }

    override fun createState() = SendZCashUiState(
        availableBalance = amountState.availableBalance,
        addressError = addressState.addressError,
        amountCaution = amountState.amountCaution,
        memoIsAllowed = memoState.memoIsAllowed,
        canBeSend = amountState.canBeSend && addressState.canBeSend,
        showAddressInput = showAddressInput,
        address = address
    )

    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
        viewModelScope.launch {
            addressService.setAddress(address)
        }
    }

    fun onEnterMemo(memo: String) {
        memoService.setMemo(memo)
    }

    private suspend fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState

        feeService.setAmount(amountState.amount)

        emitState()
    }

    private suspend fun handleUpdatedAddressState(addressState: SendZCashAddressService.State) {
        this.addressState = addressState

        memoService.setAddressType(addressState.addressType)
        feeService.setAddress(addressState.address)

        emitState()
    }

    private suspend fun handleUpdatedMemoState(memoState: SendZCashMemoService.State) {
        this.memoState = memoState

        feeService.setMemo(memoState.memo)

        emitState()
    }

    private fun handleUpdatedFeeState(feeState: SendZcashFeeService.State) {
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
            fee = feeState.fee,
            address = address,
            contact = contact,
            token = wallet.token,
            feeCoin = wallet.coin,
            memo = memoState.memo,
            error = feeState.error
        )
    }

    fun onClickSend() {
        viewModelScope.launch {
            send()
        }
    }

    private suspend fun send() = withContext(Dispatchers.IO) {
        val logger = logger.getScopedUnique()
        logger.info("click")

        try {
            sendResult = SendResult.Sending

            val proposal = adapter.proposeTransfer(
                amountState.amount!!,
                addressState.address!!.hex,
                memoState.memo
            )

            logger.info("send proposal ready")
            sendResult = SendResult.Sent()

            val address = addressState.address!!
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    adapter.sendProposal(proposal)
                    logger.info("submitted")
                    recentAddressManager.setRecentAddress(address, BlockchainType.Zcash)
                } catch (e: Throwable) {
                    logger.warning("submission failed", e)
                }
            }
        } catch (e: Throwable) {
            logger.warning("failed", e)
            sendResult = SendResult.Failed(createCaution(e))
        }
    }

    private fun createCaution(error: Throwable) = when (error) {
        is UnknownHostException -> HSCaution(TranslatableString.ResString(R.string.Hud_Text_NoInternet))
        is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
        else -> HSCaution(TranslatableString.PlainString(error.message ?: ""))
    }
}

data class SendZCashUiState(
    val availableBalance: BigDecimal,
    val addressError: Throwable?,
    val amountCaution: HSCaution?,
    val memoIsAllowed: Boolean,
    val canBeSend: Boolean,
    val showAddressInput: Boolean,
    val address: Address,
)
