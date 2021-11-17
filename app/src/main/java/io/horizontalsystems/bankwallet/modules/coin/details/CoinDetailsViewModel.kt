package io.horizontalsystems.bankwallet.modules.coin.details

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.modules.coin.CoinViewFactory
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.ChartInfoHeaderItem
import io.horizontalsystems.core.entities.Currency
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
                MarketInfoDetails.SecurityLevel.Low -> SecurityLevel.Low
                MarketInfoDetails.SecurityLevel.Medium -> SecurityLevel.Medium
                MarketInfoDetails.SecurityLevel.High -> SecurityLevel.High
            }
            securityViewItems.add(SecurityViewItem(SecurityType.Privacy, privacy.title, privacy.grade))
        }

        marketInfoDetails.decentralizedIssuance?.let { decentralizedIssuance ->
            val issuance = when (decentralizedIssuance) {
                true -> SecurityIssuance.Decentralized
                false -> SecurityIssuance.Centralized
            }
            securityViewItems.add(SecurityViewItem(SecurityType.Issuance, issuance.title, issuance.grade))
        }

        marketInfoDetails.confiscationResistant?.let { confiscationResistant ->
            val resistance = when (confiscationResistant) {
                true -> SecurityResistance.Yes
                false -> SecurityResistance.No
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
                true -> SecurityResistance.Yes
                false -> SecurityResistance.No
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

    fun refresh() {
        service.refresh()
        viewModelScope.launch {
            isRefreshingLiveData.postValue(true)
            delay(1000)
            isRefreshingLiveData.postValue(false)
        }
    }

    data class ViewItem(
        val hasMajorHolders: Boolean,
        val volumeChart: ChartViewItem?,
        val tvlChart: ChartViewItem?,
        val tvlRank: String?,
        val tvlRatio: String?,
        val treasuries: String?,
        val fundsInvested: String?,
        val reportsCount: String?,
        val securityViewItems: List<SecurityViewItem>,
        val auditAddresses: List<String>
    )

    data class ChartViewItem(
        val title: String,
        val subtitle: ChartInfoHeaderItem,
        val badge: String?,
        val currency: Currency,
        val chartInfoData: ChartInfoData
    )

    data class SecurityViewItem(
        val type: SecurityType,
        @StringRes
        val value: Int,
        val grade: SecurityGrade
    )

    enum class SecurityLevel(@StringRes val title: Int, val grade: SecurityGrade) {
        Low(R.string.CoinPage_SecurityParams_Low, SecurityGrade.Low),
        Medium(R.string.CoinPage_SecurityParams_Medium, SecurityGrade.Medium),
        High(R.string.CoinPage_SecurityParams_High, SecurityGrade.High)
    }

    enum class SecurityIssuance(@StringRes val title: Int, val grade: SecurityGrade) {
        Decentralized(R.string.CoinPage_SecurityParams_Decentralized, SecurityGrade.High),
        Centralized(R.string.CoinPage_SecurityParams_Centralized, SecurityGrade.Low)
    }

    enum class SecurityResistance(@StringRes val title: Int, val grade: SecurityGrade) {
        Yes(R.string.CoinPage_SecurityParams_Yes, SecurityGrade.High),
        No(R.string.CoinPage_SecurityParams_No, SecurityGrade.Low),
    }

    enum class SecurityGrade {
        Low, Medium, High;
    }

    enum class SecurityType(@StringRes val title: Int) {
        Privacy(R.string.CoinPage_SecurityParams_Privacy),
        Issuance(R.string.CoinPage_SecurityParams_Issuance),
        ConfiscationResistance(R.string.CoinPage_SecurityParams_ConfiscationResistance),
        CensorshipResistance(R.string.CoinPage_SecurityParams_CensorshipResistance)
    }

}
