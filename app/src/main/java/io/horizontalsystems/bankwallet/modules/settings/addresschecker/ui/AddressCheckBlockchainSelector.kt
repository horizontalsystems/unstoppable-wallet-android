package io.horizontalsystems.bankwallet.modules.settings.addresschecker.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.SearchBar
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AddressCheckBlockchainSelectorScreen(
    onSelect: (Blockchain) -> Unit,
    onClose: () -> Unit,
) {
    val viewModel =
        viewModel<AddressCheckBlockchainSelectorViewModel>(factory = AddressCheckBlockchainSelectorModule.Factory())
    val uiState = viewModel.uiState

    Scaffold(
        topBar = {
            SearchBar(
                title = stringResource(R.string.SettingsAddressChecker_SelectBlockchain),
                menuItems = listOf(),
                onClose = onClose,
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
            uiState.blockchains.forEachIndexed { _, item ->
                BlockchainCell(
                    blockchain = item,
                    checked = false,
                ) {
                    onSelect.invoke(item)
                }
            }
            if (uiState.blockchains.isNotEmpty()) {
                HsDivider()
            }
            VSpacer(32.dp)
        }
    }
}

@Composable
private fun BlockchainCell(
    blockchain: Blockchain,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    CellUniversal(
        borderTop = true,
        onClick = onToggle
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = blockchain.type.imageUrl,
                error = painterResource(R.drawable.ic_platform_placeholder_32)
            ),
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
        body_leah(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .weight(1f),
            text = blockchain.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (checked) {
            Icon(
                painter = painterResource(R.drawable.ic_checkmark_20),
                tint = ComposeAppTheme.colors.jacob,
                contentDescription = null,
            )
        }
    }
}

class AddressCheckBlockchainSelectorViewModel(
    walletManager: IWalletManager,
) : ViewModelUiState<AddressCheckBlockchainSelectorUiState>() {

    private var blockchains = emptyList<Blockchain>()
    private var filteredBlockchains = emptyList<Blockchain>()

    init {
        viewModelScope.launch {
            blockchains = walletManager.activeWallets
                .map { it.token.blockchain }
                .distinctBy { it.type }
            filteredBlockchains = blockchains
            emitState()
        }
    }

    override fun createState() = AddressCheckBlockchainSelectorUiState(
        blockchains = filteredBlockchains
    )

    fun updateFilter(text: String) {
        filteredBlockchains = if (text.isBlank()) {
            blockchains
        } else {
            blockchains.filter { it.name.contains(text, ignoreCase = true) }
        }

        emitState()
    }

}

object AddressCheckBlockchainSelectorModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddressCheckBlockchainSelectorViewModel(App.walletManager) as T
        }
    }
}

data class AddressCheckBlockchainSelectorUiState(
    val blockchains: List<Blockchain>
)