package io.horizontalsystems.bankwallet.modules.addtoken

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.icon24
import io.horizontalsystems.bankwallet.core.protocolType
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AddTokenViewModel(private val addTokenService: AddTokenService) : ViewModel() {

    private var loading = false
    private var finished = false
    private var coinName: String? = null
    private var coinCode: String? = null
    private var decimals: Int? = null
    private var tokens: List<TokenInfoUiState> = listOf()
    private var caution: Caution? = null

    var uiState by mutableStateOf(
        AddTokenUiState(
            coinName = coinName,
            coinCode = coinCode,
            decimals = decimals,
            tokens = tokens,
            addEnabled = false,
            loading = loading,
            finished = finished,
            caution = caution
        )
    )
        private set

    private var fetchCustomCoinsJob: Job? = null

    private fun emitState() {
        uiState = AddTokenUiState(
            coinName = coinName,
            coinCode = coinCode,
            decimals = decimals,
            tokens = tokens,
            addEnabled = tokens.any { it.enabled && it.checked },
            loading = loading,
            finished = finished,
            caution = caution,
        )
    }

    fun onEnterText(text: String) {
        fetchCustomCoinsJob?.cancel()
        fetchCustomCoinsJob = viewModelScope.launch {
            loading = true
            coinName = null
            coinCode = null
            decimals = null
            caution = null

            emitState()

            try {
                val tokens = addTokenService.getTokens(text)

                this@AddTokenViewModel.tokens = tokens.map {
                    TokenInfoUiState(
                        tokenInfo = it,
                        title = it.tokenQuery.protocolType ?: "",
                        image = it.tokenQuery.blockchainType.icon24,
                        checked = it.inWallet,
                        enabled = !it.inWallet
                    )
                }

                val tokenInfo = tokens.firstOrNull()
                coinName = tokenInfo?.coinName
                coinCode = tokenInfo?.coinCode
                decimals = tokenInfo?.decimals
            } catch (e: Exception) {
                caution = Caution(getErrorText(e), Caution.Type.Error)
            }

            loading = false
            emitState()
        }
    }

    fun onAddClick() {
        viewModelScope.launch {
            val tokens = tokens.filter { it.enabled && it.checked }.map { it.tokenInfo }
            addTokenService.addTokens(tokens)

            finished = true
            emitState()
        }
    }

    fun onToggleToken(tokenInfoUiState: TokenInfoUiState) {
        val tmpList = tokens.toMutableList()
        val indexOf = tmpList.indexOf(tokenInfoUiState)
        if (indexOf != -1) {
            val token = tmpList[indexOf]
            tmpList[indexOf] = token.copy(checked = !token.checked)

            tokens = tmpList

            emitState()
        }
    }

    private fun getErrorText(error: Throwable): String {
        val errorKey = when (error) {
            is AddTokenService.TokenError.NotFound -> R.string.AddEvmToken_TokenNotFound
            is AddTokenService.TokenError.InvalidReference -> R.string.AddToken_InvalidAddressError
            else -> R.string.Error
        }

        return Translator.getString(errorKey)
    }
}

data class AddTokenUiState(
    val coinName: String?,
    val coinCode: String?,
    val decimals: Int?,
    val tokens: List<TokenInfoUiState>,
    val addEnabled: Boolean,
    val loading: Boolean,
    val finished: Boolean,
    val caution: Caution?,
)

data class TokenInfoUiState(
    val tokenInfo: TokenInfo,
    val title: String,
    val image: Int,
    val checked: Boolean,
    val enabled: Boolean
)
