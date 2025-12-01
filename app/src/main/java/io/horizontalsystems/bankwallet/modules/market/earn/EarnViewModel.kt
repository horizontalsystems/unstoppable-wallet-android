package io.horizontalsystems.bankwallet.modules.market.earn

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.earn.EarnModule.ApyPeriod
import io.horizontalsystems.bankwallet.modules.market.earn.EarnModule.VaultSorting
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle
import io.horizontalsystems.marketkit.models.Apy
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.Vault
import io.horizontalsystems.subscriptions.core.TokenInsights
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal
import kotlin.coroutines.cancellation.CancellationException

class MarketEarnViewModel(
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager,
) : ViewModelUiState<EarnModule.UiState>() {

    val filterOptions = EarnModule.FilterBy.entries
    val apyPeriods = ApyPeriod.entries
    val sortingOptions = VaultSorting.entries

    private companion object {
        const val VISIBLE_ITEMS_NO_PREMIUM = 7
        const val BLURRED_ITEMS_NO_PREMIUM = 5
        const val TOTAL_ITEMS_NO_PREMIUM = VISIBLE_ITEMS_NO_PREMIUM + BLURRED_ITEMS_NO_PREMIUM
        const val REFRESH_SPINNER_MIN_DURATION_MS = 1000L
    }

    private var blockchains: List<Blockchain> = emptyList()
    private var selectedBlockchains: List<Blockchain> = emptyList()
    private var isRefreshing = false
    private var viewState: ViewState = ViewState.Loading
    private var cache: List<Vault> = listOf() // Cache for vaults to avoid unnecessary API calls
    private var apyPeriod: ApyPeriod = ApyPeriod.SEVEN_DAY
    private var sortingBy: VaultSorting = VaultSorting.APY
    private var marketDataJob: Job? = null
    private var filterBy: EarnModule.FilterBy = EarnModule.FilterBy.AllAssets
    private val hasPremium: Boolean
        get() = UserSubscriptionManager.isActionAllowed(TokenInsights)

    private var baseCurrency = currencyManager.baseCurrency

    // Cached processed items - only recalculate when filters/sorting change
    private var cachedViewItems: List<EarnModule.VaultViewItem> = emptyList()
    private var lastCacheKey: CacheKey? = null

    // Cache key to track when we need to recalculate view items
    private data class CacheKey(
        val vaultCount: Int,
        val filterBy: EarnModule.FilterBy,
        val apyPeriod: ApyPeriod,
        val sortingBy: VaultSorting,
        val selectedBlockchainUids: Set<String>,
        val baseCurrencyCode: String
    )

    init {
        viewModelScope.launch {
            UserSubscriptionManager.activeSubscriptionStateFlow.collect {
                resetMenu()
                fetchVaults(forceRefresh = true)
            }
        }
        viewModelScope.launch {
            currencyManager.baseCurrencyUpdatedFlow.collect {
                baseCurrency = currencyManager.baseCurrency
                invalidateCache() // Currency change requires recalculation
                fetchVaults(forceRefresh = true)
            }
        }
        fetchVaults()
    }

    fun onFilterBySelected(filterBy: EarnModule.FilterBy) {
        this.filterBy = filterBy
        invalidateCache()
        emitState()
    }

    fun onApyPeriodSelected(apyPeriod: ApyPeriod) {
        this.apyPeriod = apyPeriod
        invalidateCache()
        emitState()
    }

    fun onSortingSelected(option: VaultSorting) {
        this.sortingBy = option
        invalidateCache()
        emitState()
    }

    fun refresh() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onBlockchainsSelected(blockchains: List<Blockchain>) {
        selectedBlockchains = blockchains
        invalidateCache()
        emitState()
    }

    private fun invalidateCache() {
        lastCacheKey = null
        cachedViewItems = emptyList()
    }

    private fun getCurrentCacheKey(): CacheKey {
        return CacheKey(
            vaultCount = cache.size,
            filterBy = filterBy,
            apyPeriod = apyPeriod,
            sortingBy = sortingBy,
            selectedBlockchainUids = selectedBlockchains.map { it.uid }.toSet(),
            baseCurrencyCode = baseCurrency.code
        )
    }

    private fun getProcessedViewItems(): List<EarnModule.VaultViewItem> {
        val currentCacheKey = getCurrentCacheKey()

        if (lastCacheKey == currentCacheKey && cachedViewItems.isNotEmpty()) {
            return cachedViewItems
        }

        cachedViewItems = calculateViewItems()
        lastCacheKey = currentCacheKey
        return cachedViewItems
    }

    private fun fetchVaults(forceRefresh: Boolean = false) {
        if (marketDataJob?.isActive == true && !forceRefresh) {
            return
        }
        marketDataJob?.cancel()
        viewState = ViewState.Loading
        emitState()

        marketDataJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val newVaults = marketKit.vaults(currencyManager.baseCurrency.code).await()
                cache = newVaults
                updateVaultChains()
                invalidateCache() // New data requires recalculation
                viewState = ViewState.Success
                emitState()
            } catch (e: CancellationException) {
                // no-op
            } catch (e: Throwable) {
                viewState = ViewState.Error(e)
                emitState()
            }
        }
    }

    private fun updateVaultChains() {
        val chainUids = cache.map { it.chain }.distinct().sortedBy { it }
        blockchains = marketKit.blockchains(chainUids)
    }

    override fun createState(): EarnModule.UiState {
        val processedViewItems = getProcessedViewItems()

        val (vaults, blurredVaults) = getVaultsAndBlurredItems(processedViewItems)

        return EarnModule.UiState(
            isRefreshing = isRefreshing,
            viewState = viewState,
            items = vaults,
            blurredItems = blurredVaults,
            filterBy = filterBy,
            apyPeriod = apyPeriod,
            sortingBy = sortingBy,
            sortingByTitle = getSortingByTitle(sortingBy),
            noPremium = !hasPremium,
            blockchains = blockchains,
            chainSelectorMenuTitle = getChainsMenuTitle(selectedBlockchains),
            selectedBlockchains = selectedBlockchains,
        )
    }

    private fun getSortingByTitle(sortingBy: VaultSorting): String {
        return when (sortingBy) {
            VaultSorting.APY -> "APY"
            VaultSorting.TVL -> "TVL"
        }
    }

    private fun getVaultsAndBlurredItems(processedViewItems: List<EarnModule.VaultViewItem>) =
        if (hasPremium) {
            processedViewItems to emptyList()
        } else {
            if (processedViewItems.size > TOTAL_ITEMS_NO_PREMIUM) {
                val visible = processedViewItems.take(VISIBLE_ITEMS_NO_PREMIUM)
                val blurred =
                    processedViewItems.drop(VISIBLE_ITEMS_NO_PREMIUM).take(BLURRED_ITEMS_NO_PREMIUM)
                visible to blurred
            } else {
                processedViewItems to emptyList()
            }
        }

    private fun getChainsMenuTitle(selectedBlockchains: List<Blockchain>): String {
        return when (selectedBlockchains.size) {
            0 -> Translator.getString(R.string.Market_Vaults_Filter_AllChains)
            1 -> selectedBlockchains.first().name
            else -> Translator.getString(
                R.string.Market_Vaults_Filter_MultipleChains,
                selectedBlockchains.size
            )
        }
    }

    private fun calculateViewItems(): List<EarnModule.VaultViewItem> {
        val selectedBlockchainsUids = selectedBlockchains.map { it.uid }.toSet()
        return cache
            .filter { vault ->
                when (filterBy) {
                    EarnModule.FilterBy.AllAssets -> true
                    EarnModule.FilterBy.EthYield -> vault.assetSymbol.contains(
                        "ETH",
                        ignoreCase = true
                    )
                    EarnModule.FilterBy.UsdYield -> vault.assetSymbol.contains(
                        "USD",
                        ignoreCase = true
                    )
                }
            }
            .filter { vault ->
                if (selectedBlockchainsUids.isEmpty()) {
                    true
                } else {
                    selectedBlockchainsUids.contains(vault.chain)
                }
            }
            .map { vaultToViewItem(it, apyPeriod, blockchains, baseCurrency) }
            .sortedByDescending { vault ->
                if (sortingBy == VaultSorting.APY)
                    vault.apy
                else
                    vault.tvlRaw
            }
    }

    private fun vaultToViewItem(
        vault: Vault,
        apyPeriod: ApyPeriod,
        allBlockchains: List<Blockchain>,
        baseCurrency: Currency
    ): EarnModule.VaultViewItem {
        val tvlRaw = vault.tvl.toBigDecimalOrNull() ?: BigDecimal.ZERO
        return EarnModule.VaultViewItem(
            rank = vault.rank,
            address = vault.address,
            name = vault.name,
            apy = vault.apy.getByPeriod(apyPeriod),
            tvl = App.numberFormatter.formatFiatShort(
                tvlRaw,
                baseCurrency.symbol,
                baseCurrency.decimal
            ),
            tvlRaw = tvlRaw,
            blockchainName = allBlockchains.firstOrNull { it.uid == vault.chain }?.name
                ?: vault.chain.uppercase(),
            url = vault.url,
            holders = vault.holders?.toString(),
            assetSymbol = vault.assetSymbol,
            protocolName = vault.protocolName.replaceFirstChar(Char::titlecase),
            assetLogo = vault.protocolLogo,
        )
    }

    private fun refreshWithMinLoadingSpinnerPeriod() {
        viewModelScope.launch {
            isRefreshing = true
            emitState()
            fetchVaults(forceRefresh = true)
            delay(REFRESH_SPINNER_MIN_DURATION_MS)
            isRefreshing = false
            emitState()
        }
    }

    private fun resetMenu() {
        selectedBlockchains = emptyList()
        sortingBy = VaultSorting.APY
        apyPeriod = ApyPeriod.SEVEN_DAY
        filterBy = EarnModule.FilterBy.AllAssets
        invalidateCache()
    }
}

object EarnModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MarketEarnViewModel(
                App.marketKit,
                App.currencyManager
            ) as T
        }
    }

    enum class ApyPeriod(@StringRes val titleResId: Int) : WithTranslatableTitle {
        ONE_DAY(R.string.CoinPage_TimeDuration_Day),
        SEVEN_DAY(R.string.CoinPage_TimeDuration_Week),
        THIRTY_DAY(R.string.CoinPage_TimeDuration_Month);

        override val title = TranslatableString.ResString(titleResId)
    }

    enum class VaultSorting(@StringRes val titleResId: Int) : WithTranslatableTitle {
        APY(R.string.Market_Vaults_Sorting_APY),
        TVL(R.string.Market_Vaults_Sorting_TVL);

        override val title = TranslatableString.ResString(titleResId)
    }

    data class UiState(
        val isRefreshing: Boolean,
        val viewState: ViewState = ViewState.Loading,
        val items: List<VaultViewItem> = listOf(),
        val blurredItems: List<VaultViewItem> = listOf(),
        val filterBy: FilterBy,
        val apyPeriod: ApyPeriod,
        val sortingBy: VaultSorting,
        val sortingByTitle: String,
        val noPremium: Boolean,
        val chainSelectorMenuTitle: String,
        val selectedBlockchains: List<Blockchain>,
        val blockchains: List<Blockchain>,
    )

    data class VaultViewItem(
        val rank: Int,
        val address: String,
        val name: String,
        val apy: BigDecimal,
        val tvl: String,
        val tvlRaw: BigDecimal,
        val url: String?,
        val holders: String?,
        val assetSymbol: String,
        val assetLogo: String?,
        val protocolName: String,
        val blockchainName: String,
    )

    enum class FilterBy(@StringRes val titleResId: Int) : WithTranslatableTitle {
        AllAssets(R.string.Market_Vaults_Filter_AllAssets),
        EthYield(R.string.Market_Vaults_Filter_ETHYield),
        UsdYield(R.string.Market_Vaults_Filter_USDYield);

        override val title = TranslatableString.ResString(titleResId)
    }
}

private fun Apy.getByPeriod(period: ApyPeriod): BigDecimal {
    val stringValue = when (period) {
        ApyPeriod.ONE_DAY -> this.oneDay
        ApyPeriod.SEVEN_DAY -> this.sevenDay
        ApyPeriod.THIRTY_DAY -> this.thirtyDay
    }
    return stringValue.toBigDecimalOrNull() ?: BigDecimal.ZERO
}