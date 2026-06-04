package io.horizontalsystems.bankwallet.modules.opencryptopay

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.storage.OcpPaymentDao
import io.horizontalsystems.bankwallet.entities.OcpPaymentRecord
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionResult
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceBtc
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceFactory
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.ValueType
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.ViewItem
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.math.BigDecimal
import java.time.Instant

@HiltViewModel(assistedFactory = OpenCryptoPayConfirmationViewModel.Factory::class)
class OpenCryptoPayConfirmationViewModel @AssistedInject constructor(
    @Assisted private val input: OpenCryptoPayConfirmationPage.Input,
    private val ocpPaymentDao: OcpPaymentDao,
    private val application: android.app.Application,
    private val marketKit: io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper,
    private val currencyManager: io.horizontalsystems.bankwallet.core.managers.CurrencyManager,
    private val numberFormatter: io.horizontalsystems.bankwallet.core.IAppNumberFormatter,
) : ViewModelUiState<OpenCryptoPayEvmConfirmationUiState>() {

    val wallet = input.wallet
    private val callbackUrl = input.callbackUrl
    private val quoteId = input.quoteId
    private val paymentId = input.paymentId
    private val method = input.method
    private val asset = input.asset
    private val assetAmount = input.assetAmount
    private val merchant = input.merchant
    private val expirationIso = input.expirationIso
    private val minFee = input.minFee

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
        val baseUrl = proofUrl.substringBefore("/tx/").let { it.trimEnd('/') + "/" }
        if (wallet.token.blockchainType == BlockchainType.Bitcoin) {
            val signed = withContext(Dispatchers.IO) {
                (sendTransactionService as SendTransactionServiceBtc).signTransaction()
            }
            submitProofHexWithRetry(baseUrl, signed.hex)

            ocpPaymentDao.insert(
                OcpPaymentRecord(
                    txHash = signed.transactionHash,
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
        } else {
            val result = withContext(Dispatchers.IO) { sendTransactionService.sendTransaction() }
            val txHash = extractTxHash(result)
                ?: throw Exception("Could not get transaction hash")

            ocpPaymentDao.insert(
                OcpPaymentRecord(
                    paymentId = paymentId,
                    quoteId = quoteId,
                    proofUrl = proofUrl,
                    method = method,
                    txHash = txHash,
                    merchant = merchant,
                    expirationIso = expirationIso,
                    createdAt = System.currentTimeMillis(),
                    proofSubmittedAt = null,
                )
            )
            OcpProofSubmissionWorker.enqueue(application, txHash)
        }
    }

    private suspend fun submitProofHexWithRetry(baseUrl: String, rawHex: String) {
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
                    recommendedGasRate = minFee?.let { kotlin.math.ceil(it).toInt() },
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
        val coinAmount = numberFormatter.formatCoinFull(amount, coin.code, wallet.token.decimals)
        val rate = marketKit.coinPrice(coin.uid, currencyManager.baseCurrency.code)
        val fiatAmount = rate?.let {
            numberFormatter.formatFiatFull(it.value * amount, currencyManager.baseCurrency.symbol)
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

    @AssistedFactory
    interface Factory {
        fun create(input: OpenCryptoPayConfirmationPage.Input): OpenCryptoPayConfirmationViewModel
    }
}
