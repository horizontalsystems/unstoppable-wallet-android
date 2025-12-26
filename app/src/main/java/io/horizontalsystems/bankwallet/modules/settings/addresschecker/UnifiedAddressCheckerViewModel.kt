package io.horizontalsystems.bankwallet.modules.settings.addresschecker

import HashDitAddressValidator
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.address.ChainalysisAddressValidator
import io.horizontalsystems.bankwallet.core.address.Eip20AddressValidator
import io.horizontalsystems.bankwallet.core.address.Trc20AddressValidator
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerFactory
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class UnifiedAddressCheckerViewModel(
    private val marketKitWrapper: MarketKitWrapper,
    addressHandlerFactory: AddressHandlerFactory,
    private val hashDitValidator: HashDitAddressValidator,
    private val chainalysisValidator: ChainalysisAddressValidator,
    private val eip20Validator: Eip20AddressValidator,
    private val trc20Validator: Trc20AddressValidator,
) : ViewModelUiState<AddressCheckState>() {
    private var addressValidationInProgress: Boolean = false

    private var checkResults: Map<IssueType, CheckState> = emptyMap()
    private var value = ""
    private var inputState: DataState<Address>? = null
    private var parseAddressJob: Job? = null

    private val parserChain = addressHandlerFactory.parserChain(null, true)
    private val coinUids = listOf("tether", "usd-coin", "paypal-usd")

    var hashDitBlockchains: List<Blockchain> = emptyList()
        private set
    var contractFullCoins: List<FullCoin> = emptyList()
    private var issueTypes: List<IssueType> = emptyList()

    init {
        viewModelScope.launch {
            hashDitBlockchains = try {
                val blockchains =
                    marketKitWrapper.blockchains(hashDitValidator.supportedBlockchainTypes.map { it.uid })
                hashDitValidator.supportedBlockchainTypes.mapNotNull { type ->
                    blockchains.firstOrNull { it.type == type }
                }
            } catch (e: Exception) {
                emptyList()
            }

            // Initialize contractFullCoins
            contractFullCoins = try {
                val fullCoins = marketKitWrapper.fullCoins(coinUids)
                coinUids.mapNotNull { uid ->
                    val fullCoin = fullCoins.firstOrNull { it.coin.uid == uid }
                    fullCoin?.let {
                        val filteredTokens = it.tokens.filter { token ->
                            eip20Validator.supports(token) || trc20Validator.supports(token)
                        }
                        FullCoin(coin = it.coin, tokens = filteredTokens)
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }

            issueTypes = buildList {
                add(IssueType.Chainalysis)
                addAll(hashDitBlockchains.map { IssueType.HashDit(it.type) })
                addAll(contractFullCoins.flatMap { it.tokens }.map { IssueType.Contract(it) })
            }

            checkResults = issueTypes.associateWith { CheckState.Idle }
        }
    }

    override fun createState() = AddressCheckState(
        value = value,
        inputState = inputState,
        addressValidationInProgress = addressValidationInProgress,
        checkResults = checkResults,
    )

    fun onEnterAddress(value: String) {
        parseAddressJob?.cancel()

        this.value = value.trim()
        inputState = null
        addressValidationInProgress = true

        if (value.isBlank()) {
            this.value = ""
            resetCheckStatus()
        } else {
            checkResults = issueTypes.associateWith { CheckState.Checking }
            emitState()
            viewModelScope.launch {
                processAddress(value)
            }
        }
    }

    private suspend fun processAddress(address: String) {
        try {
            val handlers = parserChain.supportedAddressHandlers(address)

            if (handlers.isEmpty()) {
                val error = Exception()
                inputState = DataState.Error(error)
                addressValidationInProgress = false
                emitState()
                return
            }

            val parsedAddress = handlers.first().parseAddress(address)
            check(parsedAddress)
        } catch (e: Throwable) {
            inputState = DataState.Error(e)
            addressValidationInProgress = false
            emitState()
        }
    }

    private fun resetCheckStatus() {
        checkResults = issueTypes.associateWith { CheckState.Idle }
        emitState()
    }

    private fun check(address: Address) {
        parseAddressJob = viewModelScope.launch(Dispatchers.IO) {
            val results = mutableMapOf<IssueType, CheckState>()
            val checkJobs = issueTypes.map { type ->
                async {
                    type to performSingleCheck(address, type)
                }
            }

            try {
                checkJobs.forEach { deferred ->
                    val (type, result) = deferred.await()
                    results[type] = result

                    // Update UI incrementally
                    checkResults = checkResults.toMutableMap().apply {
                        this[type] = result
                    }
                    emitState()
                }
            } catch (e: CancellationException) {
                resetCheckStatus()
                throw e
            } catch (e: Exception) {
                throw e
            } finally {
                addressValidationInProgress = false
                emitState()
            }
        }
    }

    private suspend fun performSingleCheck(address: Address, type: IssueType): CheckState {
        val canCheck = when (type) {
            is IssueType.Chainalysis -> true
            is IssueType.Contract -> {
                if (trc20Validator.supports(type.token) || eip20Validator.supports(type.token)) {
                    true
                } else {
                    false
                }
            }

            is IssueType.HashDit -> {
                address.blockchainType?.let { addressBlockchainType ->
                    EvmBlockchainManager.blockchainTypes.contains(addressBlockchainType)
                } ?: false
            }
        }

        if (!canCheck) {
            return CheckState.NotAvailable
        }

        return try {
            val isClear = when (type) {
                is IssueType.Chainalysis -> {
                    chainalysisValidator.isClear(address)
                }

                is IssueType.HashDit -> {
                    hashDitValidator.isClear(address, type.blockchainType)
                }

                is IssueType.Contract -> {
                    if (type.token.blockchainType == BlockchainType.Tron) {
                        trc20Validator.isClear(address, type.token)
                    } else {
                        eip20Validator.isClear(address, type.token)
                    }
                }
            }

            if (isClear) CheckState.Clear else CheckState.Detected

        } catch (e: Exception) {
            Log.e("TAG", "Single check error for $type: ", e)
            CheckState.NotAvailable
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val addressHandlerFactory = AddressHandlerFactory(App.appConfigProvider.udnApiKey)
            val appConfigProvider = App.appConfigProvider
            return UnifiedAddressCheckerViewModel(
                App.marketKit,
                addressHandlerFactory,
                HashDitAddressValidator(
                    appConfigProvider.hashDitBaseUrl,
                    appConfigProvider.hashDitApiKey,
                    App.evmBlockchainManager
                ),
                ChainalysisAddressValidator(
                    appConfigProvider.chainalysisBaseUrl,
                    appConfigProvider.chainalysisApiKey
                ),
                Eip20AddressValidator(App.evmSyncSourceManager),
                Trc20AddressValidator(),
            ) as T
        }
    }
}

data class AddressCheckState(
    val value: String,
    val inputState: DataState<Address>?,
    val addressValidationInProgress: Boolean,
    val checkResults: Map<IssueType, CheckState>,
)

sealed class IssueType {
    object Chainalysis : IssueType()
    data class HashDit(val blockchainType: BlockchainType) : IssueType()
    data class Contract(val token: Token) : IssueType()

    override fun equals(other: Any?): Boolean {
        return when {
            this is Chainalysis && other is Chainalysis -> true
            this is HashDit && other is HashDit -> this.blockchainType == other.blockchainType
            this is Contract && other is Contract -> this.token == other.token
            else -> false
        }
    }

    override fun hashCode(): Int {
        return when (this) {
            is Chainalysis -> "chainalysis".hashCode()
            is HashDit -> blockchainType.hashCode()
            is Contract -> token.hashCode()
        }
    }
}

enum class CheckState {
    Idle,
    Checking,
    Clear,
    Detected,
    NotAvailable;

    val title: Int
        get() = when (this) {
            Clear -> R.string.Send_Address_Error_Clear
            Detected -> R.string.Send_Address_Error_Detected
            NotAvailable -> R.string.NotAvailable
            else -> R.string.Idle
        }
}