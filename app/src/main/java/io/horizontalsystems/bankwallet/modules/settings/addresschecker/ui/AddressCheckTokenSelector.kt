package io.horizontalsystems.bankwallet.modules.settings.addresschecker.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SearchBar
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AddressCheckTokenSelectorScreen(
    selectedBlockchain: Blockchain?,
    onSelect: (Token) -> Unit,
    onBackPress: () -> Unit,
) {
    val viewModel =
        viewModel<AddressCheckTokenSelectorViewModel>(
            factory = AddressCheckTokenSelectorModule.Factory(
                selectedBlockchain
            )
        )
    val uiState = viewModel.uiState

    Scaffold(
        topBar = {
            SearchBar(
                title = stringResource(R.string.SettingsAddressChecker_SelectCoin),
                menuItems = listOf(),
                onClose = onBackPress,
                onSearchTextChanged = { text ->
                    viewModel.updateFilter(text)
                }
            )
        },
        backgroundColor = ComposeAppTheme.colors.tyler
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))
            uiState.tokens.forEachIndexed { _, item ->
                TokenCell(
                    token = item,
                    onItemClick = {
                        onSelect.invoke(item)
                    },
                )
            }
            if (uiState.tokens.isNotEmpty()) {
                HsDivider()
            }
            VSpacer(32.dp)
        }
    }
}

@Composable
private fun TokenCell(
    token: Token,
    onItemClick: (Token) -> Unit,
) {
    val imageSource =
        ImageSource.Remote(token.blockchain.type.imageUrl, R.drawable.ic_platform_placeholder_32)
    Column {
        HsDivider()
        RowUniversal(
            onClick = { onItemClick.invoke(token) },
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalPadding = 0.dp
        ) {
            Image(
                painter = imageSource.painter(),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 16.dp, top = 12.dp, bottom = 12.dp)
                    .size(32.dp)
                    .clip(CircleShape)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    body_leah(
                        text = token.coin.code,
                        maxLines = 1,
                    )
                    token.badge?.let { labelText ->
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(ComposeAppTheme.colors.blade)
                        ) {
                            Text(
                                modifier = Modifier.padding(
                                    start = 4.dp,
                                    end = 4.dp,
                                    bottom = 1.dp
                                ),
                                text = labelText,
                                color = ComposeAppTheme.colors.leah,
                                style = ComposeAppTheme.typography.microSB,
                                maxLines = 1,
                            )
                        }
                    }
                }
                subhead2_grey(
                    text = token.coin.name,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
    }
}

class AddressCheckTokenSelectorViewModel(
    walletManager: IWalletManager,
    blockchain: Blockchain?,
) : ViewModelUiState<AddressCheckTokenSelectorUiState>() {

    private var tokens = emptyList<Token>()
    private var filtered = emptyList<Token>()

    init {
        viewModelScope.launch {
            tokens = walletManager.activeWallets
                .map { it.token }
                .filter { it.blockchain == blockchain }
                .distinctBy { it.type }
            filtered = tokens
            emitState()
        }
    }

    override fun createState() = AddressCheckTokenSelectorUiState(
        tokens = filtered
    )

    fun updateFilter(text: String) {
        filtered = if (text.isBlank()) {
            tokens
        } else {
            tokens.filter {
                it.coin.name.contains(text, ignoreCase = true)
                        || it.coin.code.contains(text, ignoreCase = true)
                        || it.blockchain.name.contains(text, ignoreCase = true)
            }
        }

        emitState()
    }

}

object AddressCheckTokenSelectorModule {
    class Factory(private val blockchain: Blockchain?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddressCheckTokenSelectorViewModel(App.walletManager, blockchain) as T
        }
    }
}

data class AddressCheckTokenSelectorUiState(
    val tokens: List<Token>
)