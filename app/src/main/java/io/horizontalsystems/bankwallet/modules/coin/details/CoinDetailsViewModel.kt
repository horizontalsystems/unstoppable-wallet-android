package io.horizontalsystems.bankwallet.modules.coin.details

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.MarketInfoDetails
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                        viewStateLiveData.postValue(ViewState.Error)
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
            volumeChart = null,//chartViewItem(item.totalVolumes,  service.coin.marketCapRank?.let { "#$it" })
            tvlChart = null,
            tvlRank = item.marketInfoDetails.tvlRank?.let { "#$it" },
            tvlRatio = item.marketInfoDetails.tvlRatio?.let { numberFormatter.format(it, 2, 2) },
            treasuries = item.marketInfoDetails.totalTreasuries?.let {
                numberFormatter.formatCurrencyValueAsShortened(
                    CurrencyValue(service.currency, it)
                )
            },
            fundsInvested = item.marketInfoDetails.totalFundsInvested?.let {
                numberFormatter.formatCurrencyValueAsShortened(
                    CurrencyValue(service.currency, it)
                )
            },
            reportsCount = if (item.marketInfoDetails.reportsCount == 0) null else item.marketInfoDetails.reportsCount.toString(),
            securityViewItems = securityViewItems(item.marketInfoDetails),
            auditAddresses = service.auditAddresses

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

