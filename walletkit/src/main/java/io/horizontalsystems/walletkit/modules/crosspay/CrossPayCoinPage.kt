package io.horizontalsystems.walletkit.modules.crosspay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.badge
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.nav3.LocalResultEventBus
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.BadgeText
import io.horizontalsystems.walletkit.ui.compose.components.CoinImage
import io.horizontalsystems.walletkit.ui.compose.components.FormsInput
import io.horizontalsystems.walletkit.ui.compose.components.HSpacer
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.headline2_leah
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_grey
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Destination-token picker for the CrossPay tab: only tokens the exact-output provider can
 * actually deliver for the given source, so a pick can never dead-end in "not supported".
 * Returns the chosen [Token] as the page result.
 */
@Serializable
data class CrossPayCoinPage(val tokenIn: Token) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val resultEventBus = LocalResultEventBus.current
        val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<CrossPayCoinViewModel>(
            factory = CrossPayCoinViewModel.Factory(tokenIn)
        )

        CrossPayCoinScreen(
            navigation = navigation,
            viewModel = viewModel,
        ) {
            resultEventBus.sendResult(it)
            navigation.removeLastOrNull()
        }
    }
}

@Composable
private fun CrossPayCoinScreen(
    navigation: HSNavigation,
    viewModel: CrossPayCoinViewModel,
    onSelect: (Token) -> Unit,
) {
    HSScaffold(
        title = stringResource(R.string.CrossPay_ChooseCoin),
        onBack = navigation::removeLastOrNull,
        bottomBar = {
            FormsInput(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .imePadding(),
                hint = stringResource(R.string.Market_Search),
                singleLine = true,
                pasteEnabled = false,
            ) {
                viewModel.setQuery(it)
            }
        },
    ) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(viewModel.uiState.tokens) { token ->
                CoinRow(token) { onSelect(token) }
                HsDivider(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CoinRow(token: Token, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinImage(
            token = token,
            modifier = Modifier.size(40.dp)
        )
        HSpacer(16.dp)
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                headline2_leah(text = token.coin.code)
                token.badge?.let {
                    HSpacer(8.dp)
                    BadgeText(
                        text = it,
                        background = ComposeAppTheme.colors.blade,
                        textColor = ComposeAppTheme.colors.leah,
                    )
                }
            }
            VSpacer(3.dp)
            subhead2_grey(text = token.coin.name)
        }
    }
}

class CrossPayCoinViewModel(
    private val tokenIn: Token,
) : androidx.lifecycle.ViewModel() {

    private val provider = CrossPayManager.resolveProvider()

    private var allTokens = listOf<Token>()
    private var query = ""

    var uiState by mutableStateOf(UiState(emptyList()))
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                provider?.start()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // An empty list is the visible symptom; picking simply requires a retry.
            }
            allTokens = provider?.supportedTokensOut(tokenIn)
                .orEmpty()
                .sortedWith(compareBy({ it.coin.marketCapRank ?: Int.MAX_VALUE }, { it.coin.code }))
            emit()
        }
    }

    fun setQuery(query: String) {
        this.query = query
        emit()
    }

    private fun emit() {
        val filtered = if (query.isBlank()) {
            allTokens
        } else {
            allTokens.filter {
                it.coin.code.contains(query, ignoreCase = true) ||
                        it.coin.name.contains(query, ignoreCase = true)
            }
        }
        uiState = UiState(filtered)
    }

    data class UiState(val tokens: List<Token>)

    class Factory(private val tokenIn: Token) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CrossPayCoinViewModel(tokenIn) as T
        }
    }
}
