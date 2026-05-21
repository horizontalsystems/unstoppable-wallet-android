package io.horizontalsystems.bankwallet.modules.opencryptopay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionResult
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceBtc
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceFactory
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.ValueType
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.ViewItem
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import timber.log.Timber
import java.math.BigDecimal
import java.time.Instant

class OpenCryptoPayConfirmationViewModel(
    val wallet: Wallet,
    private val callbackUrl: String,
    private val quoteId: String,
    private val paymentId: String,
    private val method: String,
    private val asset: String,
    private val assetAmount: String,
    private val merchant: String?,
    private val expirationIso: String,
) : ViewModelUiState<OpenCryptoPayEvmConfirmationUiState>() {

    val sendTransactionService = SendTransactionServiceFactory.create(wallet.token)

    private var initialLoading = true
    private var apiLoading = true
    private var fetchError: CautionViewItem? = null
    private var sendTransactionState = sendTransactionService.stateFlow.value
    private var secondsUntilExpiry: Int? = null
    private var countdownJob: Job? = null
    private var address: String = ""
    private var amount: BigDecimal = BigDecimal.ZERO
    private var proofUrl: String = ""
    private var sectionViewItems: List<SectionViewItem> = emptyList()

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
                val (resolvedAddress, resolvedProofUrl) = fetchOcpTransactionDetails(callbackUrl, quoteId, paymentId, method, asset)
                address = resolvedAddress
                proofUrl = resolvedProofUrl
                amount = assetAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                sectionViewItems = buildSectionViewItems()
                sendTransactionService.setSendTransactionData(buildSendData())
            } catch (e: Exception) {
                fetchError = CautionViewItem(
                    title = Translator.getString(R.string.Error),
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
                } catch (_: Exception) { break }
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
        val baseUrl = proofUrl.substringBefore("/tx/").let { it.trimEnd('/') + "/" }
        if (wallet.token.blockchainType == BlockchainType.Bitcoin) {
            val rawHex = withContext(Dispatchers.IO) {
                (sendTransactionService as SendTransactionServiceBtc).signTransaction()
            }
            Timber.d("OCP BTC sign: quote=$quoteId method=$method hexPrefix=${rawHex.take(20)}")
            submitProofHexWithRetry(baseUrl, rawHex)
        } else {
            Timber.d("OCP pay: wallet=${wallet.token.coin.code} address=$address amount=$amount")
            val result = withContext(Dispatchers.IO) { sendTransactionService.sendTransaction() }
            val txHash = extractTxHash(result)
                ?: throw Exception("Could not get transaction hash")
            Timber.d("OCP tx sent: hash=$txHash")
            ocpProofScope.launch {
                submitProofWithRetry(baseUrl, txHash)
            }
        }
    }

    private suspend fun submitProofHexWithRetry(baseUrl: String, rawHex: String) {
        repeat(3) { attempt ->
            try {
                OcpProofService.service(baseUrl).submitProofHex(
                    url = proofUrl,
                    quote = quoteId,
                    method = method,
                    hex = rawHex,
                )
                Timber.i("OCP BTC proof submitted: hexPrefix=${rawHex.take(20)}")
                return
            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                Timber.e("OCP BTC proof HTTP ${e.code()}: $body")
                throw Exception("HTTP ${e.code()}: $body")
            } catch (e: Exception) {
                if (attempt < 2) {
                    Timber.w("OCP BTC proof attempt ${attempt + 1}/3 failed (${e.javaClass.simpleName}: ${e.message}), retrying in 2000ms")
                    delay(2000L)
                } else {
                    Timber.w("OCP BTC proof attempt ${attempt + 1}/3 failed (${e.javaClass.simpleName}: ${e.message})")
                }
            }
        }
        Timber.e("OCP BTC proof failed after retries")
        throw Exception("Could not submit payment to merchant. Please try again.")
    }

    private suspend fun submitProofWithRetry(baseUrl: String, txHash: String) {
        val delays = listOf(2_000L, 5_000L, 10_000L, 30_000L, 60_000L)
        for ((attempt, delayMs) in delays.withIndex()) {
            try {
                Timber.d("OCP submitting proof: tx=$txHash")
                OcpProofService.service(baseUrl).submitProofTx(
                    url = proofUrl,
                    quote = quoteId,
                    method = method,
                    tx = txHash,
                )
                Timber.i("OCP proof submitted: tx=$txHash attempt=${attempt + 1}")
                return
            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                Timber.e("OCP proof HTTP ${e.code()}: $body tx=$txHash")
                return
            } catch (e: Exception) {
                Timber.w("OCP proof attempt ${attempt + 1}/${delays.size} failed (${e.javaClass.simpleName}: ${e.message}) tx=$txHash, retrying in ${delayMs}ms")
                delay(delayMs)
            }
        }
        Timber.e("OCP proof gave up after ${delays.size} attempts, tx=$txHash")
    }

    private fun extractTxHash(result: SendTransactionResult): String? = when (result) {
        is SendTransactionResult.Btc -> result.transactionRecord?.transactionHash
        is SendTransactionResult.Tron -> result.txHash
        is SendTransactionResult.Solana -> result.txHash
        is SendTransactionResult.Monero -> result.txHash
        is SendTransactionResult.Zano -> result.txHash
        is SendTransactionResult.Zcash -> result.transactionHash
        else -> null
    }

    private fun buildSendData(): SendTransactionData {
        return when (wallet.token.blockchainType) {
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.ECash,
            BlockchainType.Litecoin,
            BlockchainType.Dash ->
                SendTransactionData.Btc(
                    address = address,
                    memo = null,
                    amount = amount,
                    recommendedGasRate = null,
                    minimumSendAmount = null,
                    changeToFirstInput = false,
                    utxoFilters = UtxoFilters(),
                )
            BlockchainType.Solana -> SendTransactionData.Solana.Simple(address, amount)
            BlockchainType.Tron -> SendTransactionData.Tron.Simple(address, amount)
            BlockchainType.Monero -> SendTransactionData.Monero(address, amount, null)
            BlockchainType.Zano -> SendTransactionData.Zano(address, amount, null)
            else -> throw IllegalStateException("Unsupported chain: ${wallet.token.blockchainType}")
        }
    }

    private fun buildSectionViewItems(): List<SectionViewItem> {
        val coin = wallet.token.coin
        val coinAmount = App.numberFormatter.formatCoinFull(amount, coin.code, wallet.token.decimals)
        val rate = App.marketKit.coinPrice(coin.uid, App.currencyManager.baseCurrency.code)
        val fiatAmount = rate?.let {
            App.numberFormatter.formatFiatFull(it.value * amount, App.currencyManager.baseCurrency.symbol)
        }
        return listOf(
            SectionViewItem(
                viewItems = listOf(
                    ViewItem.Amount(
                        fiatAmount = fiatAmount,
                        coinAmount = coinAmount,
                        type = ValueType.Outgoing,
                        token = wallet.token,
                    ),
                    ViewItem.Address(
                        title = Translator.getString(R.string.Send_Confirmation_To),
                        address = address,
                    )
                )
            )
        )
    }

    class Factory(
        private val wallet: Wallet,
        private val callbackUrl: String,
        private val quoteId: String,
        private val paymentId: String,
        private val method: String,
        private val asset: String,
        private val assetAmount: String,
        private val merchant: String?,
        private val expirationIso: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OpenCryptoPayConfirmationViewModel(
                wallet, callbackUrl, quoteId, paymentId, method, asset, assetAmount, merchant, expirationIso
            ) as T
        }
    }
}

private val ocpProofScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
