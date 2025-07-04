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
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle
import io.horizontalsystems.marketkit.models.Apy
import io.horizontalsystems.marketkit.models.Vault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlin.coroutines.cancellation.CancellationException

class MarketEarnViewModel(
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager,
) : ViewModelUiState<EarnModule.UiState>() {

    val filterOptions = EarnModule.FilterBy.entries
    val apyPeriods = ApyPeriod.entries
    var chainOptions =
        listOf<EarnModule.VaultChainOption>() // Will be populated based on available vaults
        private set

    private var isRefreshing = false
    private var viewState: ViewState = ViewState.Loading
    private var vaultViewItems: List<EarnModule.VaultViewItem> = listOf()
    private var cache: List<Vault> = listOf() // Cache for vaults to avoid unnecessary API calls
    private var apyPeriod: ApyPeriod = ApyPeriod.SEVEN_DAY // Default period
    private var marketDataJob: Job? = null
    private var filterBy: EarnModule.FilterBy = EarnModule.FilterBy.AllAssets
    private var chainSelected: EarnModule.VaultChainOption =
        EarnModule.VaultChainOption.AllChains

    private val usdCurrency: Currency by lazy {
        currencyManager.currencies.first { it.code == "USD" }
    }

    init {
        getVaults()
    }

    fun onFilterBySelected(filterBy: EarnModule.FilterBy) {
        this.filterBy = filterBy
        updateViewItems()
    }

    fun onApyPeriodSelected(apyPeriod: ApyPeriod) {
        this.apyPeriod = apyPeriod
        updateViewItems()
    }

    fun onChainSelected(chainOption: EarnModule.VaultChainOption) {
        chainSelected = chainOption
        updateViewItems()
    }

    fun refresh() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    private fun getVaults() {
        marketDataJob?.cancel()
        marketDataJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                cache = marketKit.vaults().blockingGet()
                updateVaultChains()
                updateViewItems()
                viewState = ViewState.Success
            } catch (e: CancellationException) {
                // no-op
            } catch (e: Throwable) {
                viewState = ViewState.Error(e)
            }
            emitState()
        }
    }

    private fun updateVaultChains() {
        val chains = cache.map { it.chain }.distinct().sortedBy { it }
        chainOptions = listOf(EarnModule.VaultChainOption.AllChains) +
                chains.mapNotNull { EarnModule.VaultChainOption.fromString(it) }

        chainSelected = EarnModule.VaultChainOption.AllChains
        emitState()
    }

    override fun createState(): EarnModule.UiState {
        return EarnModule.UiState(
            isRefreshing = isRefreshing,
            viewState = viewState,
            items = vaultViewItems,
            filterBy = filterBy,
            apyPeriod = apyPeriod,
            chainSelected = chainSelected,
        )
    }

    private fun updateViewItems() {
        vaultViewItems = cache.filter { vault ->
            when (filterBy) {
                EarnModule.FilterBy.AllAssets -> true
                EarnModule.FilterBy.EthYield -> vault.assetSymbol.contains("ETH")
                EarnModule.FilterBy.UsdYield -> !vault.assetSymbol.contains("ETH")
            }
        }
            .filter { vault ->
                chainSelected == EarnModule.VaultChainOption.AllChains ||
                        vault.chain.equals(chainSelected.uid, ignoreCase = true)
            }
            .map { vaultViewItem(it) }
            .sortedByDescending { it.apy }

        emitState()
    }

    private fun vaultViewItem(vault: Vault): EarnModule.VaultViewItem =
        EarnModule.VaultViewItem(
            address = vault.address,
            name = vault.name,
            apy = vault.apy.getByPeriod(apyPeriod),
            tvl = App.numberFormatter.formatFiatShort(
                vault.tvl.toBigDecimal(),
                usdCurrency.symbol,
                usdCurrency.decimal
            ),
            chain = vault.chain.replaceFirstChar(Char::uppercase),
            url = vault.url,
            holders = vault.holders?.toString() ?: "---",
            assetSymbol = vault.assetSymbol,
            protocolName = vault.protocolName.replaceFirstChar(Char::titlecase),
            protocolLogo = vault.protocolLogo
        )

    private fun refreshWithMinLoadingSpinnerPeriod() {
        isRefreshing = true
        emitState()
        viewModelScope.launch {
            delay(1000)
            isRefreshing = false
            emitState()
        }
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

    data class UiState(
        val isRefreshing: Boolean,
        val viewState: ViewState = ViewState.Loading,
        val items: List<VaultViewItem> = listOf(),
        val filterBy: FilterBy,
        val apyPeriod: ApyPeriod,
        val chainSelected: VaultChainOption,
    )

    data class VaultViewItem(
        val address: String,
        val name: String,
        val apy: BigDecimal,
        val tvl: String,
        val chain: String,
        val url: String?,
        val holders: String,
        val assetSymbol: String,
        val protocolName: String,
        val protocolLogo: String,
    )

    enum class VaultChainOption(val uid: String) : WithTranslatableTitle {
        AllChains(Translator.getString(R.string.MarketEarn_Filter_AllChains)),
        Ethereum("Ethereum"),
        Arbitrum("Arbitrum"),
        Base("Base"),
        Optimism("Optimism");

        override val title = TranslatableString.PlainString(uid)

        companion object {
            fun fromString(chain: String): VaultChainOption? {
                return entries.firstOrNull { it.uid.equals(chain, ignoreCase = true) }
            }
        }
    }

    enum class FilterBy(@StringRes val titleResId: Int) : WithTranslatableTitle {
        AllAssets(R.string.MarketEarn_Filter_AllAssets),
        EthYield(R.string.MarketEarn_Filter_ETHYield),
        UsdYield(R.string.MarketEarn_Filter_USDYield);

        override val title = TranslatableString.ResString(titleResId)
    }
}

private fun Apy.getByPeriod(period: ApyPeriod): BigDecimal {
    val stringValue = when (period) {
        ApyPeriod.ONE_DAY -> this.oneDay
        ApyPeriod.SEVEN_DAY -> this.sevenDay
        ApyPeriod.THIRTY_DAY -> this.thirtyDay
    }
    return stringValue.toBigDecimalOrNull()?.times(BigDecimal(100)) ?: BigDecimal.ZERO
}