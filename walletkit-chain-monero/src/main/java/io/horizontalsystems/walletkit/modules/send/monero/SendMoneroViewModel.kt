package io.horizontalsystems.walletkit.modules.send.monero

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.AppLogger
import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.HSCaution
import io.horizontalsystems.walletkit.core.IBalanceAdapter
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.IMoneroAccountsAdapter
import io.horizontalsystems.walletkit.core.ISendMoneroAdapter
import io.horizontalsystems.walletkit.core.LocalizedException
import io.horizontalsystems.walletkit.core.MoneroUnspentOutput
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.managers.RecentAddressManager
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.amount.SendAmountService
import io.horizontalsystems.walletkit.modules.contacts.ContactsRepository
import io.horizontalsystems.walletkit.modules.send.SendConfirmationData
import io.horizontalsystems.walletkit.modules.send.SendResult
import io.horizontalsystems.walletkit.modules.send.bitcoin.UtxoData
import io.horizontalsystems.walletkit.modules.send.bitcoin.UtxoType
import io.horizontalsystems.walletkit.modules.xrate.XRateService
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException

class SendMoneroViewModel(
    val wallet: Wallet,
    private val sendToken: Token,
    val feeToken: Token,
    val adapter: ISendMoneroAdapter,
    val coinMaxAllowedDecimals: Int,
    private val xRateService: XRateService,
    private val address: Address,
    private val showAddressInput: Boolean,
    private val amountService: SendAmountService,
    private val addressService: SendMoneroAddressService,
    private val feeService: SendMoneroFeeService,
    private val contactsRepo: ContactsRepository,
    private val recentAddressManager: RecentAddressManager,
    private val balanceAdapter: IBalanceAdapter,
    private val localStorage: ILocalStorage,
    private val accountsAdapter: IMoneroAccountsAdapter,
) : ViewModelUiState<SendMoneroUiState>() {
    val blockchainType = wallet.token.blockchainType
    val feeTokenMaxAllowedDecimals = feeToken.decimals
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var feeState = feeService.stateFlow.value
    private var memo: String? = null

    private var utxoData: UtxoData? = null
    private var utxoExpertModeEnabled = localStorage.utxoExpertModeEnabled
    private var balanceState = balanceAdapter.balanceState

    var customUnspentOutputs: List<MoneroUnspentOutput>? = null
        private set

    var coinRate by mutableStateOf(xRateService.getRate(sendToken.coin.uid))
        private set
    var feeCoinRate by mutableStateOf(xRateService.getRate(feeToken.coin.uid))
        private set
    var sendResult by mutableStateOf<SendResult?>(null)
        private set

    private val logger: AppLogger = AppLogger("send-monero")

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
        viewModelScope.launch(Dispatchers.Default) {
            updateUtxoData()

            emitState()
        }
        viewModelScope.launch(Dispatchers.Default) {
            localStorage.utxoExpertModeEnabledFlow.collect { enabled ->
                utxoExpertModeEnabled = enabled

                emitState()
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            merge(
                balanceAdapter.balanceStateUpdatedFlowable.asFlow(),
                balanceAdapter.balanceUpdatedFlowable.asFlow()
            )
                .catch { logger.warning("balance updates flow failed", it) }
                .collect {
                    // the handler reaches JNI; one native error must not kill
                    // this collector for the rest of the screen's lifetime
                    try {
                        handleBalanceAdapterUpdate()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        logger.warning("handleBalanceAdapterUpdate failed", e)
                    }
                }
        }

        addressService.setAddress(address)
    }

    private suspend fun handleBalanceAdapterUpdate() {
        balanceState = balanceAdapter.balanceState

        // while the wallet scans, outputs appear and disappear; drop selections
        // that no longer reference an existing spendable output
        val outputs = adapter.getUnspentOutputs()
        customUnspentOutputs?.let { selection ->
            val validKeyImages = outputs.map { it.keyImage }.toSet()
            val pruned = selection.filter { it.keyImage in validKeyImages }
            if (pruned.size != selection.size) {
                customUnspentOutputs = pruned.ifEmpty { null }
            }
        }

        amountService.setAvailableBalance(
            customUnspentOutputs?.sumOf { it.amount } ?: accountsAdapter.activeAccountBalanceData.available
        )
        updateUtxoData()

        emitState()
    }


    override fun createState() = SendMoneroUiState(
        availableBalance = amountState.availableBalance,
        amountCaution = amountState.amountCaution,
        addressError = addressState.addressError,
        canBeSend = amountState.canBeSend && addressState.canBeSend && feeState.fee != null && balanceState is AdapterState.Synced,
        showAddressInput = showAddressInput,
        fee = feeState.fee,
        feeInProgress = feeState.inProgress,
        address = address,
        utxoData = if (utxoExpertModeEnabled) utxoData else null,
        syncState = balanceState,
        feeCaution = feeState.error?.let { createCaution(it) },
    )

    fun updateCustomUnspentOutputs(customUnspentOutputs: List<MoneroUnspentOutput>) {
        this.customUnspentOutputs = customUnspentOutputs.ifEmpty { null }

        amountService.setAvailableBalance(
            this.customUnspentOutputs?.sumOf { it.amount } ?: accountsAdapter.activeAccountBalanceData.available
        )

        viewModelScope.launch(Dispatchers.Default) {
            updateUtxoData()

            emitState()
        }
    }

    private suspend fun updateUtxoData() {
        // unlike Bitcoin, wallet2 does not reveal which inputs auto-selection will use
        // before the transaction is created, so Auto mode shows only the total count
        val totalOutputs = adapter.getUnspentOutputs().size
        utxoData = UtxoData(
            type = if (customUnspentOutputs == null) UtxoType.Auto else UtxoType.Manual,
            value = customUnspentOutputs?.let { "${it.size} / $totalOutputs" } ?: "$totalOutputs"
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

    private fun handleUpdatedAddressState(addressState: SendMoneroAddressService.State) {
        this.addressState = addressState
        feeService.setAddress(addressState.address)

        emitState()
    }

    private fun handleUpdatedFeeState(feeState: SendMoneroFeeService.State) {
        this.feeState = feeState

        emitState()
    }

    /**
     * Confirmation data for the current input, or null when it isn't available.
     *
     * The pieces are filled in asynchronously and none of them survive process death, so a
     * confirmation screen restored from the saved back stack sees empty state. Reporting that as
     * null lets the caller send the user back to the form instead of crashing on composition.
     */
    fun getConfirmationData(): SendConfirmationData? {
        val address = addressState.address ?: return null
        val contact = contactsRepo.getContactsFiltered(
            blockchainType,
            addressQuery = address.hex
        ).firstOrNull()
        return SendConfirmationData(
            amount = amountState.amount ?: return null,
            fee = feeState.fee ?: return null,
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

            adapter.send(
                amountState.amount!!,
                addressState.address?.hex!!,
                memo,
                customUnspentOutputs?.map { it.keyImage }
            )

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
}

data class SendMoneroUiState(
    val availableBalance: BigDecimal?,
    val amountCaution: HSCaution?,
    val addressError: Throwable?,
    val canBeSend: Boolean,
    val showAddressInput: Boolean,
    val fee: BigDecimal?,
    val feeInProgress: Boolean,
    val address: Address,
    val utxoData: UtxoData?,
    val syncState: AdapterState,
    val feeCaution: HSCaution?,
)
