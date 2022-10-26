package io.horizontalsystems.bankwallet.modules.coin.details

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.chart.ChartCurrencyValueFormatterShortened
import io.horizontalsystems.bankwallet.modules.chart.ChartModule
import io.horizontalsystems.bankwallet.modules.chart.ChartNumberFormatterShortened
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.CensorshipResistanceLevel
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.ConfiscationResistanceLevel
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.IssuanceLevel
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.PrivacyLevel
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.SecurityType
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.SecurityViewItem
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.ViewItem
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.chartview.ChartDataBuilder
import io.horizontalsystems.marketkit.models.ChartPoint
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.MarketInfoDetails
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CoinDetailsViewModel(
    private val service: CoinDetailsService
) : ViewModel() {

    private val numberFormatter = ChartNumberFormatterShortened()
    private val currencyValueFormatter = ChartCurrencyValueFormatterShortened()
    private val disposables = CompositeDisposable()

    val coin: Coin
        get() = service.coin

    val viewStateLiveData = MutableLiveData<ViewState>(ViewState.Loading)
    val isRefreshingLiveData = MutableLiveData<Boolean>()
    val viewItemLiveData = MutableLiveData<ViewItem>()

    init {
        service.stateObservable
            .subscribeIO { state ->
                when (state) {
                    is DataState.Loading -> {
                        viewStateLiveData.postValue(ViewState.Loading)
                    }
                    is DataState.Success -> {
                        viewStateLiveData.postValue(ViewState.Success)

                        viewItemLiveData.postValue(viewItem(state.data))
                    }
                    is DataState.Error -> {
                        viewStateLiveData.postValue(ViewState.Error(state.error))
                    }
                }
            }
            .let {
                disposables.add(it)
            }

        service.start()
    }

    fun refresh() {
        service.refresh()
        viewModelScope.launch {
            isRefreshingLiveData.postValue(true)
            delay(1000)
            isRefreshingLiveData.postValue(false)
        }
    }

    override fun onCleared() {
        disposables.clear()
    }

    private fun viewItem(item: CoinDetailsService.Item): ViewItem {
        return ViewItem(
            proChartsActivated = item.proCharts.activated,
            tokenLiquidityViewItem = getTokenLiquidityViewItem(item.proCharts),
            tokenDistributionViewItem = getTokenDistributionViewItem(item.proCharts, service.hasMajorHolders),
            tvlChart = chart(item.tvls, currencyValueFormatter),
            tvlRank = item.marketInfoDetails.tvlRank?.let { "#$it" },
            tvlRatio = item.marketInfoDetails.tvlRatio?.let { App.numberFormatter.format(it, 2, 2) },
            treasuries = item.marketInfoDetails.totalTreasuries?.let {
                numberFormatter.formatValue(service.currency, it)
            },
            fundsInvested = item.marketInfoDetails.totalFundsInvested?.let {
                numberFormatter.formatValue(service.usdCurrency, it)
            },
            reportsCount = if (item.marketInfoDetails.reportsCount == 0) null else item.marketInfoDetails.reportsCount.toString(),
            securityViewItems = securityViewItems(item.marketInfoDetails),
            auditAddresses = service.auditAddresses
        )
    }

    private fun getTokenLiquidityViewItem(proCharts: CoinDetailsService.ProCharts): CoinDetailsModule.TokenLiquidityViewItem? {
        val volume = chart(proCharts.dexVolumes, currencyValueFormatter)
        val liquidity = chart(proCharts.dexLiquidity, currencyValueFormatter)

        if (volume == null && liquidity == null) return null

        return CoinDetailsModule.TokenLiquidityViewItem(volume, liquidity)
    }

    private fun getTokenDistributionViewItem(proCharts: CoinDetailsService.ProCharts, hasMajorHolders: Boolean): CoinDetailsModule.TokenDistributionViewItem? {
        val txCount = chart(proCharts.txCount, numberFormatter)
        val txVolume = chart(proCharts.txVolume, currencyValueFormatter)
        val activeAddresses = chart(proCharts.activeAddresses, numberFormatter)

        if (txCount == null && txVolume == null && activeAddresses == null && !hasMajorHolders) return null

        return CoinDetailsModule.TokenDistributionViewItem(txCount, txVolume, activeAddresses, hasMajorHolders)
    }

    private fun chart(values: List<ChartPoint>?, valueFormatter: ChartModule.ChartNumberFormatter): CoinDetailsModule.ChartViewItem? {
        if (values.isNullOrEmpty()) return null

        val first = values.first()
        val last = values.last()

        val points = values.map {
            io.horizontalsystems.chartview.models.ChartPoint(it.value.toFloat(), it.timestamp)
        }

        val lastItemValue = last.value
        val firstItemValue = first.value

        val diff = (lastItemValue - firstItemValue) / firstItemValue * 100.toBigDecimal()
        val chartData = ChartDataBuilder.buildFromPoints(points)
        val value = valueFormatter.formatValue(service.currency, lastItemValue)

        val percentDiff = Value.Percent(diff)

        return CoinDetailsModule.ChartViewItem(
            value = value,
            diff = App.numberFormatter.formatValueAsDiff(percentDiff),
            chartData = chartData,
            if (percentDiff.raw().signum() >= 0) CoinDetailsModule.ChartMovementTrend.Up else CoinDetailsModule.ChartMovementTrend.Down
        )
    }

    private fun chart(proData: CoinDetailsService.ProData, valueFormatter: ChartModule.ChartNumberFormatter): CoinDetailsModule.ChartViewItem? =
        when (proData) {
            is CoinDetailsService.ProData.Empty -> null
            is CoinDetailsService.ProData.Forbidden -> CoinDetailsModule.ChartViewItem(
                "***",
                Translator.getString(R.string.CoinPage_Chart_Locked),
                ChartDataBuilder.placeholder,
                CoinDetailsModule.ChartMovementTrend.Neutral
            )

            is CoinDetailsService.ProData.Completed -> chart(proData.chartPoints, valueFormatter)
        }

    private fun securityViewItems(marketInfoDetails: MarketInfoDetails): List<SecurityViewItem> {
        val securityViewItems = mutableListOf<SecurityViewItem>()

        marketInfoDetails.privacy?.let {
            val privacy = when (it) {
                MarketInfoDetails.SecurityLevel.Low -> PrivacyLevel.Low
                MarketInfoDetails.SecurityLevel.Medium -> PrivacyLevel.Medium
                MarketInfoDetails.SecurityLevel.High -> PrivacyLevel.High
            }
            securityViewItems.add(SecurityViewItem(SecurityType.Privacy, privacy.title, privacy.grade))
        }

        marketInfoDetails.decentralizedIssuance?.let { decentralizedIssuance ->
            val issuance = when (decentralizedIssuance) {
                true -> IssuanceLevel.Decentralized
                false -> IssuanceLevel.Centralized
            }
            securityViewItems.add(SecurityViewItem(SecurityType.Issuance, issuance.title, issuance.grade))
        }

        marketInfoDetails.confiscationResistant?.let { confiscationResistant ->
            val resistance = when (confiscationResistant) {
                true -> ConfiscationResistanceLevel.Yes
                false -> ConfiscationResistanceLevel.No
            }
            securityViewItems.add(
                SecurityViewItem(
                    SecurityType.ConfiscationResistance,
                    resistance.title,
                    resistance.grade
                )
            )
        }

        marketInfoDetails.censorshipResistant?.let { censorshipResistant ->
            val resistance = when (censorshipResistant) {
                true -> CensorshipResistanceLevel.Yes
                false -> CensorshipResistanceLevel.No
            }
            securityViewItems.add(
                SecurityViewItem(
                    SecurityType.CensorshipResistance,
                    resistance.title,
                    resistance.grade
                )
            )
        }

        return securityViewItems
    }
}
