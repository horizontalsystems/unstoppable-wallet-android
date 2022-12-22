package io.horizontalsystems.bankwallet.modules.addtoken

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.protocolType
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.tokenIconPlaceholder
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AddTokenViewModel(private val addTokenService: AddTokenService) : ViewModel() {

    private var loading = false
    private var finished = false
    private var tokens: List<TokenInfoUiState> = listOf()
    private var alreadyAddedTokens: List<AlreadyAddedToken> = listOf()
    private var caution: Caution? = null
    private val actionButton: AddTokenButton
        get() {
            val enabled = tokens.any { it.enabled && it.checked }
            val title =
                Translator.getString(if (enabled) R.string.Button_Add else R.string.AddToken_ChooseToken)
            return AddTokenButton(title, enabled)
        }

    var uiState by mutableStateOf(
        AddTokenUiState(
            tokens = tokens,
            alreadyAddedTokens = alreadyAddedTokens,
            actionButton = actionButton,
            loading = loading,
            finished = finished,
            caution = caution
        )
    )
        private set

    private var fetchCustomCoinsJob: Job? = null

    private fun emitState() {
        uiState = AddTokenUiState(
            tokens = tokens,
            alreadyAddedTokens = alreadyAddedTokens,
            actionButton = actionButton,
            loading = loading,
            finished = finished,
            caution = caution,
        )
    }

    fun onEnterText(text: String) {
        fetchCustomCoinsJob?.cancel()
        fetchCustomCoinsJob = viewModelScope.launch {
            loading = true
            caution = null

            emitState()

            try {
                val tokens = addTokenService.getTokens(text)

                val supportedTokens = tokens.filter { it.supported }
                val notInWalletTokens = supportedTokens.filter { !it.inWallet }

                this@AddTokenViewModel.tokens = notInWalletTokens.map {
                        TokenInfoUiState(
                            tokenInfo = it,
                            title = it.token.coin.code,
                            subtitle = it.token.coin.name,
                            badge = it.token.tokenQuery.protocolType,
                            image = ImageSource.Remote(it.token.coin.imageUrl, it.token.blockchainType.tokenIconPlaceholder),
                            checked = notInWalletTokens.size == 1 && supportedTokens.size == 1,
                            enabled = true
                        )
                    }

                this@AddTokenViewModel.alreadyAddedTokens = supportedTokens
                    .filter { it.inWallet }
                    .map {
                        AlreadyAddedToken(
                            title = it.token.coin.code,
                            subtitle = it.token.coin.name,
                            badge = it.token.tokenQuery.protocolType,
                            image = ImageSource.Remote(it.token.coin.imageUrl, it.token.blockchainType.tokenIconPlaceholder),
                        )
                    }

                val tokenInfo = tokens.firstOrNull()

                if (supportedTokens.isEmpty() && tokenInfo?.supported == false) {
                    caution = Caution(
                        Translator.getString(
                            R.string.ManageCoins_NotSupportedDescription,
                            addTokenService.accountType?.description ?: "",
                            tokenInfo.token.coin.name,
                        ), Caution.Type.Warning
                    )
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

data class AddTokenButton(
    val title: String,
    val enabled: Boolean
)

data class AddTokenUiState(
    val tokens: List<TokenInfoUiState>,
    val alreadyAddedTokens: List<AlreadyAddedToken>,
    val actionButton: AddTokenButton,
    val loading: Boolean,
    val finished: Boolean,
    val caution: Caution?,
)

data class AlreadyAddedToken(
    val title: String,
    val subtitle: String,
    val badge: String?,
    val image: ImageSource,
)

data class TokenInfoUiState(
    val tokenInfo: AddTokenService.TokenInfo,
    val title: String,
    val subtitle: String,
    val badge: String?,
    val image: ImageSource,
    val checked: Boolean,
    val enabled: Boolean
)
