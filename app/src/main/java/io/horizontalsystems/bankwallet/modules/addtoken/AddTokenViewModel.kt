package io.horizontalsystems.bankwallet.modules.addtoken

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Caution
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddTokenViewModel(private val addTokenService: AddTokenService) :
    ViewModelUiState<AddTokenUiState>() {

    private var loading = false
    private var finished = false
    private var addButtonEnabled = false
    private var tokenInfo: AddTokenService.TokenInfo? = null
    private var caution: Caution? = null
    private var enteredText = ""

    override fun createState() = AddTokenUiState(
        tokenInfo = tokenInfo,
        addButtonEnabled = addButtonEnabled,
        loading = loading,
        finished = finished,
        caution = caution
    )

    val blockchains by addTokenService::blockchains

    var selectedBlockchain by mutableStateOf(blockchains.first { it.type == BlockchainType.Ethereum })
        private set

    private var fetchCustomCoinsJob: Job? = null

    fun onBlockchainSelect(blockchain: Blockchain) {
        selectedBlockchain = blockchain
        if (enteredText.isNotBlank()) {
            updateTokenInfo(blockchain, enteredText)
        }
    }

    fun onEnterText(text: String) {
        enteredText = text
        updateTokenInfo(selectedBlockchain, text)
    }

    private fun updateTokenInfo(blockchain: Blockchain, text: String) {
        fetchCustomCoinsJob?.cancel()
        tokenInfo = null
        addButtonEnabled = false
        caution = null
        loading = true

        emitState()

        fetchCustomCoinsJob = viewModelScope.launch {
            try {
                tokenInfo = withContext(Dispatchers.IO) {
                    addTokenService.tokenInfo(blockchain, text.trim())
                }
                tokenInfo?.let {
                    if (it.inCoinList) {
                        caution = Caution(
                            Translator.getString(R.string.AddToken_CoinAlreadyInListWarning),
                            Caution.Type.Warning
                        )
                    } else {
                        addButtonEnabled = true
                    }
                }
            } catch (e: Exception) {
                caution = Caution(getErrorText(e), Caution.Type.Error)
            }

            loading = false
            emitState()
        }
    }

    fun onAddClick() {
        viewModelScope.launch {
            tokenInfo?.let {
                addTokenService.addToken(it)
                finished = true
            }

            emitState()
        }
    }

    private fun getErrorText(error: Throwable): String = when (error) {
        is AddTokenService.TokenError.NotFound -> {
            Translator.getString(
                R.string.AddEvmToken_ContractAddressNotFoundInBlockchain,
                selectedBlockchain.name
            )
        }

        is AddTokenService.TokenError.InvalidReference -> {
            Translator.getString(R.string.AddToken_InvalidContractAddress)
        }

        else -> Translator.getString(R.string.Error)
    }
}

data class AddTokenUiState(
    val tokenInfo: AddTokenService.TokenInfo?,
    val addButtonEnabled: Boolean,
    val loading: Boolean,
    val finished: Boolean,
    val caution: Caution?,
)
