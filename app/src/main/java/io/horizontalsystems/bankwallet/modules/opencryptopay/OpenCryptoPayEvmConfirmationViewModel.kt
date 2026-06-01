package io.horizontalsystems.bankwallet.modules.opencryptopay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.core.storage.OcpPaymentDao
import io.horizontalsystems.bankwallet.entities.OcpPaymentRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceEvm
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataField
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewItemFactory
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import timber.log.Timber
import java.math.BigDecimal
import java.time.Instant

class OpenCryptoPayEvmConfirmationViewModel(
    private val sendEvmTransactionViewItemFactory: SendEvmTransactionViewItemFactory,
    val sendTransactionService: SendTransactionServiceEvm,
    private val ocpPaymentDao: OcpPaymentDao,
    private val wallet: Wallet,
    private val callbackUrl: String,
    private val quoteId: String,
    private val paymentId: String,
    private val method: String,
    private val asset: String,
    private val assetAmount: String,
    val blockchainType: BlockchainType,
    private val merchant: String?,
    private val expirationIso: String,
) : ViewModelUiState<OpenCryptoPayEvmConfirmationUiState>() {

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
                    title = "Error",
                    text = e.message ?: "Failed to load payment details",
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
        repeat(3) { attempt ->
            Timber.d(
                "OCP EVM GET /tx/ url=$proofUrl quote=$quoteId method=$method" +
                        " hexPrefix=${rawHex.take(20)} attempt=${attempt + 1}/3"
            )
            try {
                OcpProofService.service(baseUrl).submitProofHex(
                    url = proofUrl,
                    quote = quoteId,
                    method = method,
                    hex = rawHex,
                )
                Timber.d("OCP EVM /tx/ success attempt=${attempt + 1}")
                return
            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                Timber.e("OCP EVM proof failed: HTTP ${e.code()} body=$body")
                throw Exception("HTTP ${e.code()}: $body")
            } catch (e: Exception) {
                Timber.d("OCP EVM /tx/ transient failure attempt=${attempt + 1} (${e.javaClass.simpleName}: ${e.message})")
                if (attempt < 2) delay(2000L)
            }
        }
        Timber.e("OCP EVM proof failed after retries")
        throw Exception("Could not submit payment to merchant. Please try again.")
    }

    class Factory(
        private val wallet: Wallet,
        private val callbackUrl: String,
        private val quoteId: String,
        private val paymentId: String,
        private val method: String,
        private val asset: String,
        private val assetAmount: String,
        private val blockchainType: BlockchainType,
        private val merchant: String?,
        private val expirationIso: String,
        private val minFee: Double?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val minGasPrice: GasPrice? = minFee?.let { fee ->
                val feeLong = kotlin.math.ceil(fee).toLong()
                if (App.evmBlockchainManager.getChain(blockchainType).isEIP1559Supported) {
                    GasPrice.Eip1559(maxFeePerGas = feeLong, maxPriorityFeePerGas = 0)
                } else {
                    GasPrice.Legacy(feeLong)
                }
            }
            val sendTransactionService = SendTransactionServiceEvm(blockchainType, minGasPrice = minGasPrice)
            val feeToken = App.evmBlockchainManager.getBaseToken(blockchainType)!!
            val coinServiceFactory = EvmCoinServiceFactory(
                feeToken, App.marketKit, App.currencyManager, App.coinManager
            )
            val sendEvmTransactionViewItemFactory = SendEvmTransactionViewItemFactory(
                App.evmLabelManager, coinServiceFactory, App.contactsRepository, blockchainType
            )
            return OpenCryptoPayEvmConfirmationViewModel(
                sendEvmTransactionViewItemFactory,
                sendTransactionService,
                App.appDatabase.ocpPaymentDao(),
                wallet,
                callbackUrl,
                quoteId,
                paymentId,
                method,
                asset,
                assetAmount,
                blockchainType,
                merchant,
                expirationIso,
            ) as T
        }
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
