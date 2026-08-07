package io.horizontalsystems.walletkit.modules.opencryptopay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.core.managers.EvmKitManagerRegistry
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ISendEthereumAdapter
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.core.storage.OcpPaymentDao
import io.horizontalsystems.walletkit.entities.OcpPaymentRecord
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceEvm
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataField
import io.horizontalsystems.walletkit.modules.send.SendModule
import io.horizontalsystems.walletkit.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.walletkit.modules.sendevmtransaction.SendEvmTransactionViewItemFactory
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
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
                if (EvmKitManagerRegistry.getChain(blockchainType).isEIP1559Supported) {
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
