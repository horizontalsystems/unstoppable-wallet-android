package io.horizontalsystems.bankwallet.modules.opencryptopay

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.storage.OcpPaymentDao
import io.horizontalsystems.bankwallet.entities.OcpPaymentRecord
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceEvm
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataField
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewItemFactory
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.GasPrice
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.math.BigDecimal
import java.time.Instant

@HiltViewModel(assistedFactory = OpenCryptoPayEvmConfirmationViewModel.Factory::class)
class OpenCryptoPayEvmConfirmationViewModel @AssistedInject constructor(
    @Assisted private val input: OpenCryptoPayEvmConfirmationPage.Input,
    private val ocpPaymentDao: OcpPaymentDao,
) : ViewModelUiState<OpenCryptoPayEvmConfirmationUiState>() {

    private val wallet = input.wallet
    private val callbackUrl = input.callbackUrl
    private val quoteId = input.quoteId
    private val paymentId = input.paymentId
    private val method = input.method
    private val asset = input.asset
    private val assetAmount = input.assetAmount
    val blockchainType = input.blockchainType
    private val merchant = input.merchant
    private val expirationIso = input.expirationIso

    val sendTransactionService = SendTransactionServiceEvm(
        blockchainType,
        minGasPrice = input.minFee?.let { fee ->
            val feeLong = kotlin.math.ceil(fee).toLong()
            if (App.evmBlockchainManager.getChain(blockchainType).isEIP1559Supported) {
                GasPrice.Eip1559(maxFeePerGas = feeLong, maxPriorityFeePerGas = 0)
            } else {
                GasPrice.Legacy(feeLong)
            }
        }
    )

    private val sendEvmTransactionViewItemFactory = SendEvmTransactionViewItemFactory(
        App.evmLabelManager,
        EvmCoinServiceFactory(
            App.evmBlockchainManager.getBaseToken(blockchainType)!!,
            App.marketKit,
            App.currencyManager,
            App.coinManager,
        ),
        App.contactsRepository,
        blockchainType,
    )

    private var initialLoading = true
    private var apiLoading = true
    private var fetchError: CautionViewItem? = null
    private var sendTransactionState = sendTransactionService.stateFlow.value
    private var secondsUntilExpiry: Int? = null
    private var countdownJob: Job? = null
    private var sectionViewItems: List<SectionViewItem> = emptyList()
    private var proofUrl: String = ""

    init {
        viewModelScope.launch {
            sendTransactionService.stateFlow.collect { state ->
                sendTransactionState = state
                updateInitialLoading()
                emitState()
            }
        }
        sendTransactionService.start(viewModelScope)
        viewModelScope.launch {
            try {
                val (address, resolvedProofUrl) = fetchOcpTransactionDetails(callbackUrl, quoteId, paymentId, method, asset)
                proofUrl = resolvedProofUrl
                val amount = assetAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val adapter = App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter
                    ?: throw Exception("Ethereum adapter not found for ${wallet.token.coin.code}")
                val transactionData = adapter.getTransactionData(amount, Address(address))
                val decoration = sendTransactionService.decorate(transactionData)
                sectionViewItems = sendEvmTransactionViewItemFactory.getItems(transactionData, null, decoration)
                sendTransactionService.setSendTransactionData(SendTransactionData.Evm(transactionData, null))
            } catch (e: Exception) {
                fetchError = CautionViewItem(
                    title = Translator.getString(R.string.Error),
                    text = e.message ?: Translator.getString(R.string.OpenCryptoPay_Error_LoadFailed),
                    type = CautionViewItem.Type.Error,
                )
            } finally {
                apiLoading = false
                updateInitialLoading()
                emitState()
            }
        }
        startCountdown()
    }

    private fun updateInitialLoading() {
        if (!apiLoading && !sendTransactionState.loading) {
            initialLoading = false
        }
    }

    override fun createState() = OpenCryptoPayEvmConfirmationUiState(
        networkFee = sendTransactionState.networkFee,
        cautions = fetchError?.let { listOf(it) } ?: sendTransactionState.cautions,
        payEnabled = fetchError == null && sendTransactionState.sendable,
        transactionFields = sendTransactionState.fields,
        sectionViewItems = sectionViewItems,
        initialLoading = initialLoading,
        merchant = merchant,
        url = callbackUrl,
        secondsUntilExpiry = secondsUntilExpiry,
    )

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                val now = Instant.now().epochSecond
                val expiry = try {
                    Instant.parse(expirationIso).epochSecond
                } catch (_: Exception) {
                    break
                }
                secondsUntilExpiry = maxOf(0, (expiry - now).toInt())
                emitState()
                if (secondsUntilExpiry == 0) break
                delay(1000)
            }
        }
    }

    override fun onCleared() {
        countdownJob?.cancel()
        super.onCleared()
    }

    suspend fun pay() {
        val signed = sendTransactionService.signTransaction()
        val baseUrl = proofUrl.substringBefore("/tx/").let { it.trimEnd('/') + "/" }
        submitProofWithRetry(baseUrl, signed.hex)

        ocpPaymentDao.insert(
            OcpPaymentRecord(
                txHash = signed.txHash,
                paymentId = paymentId,
                quoteId = quoteId,
                proofUrl = proofUrl,
                method = method,
                merchant = merchant,
                expirationIso = expirationIso,
                createdAt = System.currentTimeMillis(),
                proofSubmittedAt = System.currentTimeMillis(),
            )
        )
    }

    private suspend fun submitProofWithRetry(baseUrl: String, rawHex: String) {
        var lastError: Exception = Exception(Translator.getString(R.string.OpenCryptoPay_Error_SubmitFailed))
        repeat(3) { attempt ->
            try {
                OcpProofService.service(baseUrl).submitProofHex(
                    url = proofUrl,
                    quote = quoteId,
                    method = method,
                    hex = rawHex,
                )
                return
            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                lastError = Exception("HTTP ${e.code()}: $body")
                if (attempt < 2) delay(2000L)
            } catch (e: Exception) {
                lastError = e
                if (attempt < 2) delay(2000L)
            }
        }
        throw lastError
    }

    @AssistedFactory
    interface Factory {
        fun create(input: OpenCryptoPayEvmConfirmationPage.Input): OpenCryptoPayEvmConfirmationViewModel
    }
}

data class OpenCryptoPayEvmConfirmationUiState(
    val networkFee: SendModule.AmountData?,
    val cautions: List<CautionViewItem>,
    val payEnabled: Boolean,
    val transactionFields: List<DataField>,
    val sectionViewItems: List<SectionViewItem>,
    val initialLoading: Boolean,
    val merchant: String?,
    val url: String,
    val secondsUntilExpiry: Int?,
)
