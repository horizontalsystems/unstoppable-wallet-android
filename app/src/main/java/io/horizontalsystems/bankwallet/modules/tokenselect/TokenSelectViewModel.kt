package io.horizontalsystems.bankwallet.modules.tokenselect

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.BalanceHiddenManager
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.EvmSyncSourceManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.WalletManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.sorting.sortedByCriteria
import io.horizontalsystems.bankwallet.core.storage.EnabledWalletsCacheDao
import io.horizontalsystems.bankwallet.core.title
import io.horizontalsystems.bankwallet.modules.balance.BalanceActiveWalletRepository
import io.horizontalsystems.bankwallet.modules.balance.BalanceAdapterRepository
import io.horizontalsystems.bankwallet.modules.balance.BalanceCache
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule
import io.horizontalsystems.bankwallet.modules.balance.BalanceService
import io.horizontalsystems.bankwallet.modules.balance.BalanceSorter
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem2
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItemFactory
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewTypeManager
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel(assistedFactory = TokenSelectViewModel.Factory::class)
class TokenSelectViewModel @AssistedInject constructor(
    @Assisted("blockchainTypes") private val blockchainTypes: List<BlockchainType>?,
    @Assisted("tokenTypes") private val tokenTypes: List<TokenType>?,
    private val walletManager: WalletManager,
    private val evmSyncSourceManager: EvmSyncSourceManager,
    private val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper,
    private val adapterManager: IAdapterManager,
    private val enabledWalletsCacheDao: EnabledWalletsCacheDao,
    private val localStorage: ILocalStorage,
    private val connectivityManager: ConnectivityManager,
    private val accountManager: IAccountManager,
    private val balanceViewTypeManager: BalanceViewTypeManager,
    private val balanceHiddenManager: BalanceHiddenManager,
) : ViewModelUiState<TokenSelectUiState>() {

    private lateinit var service: BalanceService
    private lateinit var balanceViewItemFactory: BalanceViewItemFactory

    private var noItems = false
    private var hasAssets = false
    private var query: String? = null
    private var balanceViewItems = listOf<BalanceViewItem2>()
    private var availableBlockchainTypes: List<BlockchainType>? = blockchainTypes
    private val allTab = SelectChainTab(title = Translator.getString(R.string.Market_All), null)
    private var selectedChainTab: SelectChainTab = allTab

    override fun createState() = TokenSelectUiState(
        items = balanceViewItems,
        noItems = noItems,
        hasAssets = hasAssets,
        selectedTab = selectedChainTab,
        tabs = getTabs(),
        balanceHidden = balanceHiddenManager.balanceHidden
    )

    override fun onCleared() {
        service.clear()
    }

    init {
        service = BalanceService(
            BalanceActiveWalletRepository(walletManager, evmSyncSourceManager),
            BalanceXRateRepository("wallet", currencyManager, marketKit),
            BalanceAdapterRepository(adapterManager, BalanceCache(enabledWalletsCacheDao)),
            localStorage,
            connectivityManager,
            BalanceSorter(),
            accountManager
        )
        balanceViewItemFactory = BalanceViewItemFactory()

        service.start()

        viewModelScope.launch {
            service.balanceItemsFlow.collect { items ->
                val initialFilteredItems = items?.filter { item ->
                    blockchainTypes?.contains(item.wallet.token.blockchainType) ?: true
                }

                availableBlockchainTypes = initialFilteredItems
                    ?.map { it.wallet.token.blockchainType }
                    ?.distinct()

                refreshViewItems(items)
            }
        }

        viewModelScope.launch {
            balanceHiddenManager.balanceHiddenFlow.collect {
                emitState()
            }
        }
    }

    fun updateFilter(q: String) {
        query = q
        viewModelScope.launch {
            refreshViewItems(service.balanceItemsFlow.value)
        }
    }

    fun onTabSelected(tab: SelectChainTab) {
        selectedChainTab = tab
        viewModelScope.launch {
            refreshViewItems(service.balanceItemsFlow.value)
        }
    }


    private suspend fun refreshViewItems(balanceItems: List<BalanceModule.BalanceItem>?) {
        withContext(Dispatchers.IO) {
            if (balanceItems == null) {
                balanceViewItems = emptyList()
                hasAssets = false
                noItems = true
                emitState()
                return@withContext
            }
            hasAssets = balanceItems.isNotEmpty()

            val currentQuery = query // Local copy for thread safety
            val currentSelectedChainTab = selectedChainTab // Local copy

            val filteredItems = balanceItems.asSequence()
                .filter { item ->
                    blockchainTypes?.contains(item.wallet.token.blockchainType) ?: true
                }
                .filter { item ->
                    currentSelectedChainTab.blockchainType?.let { it == item.wallet.token.blockchainType }
                        ?: true
                }
                .filter { item -> tokenTypes?.contains(item.wallet.token.type) ?: true }
                .filter { item ->
                    if (!currentQuery.isNullOrBlank()) {
                        val coin = item.wallet.coin
                        coin.code.contains(currentQuery, true) || coin.name.contains(
                            currentQuery,
                            true
                        )
                    } else {
                        true
                    }
                }
                .toList()

            noItems = filteredItems.isEmpty()

            val itemsSorted = filteredItems.sortedByCriteria(BalanceSorter.SEND_CRITERIA)
            balanceViewItems = itemsSorted.map { balanceItem ->
                balanceViewItemFactory.viewItem2(
                    item = balanceItem,
                    currency = service.baseCurrency,
                    watchAccount = service.isWatchAccount,
                    balanceViewType = balanceViewTypeManager.balanceViewTypeFlow.value,
                    networkAvailable = service.networkAvailable,
                    amountRoundingEnabled = localStorage.amountRoundingEnabled
                )
            }

            emitState()
        }
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

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("blockchainTypes") blockchainTypes: List<BlockchainType>?,
            @Assisted("tokenTypes") tokenTypes: List<TokenType>?,
        ): TokenSelectViewModel
    }
}

data class TokenSelectUiState(
    val items: List<BalanceViewItem2>,
    val noItems: Boolean,
    val hasAssets: Boolean,
    val selectedTab: SelectChainTab,
    val tabs: List<SelectChainTab>,
    val balanceHidden: Boolean,
)

data class SelectChainTab(
    val title: String,
    val blockchainType: BlockchainType?,
)
