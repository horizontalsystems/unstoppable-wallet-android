package io.horizontalsystems.bankwallet.modules.coin.details

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.CoinViewFactory
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.CensorshipResistanceLevel
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.ConfiscationResistanceLevel
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.IssuanceLevel
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.PrivacyLevel
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.SecurityInfoViewItem
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.SecurityType
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.SecurityViewItem
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.ViewItem
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.chartview.ChartDataFactory
import io.horizontalsystems.marketkit.models.ChartPoint
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.MarketInfoDetails
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

class CoinDetailsViewModel(
    private val service: CoinDetailsService,
    private val coinViewFactory: CoinViewFactory,
    private val numberFormatter: IAppNumberFormatter
) : ViewModel() {

    private val disposables = CompositeDisposable()

    val coin: Coin
        get() = service.coin

    val viewStateLiveData = MutableLiveData<ViewState>()
    val loadingLiveData = MutableLiveData<Boolean>()
    val isRefreshingLiveData = MutableLiveData<Boolean>()
    val viewItemLiveData = MutableLiveData<ViewItem>()

    init {
        service.stateObservable
            .subscribeIO { state ->
                loadingLiveData.postValue(state == DataState.Loading)
                when (state) {
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

    fun securityInfoViewItems(type: SecurityType): List<SecurityInfoViewItem> =
        when (type) {
            SecurityType.Privacy -> {
                PrivacyLevel.values().map {
                    SecurityInfoViewItem(it.grade, it.title, it.description)
                }
            }
            SecurityType.Issuance -> {
                IssuanceLevel.values().map {
                    SecurityInfoViewItem(it.grade, it.title, it.description)
                }
            }
            SecurityType.ConfiscationResistance -> {
                ConfiscationResistanceLevel.values().map {
                    SecurityInfoViewItem(it.grade, it.title, it.description)
                }
            }
            SecurityType.CensorshipResistance -> {
                CensorshipResistanceLevel.values().map {
                    SecurityInfoViewItem(it.grade, it.title, it.description)
                }
            }
        }


    private fun viewItem(item: CoinDetailsService.Item): ViewItem {
        return ViewItem(
            hasMajorHolders = service.hasMajorHolders,
            volumeChart = chart(values = item.totalVolumes, badge = service.coin.marketCapRank?.let { "#$it" }),
            tvlChart = chart(item.tvls),
            tvlRank = item.marketInfoDetails.tvlRank?.let { "#$it" },
            tvlRatio = item.marketInfoDetails.tvlRatio?.let { numberFormatter.format(it, 2, 2) },
            treasuries = item.marketInfoDetails.totalTreasuries?.let {
                numberFormatter.formatCurrencyValueAsShortened(
                    CurrencyValue(service.currency, it)
                )
            },
            fundsInvested = item.marketInfoDetails.totalFundsInvested?.let {
                numberFormatter.formatCurrencyValueAsShortened(
                    CurrencyValue(service.usdCurrency, it)
                )
            },
            reportsCount = if (item.marketInfoDetails.reportsCount == 0) null else item.marketInfoDetails.reportsCount.toString(),
            securityViewItems = securityViewItems(item.marketInfoDetails),
            auditAddresses = service.auditAddresses
        )
    }

    private fun chart(values: List<ChartPoint>?, badge: String? = null): CoinDetailsModule.ChartViewItem? {
        if (values.isNullOrEmpty()) return null

        val first = values.first()
        val last = values.last()

        val points = values.map {
            io.horizontalsystems.chartview.models.ChartPoint(it.value.toFloat(),
                null,
                it.timestamp)
        }

        val diff = (last.value / first.value - BigDecimal.ONE) * BigDecimal(100)
        val chartData = ChartDataFactory.build(points, first.timestamp, last.timestamp, false)
        val value = App.numberFormatter.formatCurrencyValueAsShortened(CurrencyValue(service.currency, last.value))

        return CoinDetailsModule.ChartViewItem(
            badge = badge,
            value = value,
            diff = Value.Percent(diff),
            chartData = chartData
        )
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
//    private fun chartViewItem(values: List<ChartPoint>?, title: String, badge: String? = null): ChartViewItem? {
//        if (values == null) return null
//
//        val first = values.firstOrNull() ?: return null
//        val last = values.lastOrNull() ?: return null
//
//        val chartItems = values.map { ChartItem() }
//
//        coinViewFactory.createChartInfoData(
//            ChartType.MONTHLY_BY_DAY,
//
//        )
//
//        val chartViewItem = chartFactory.convert(
//            values,
//            service.chartType,
//            MetricChartModule.ValueType.CompactCurrencyValue,
//            service.baseCurrency
//        )
//        val chartInfoData = ChartInfoData(
//            chartViewItem.chartData,
//            chartViewItem.chartType,
//            chartViewItem.maxValue,
//            chartViewItem.minValue
//        )
//
//        return ChartViewItem(
//            title = title,
//            subtitle = ChartInfoHeaderItem()
//        )
//
//    }

