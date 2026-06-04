package io.horizontalsystems.bankwallet.modules.opencryptopay

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

@HiltViewModel(assistedFactory = OpenCryptoPayViewModel.Factory::class)
class OpenCryptoPayViewModel @AssistedInject constructor(
    @Assisted private val lnurl: String,
    private val walletManager: io.horizontalsystems.bankwallet.core.managers.WalletManager,
) : ViewModelUiState<OpenCryptoPayUiState>() {

    private var loading = true
    private var merchant: String? = null
    private var currency: String? = null
    private var fiatAmount: String? = null
    private var methods: List<OcpMethodViewItem> = emptyList()
    private var secondsUntilExpiry: Int? = null
    private var error: String? = null
    private var navigateToEvmConfirm: OcpEvmConfirmData? = null
    private var navigateToConfirm: OcpEvmConfirmData? = null

    private var paymentResponse: OcpPaymentResponse? = null
    private var countdownJob: Job? = null
    private var apiUrl: String? = null

    init {
        fetchPaymentDetails()
    }

    override fun createState() = OpenCryptoPayUiState(
        loading = loading,
        merchant = merchant,
        currency = currency,
        fiatAmount = fiatAmount,
        methods = methods,
        secondsUntilExpiry = secondsUntilExpiry,
        error = error,
        navigateToEvmConfirm = navigateToEvmConfirm,
        navigateToConfirm = navigateToConfirm,
    )

    private fun fetchPaymentDetails() {
        loading = true
        error = null
        emitState()
        viewModelScope.launch {
            try {
                val url = resolveApiUrl(lnurl)
                apiUrl = url
                val baseUrl = url.substringBefore("?").let { u ->
                    val lastSlash = u.lastIndexOf('/')
                    if (lastSlash > 8) u.substring(0, lastSlash + 1) else "$u/"
                }
                val response = OcpApiService.service(baseUrl).getPaymentDetails(url, timeout = 0)

                if (response.statusCode == 404 || response.quote == null) {
                    error = response.message ?: Translator.getString(R.string.OpenCryptoPay_Error_NoActivePayment)
                    loading = false
                    emitState()
                    return@launch
                }

                paymentResponse = response
                merchant = response.merchant
                currency = response.requestedAmount?.asset
                fiatAmount = response.requestedAmount?.amount?.let { formatAmount(it) }

                val activeWallets = walletManager.activeWallets

                methods = response.transferAmounts
                    .filter { it.available && it.assets.isNotEmpty() }
                    .flatMap { transfer ->
                        val blockchainType = transfer.supportedBlockchainTypes().firstOrNull()
                            ?: return@flatMap emptyList()
                        transfer.assets.mapNotNull { asset ->
                            val wallet = activeWallets.firstOrNull { w ->
                                w.token.blockchainType == blockchainType &&
                                    w.token.coin.code.equals(asset.asset, ignoreCase = true)
                            }
                            wallet ?: return@mapNotNull null
                            OcpMethodViewItem(
                                transfer = transfer,
                                asset = asset,
                                blockchainType = blockchainType,
                                wallet = wallet,
                            )
                        }
                    }

                loading = false
                startCountdown(response.quote.expiration)
                emitState()
            } catch (e: Exception) {
                error = e.message ?: Translator.getString(R.string.OpenCryptoPay_Error_FetchFailed)
                loading = false
                emitState()
            }
        }
    }

    private fun startCountdown(expirationIso: String) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                val now = Instant.now().epochSecond
                val expiry = try {
                    Instant.parse(expirationIso).epochSecond
                } catch (_: Exception) {
                    break
                }
                val remaining = (expiry - now).toInt()
                if (remaining <= 0) {
                    secondsUntilExpiry = 0
                    emitState()
                    fetchPaymentDetails()
                    break
                }
                secondsUntilExpiry = remaining
                emitState()
                delay(1000)
            }
        }
    }

    fun onPayClick(methodItem: OcpMethodViewItem) {
        val response = paymentResponse ?: return
        val quote = response.quote ?: return

        val data = OcpEvmConfirmData(
            wallet = methodItem.wallet,
            callbackUrl = response.callback,
            quoteId = quote.id,
            paymentId = quote.payment,
            method = methodItem.transfer.method,
            asset = methodItem.asset.asset,
            assetAmount = methodItem.asset.amount,
            blockchainType = methodItem.blockchainType,
            merchant = response.merchant,
            expirationIso = quote.expiration,
            minFee = methodItem.transfer.minFee,
        )
        if (isEvmMethod(methodItem.transfer.method)) {
            navigateToEvmConfirm = data
        } else {
            navigateToConfirm = data
        }
        emitState()
    }

    fun onNavigatedToEvmConfirm() {
        navigateToEvmConfirm = null
        emitState()
    }

    fun onNavigatedToConfirm() {
        navigateToConfirm = null
        emitState()
    }

    fun onErrorShown() {
        error = null
        emitState()
    }

    override fun onCleared() {
        countdownJob?.cancel()
        super.onCleared()
    }

    private fun formatAmount(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) {
            amount.toLong().toString()
        } else {
            "%.2f".format(amount)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(lnurl: String): OpenCryptoPayViewModel
    }
}

// Extracts the /tx/... endpoint URL from the hint text ("...via the endpoint https://...").
// Falls back to constructing from callbackUrl + paymentId.
fun extractProofUrl(hint: String?, callbackUrl: String, paymentId: String): String {
    hint?.let {
        Regex("https?://\\S+").find(it)?.value?.trimEnd('.', ',')?.let { url ->
            return url
        }
    }
    // Fallback: replace /cb/ with /tx/ and swap the last path segment
    val base = callbackUrl.substringBefore("/cb/").trimEnd('/')
    return "$base/tx/$paymentId"
}

// Parses an OCP transaction URI into (recipient address, ERC-20 contract address or null).
// Handles EIP-681 ERC-20: ethereum:<contract>@<chainId>/transfer?address=<recipient>&uint256=<amount>
// and plain transfers:    ethereum:<recipient>@<chainId>?value=<amount>
fun parseOcpUri(uri: String): Pair<String, String?> {
    val afterScheme = uri.substringAfter(":")
    val pathPart = afterScheme.substringBefore("?")
    val query = if (uri.contains("?")) uri.substringAfter("?") else ""

    return if (pathPart.contains("/transfer")) {
        val contract = pathPart.substringBefore("@").substringBefore("/")
        val recipient = query.split("&")
            .firstOrNull { it.startsWith("address=") }
            ?.substringAfter("address=")
            ?: error("EIP-681 transfer URI missing address= parameter")
        recipient to contract
    } else {
        val recipient = pathPart.substringBefore("@")
        recipient to null
    }
}

fun resolveApiUrl(lnurl: String): String {
    val upper = lnurl.uppercase()
    return if (upper.startsWith("LNURL")) {
        LnurlDecoder.decode(lnurl)
    } else {
        // Already a plain URL (from lightning: prefix or ?lightning= param)
        lnurl
    }
}

private val evmMethods = setOf(
    "Ethereum", "BinanceSmartChain", "Polygon", "Arbitrum", "Optimism", "Base"
)

fun isEvmMethod(method: String) = method in evmMethods

data class OpenCryptoPayUiState(
    val loading: Boolean,
    val merchant: String?,
    val currency: String?,
    val fiatAmount: String?,
    val methods: List<OcpMethodViewItem>,
    val secondsUntilExpiry: Int?,
    val error: String?,
    val navigateToEvmConfirm: OcpEvmConfirmData?,
    val navigateToConfirm: OcpEvmConfirmData?,
)

data class OcpMethodViewItem(
    val transfer: OcpTransferAmount,
    val asset: OcpAsset,
    val blockchainType: BlockchainType,
    val wallet: Wallet,
)

data class OcpEvmConfirmData(
    val wallet: Wallet,
    val callbackUrl: String,
    val quoteId: String,
    val paymentId: String,
    val method: String,
    val asset: String,
    val assetAmount: String,
    val blockchainType: BlockchainType,
    val merchant: String?,
    val expirationIso: String,
    val minFee: Double?,
)

suspend fun fetchOcpTransactionDetails(
    callbackUrl: String,
    quoteId: String,
    paymentId: String,
    method: String,
    asset: String,
): Pair<String, String> {
    val baseUrl = callbackUrl.substringBefore("?").let { u ->
        val lastSlash = u.lastIndexOf('/')
        if (lastSlash > 8) u.substring(0, lastSlash + 1) else "$u/"
    }
    val txResponse = OcpApiService.service(baseUrl).getTransactionDetails(
        url = callbackUrl,
        quote = quoteId,
        method = method,
        asset = asset,
    )
    val proofUrl = extractProofUrl(txResponse.hint, callbackUrl, paymentId)
    val uri = txResponse.uri ?: throw Exception("No transaction URI received")
    val (address, _) = parseOcpUri(uri)
    return address to proofUrl
}
