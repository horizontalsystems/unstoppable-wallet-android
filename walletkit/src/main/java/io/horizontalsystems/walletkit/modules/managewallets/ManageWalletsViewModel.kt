package io.horizontalsystems.walletkit.modules.managewallets

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.Clearable
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.alternativeImageUrl
import io.horizontalsystems.walletkit.core.badge
import io.horizontalsystems.walletkit.core.iconPlaceholder
import io.horizontalsystems.walletkit.core.imageUrl
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.core.supported
import io.horizontalsystems.walletkit.core.title
import io.horizontalsystems.walletkit.modules.market.ImageSource
import io.horizontalsystems.walletkit.modules.restoreaccount.restoreblockchains.CoinViewItem
import io.horizontalsystems.walletkit.modules.tokenselect.SelectChainTab
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.launch

class ManageWalletsViewModel(
    private val service: ManageWalletsService,
    private val clearables: List<Clearable>
) : ViewModelUiState<ManageWalletsViewModel.ManageWalletsUiState>() {

    private var coinItems: List<CoinViewItem<Token>> = listOf()
    private var searchQuery = ""
    private val allTab = SelectChainTab(title = Translator.getString(R.string.Market_All), null)
    private var selectedChainTab: SelectChainTab = allTab
    private var availableBlockchainTypes: List<BlockchainType>? = BlockchainType.supported

    val addTokenEnabled: Boolean
        get() = service.accountType?.canAddTokens ?: false

    init {
        viewModelScope.launch {
            service.itemsFlow.collect {
                sync(it)
            }
        }
    }

    override fun createState() = ManageWalletsUiState(
        items = coinItems,
        searchQuery = searchQuery,
        selectedTab = selectedChainTab,
        tabs = getTabs()
    )

    fun onTabSelected(tab: SelectChainTab) {
        selectedChainTab = tab
        sync(service.items)
    }

    private fun getTabs(): List<SelectChainTab> {
        val currentAvailableBlockchainTypes = availableBlockchainTypes
        if (currentAvailableBlockchainTypes.isNullOrEmpty() || currentAvailableBlockchainTypes.size == 1) {
            return emptyList()
        }

        return listOf(allTab) + currentAvailableBlockchainTypes.map { blockchainType ->
            SelectChainTab(
                title = blockchainType.title,
                blockchainType = blockchainType
            )
        }
    }

    private fun sync(items: List<ManageWalletsService.Item>) {
        coinItems = items
            .filter { it.token.blockchainType == selectedChainTab.blockchainType || selectedChainTab.blockchainType == null }
            .map { viewItem(it) }
        emitState()
    }

    private fun viewItem(
        item: ManageWalletsService.Item,
    ) = CoinViewItem(
        item = item.token,
        imageSource = ImageSource.Remote(
            item.token.coin.imageUrl,
            item.token.iconPlaceholder,
            item.token.coin.alternativeImageUrl
        ),
        title = item.token.coin.code,
        subtitle = item.token.coin.name,
        enabled = item.enabled,
        hasInfo = item.hasInfo,
        label = item.token.badge
    )

    fun enable(token: Token) {
        service.enable(token)
    }

    fun disable(token: Token) {
        service.disable(token)
    }

    fun updateFilter(filter: String) {
        service.setFilter(filter)
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
    }

    data class ManageWalletsUiState(
        val items: List<CoinViewItem<Token>>,
        val searchQuery: String,
        val selectedTab: SelectChainTab,
        val tabs: List<SelectChainTab>,
    )
}
