package io.horizontalsystems.bankwallet.modules.coin.overview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.coin.*
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinChartAdapter
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinSubtitleAdapter
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.xrateskit.entities.ChartType
import io.horizontalsystems.xrateskit.entities.TimePeriod
import io.reactivex.disposables.CompositeDisposable
import retrofit2.HttpException
import java.math.BigDecimal

class CoinOverviewViewModel(
    private val service: CoinOverviewService,
    val coinCode: String,
    private val factory: CoinViewFactory,
    private val clearables: List<Clearable>
) : ViewModel() {

    val loadingLiveData = MutableLiveData<Boolean>()
    val coinInfoErrorLiveData = MutableLiveData<String>()
    val subtitleLiveData = MutableLiveData<CoinSubtitleAdapter.ViewItemWrapper>()
    val chartInfoLiveData = MutableLiveData<CoinChartAdapter.ViewItemWrapper>()
    val roiLiveData = MutableLiveData<List<RoiViewItem>>()
    val marketDataLiveData = MutableLiveData<List<CoinDataItem>>()
    val investorDataLiveData = MutableLiveData<List<CoinDataItem>>()
    val securityParamsLiveData = MutableLiveData<List<CoinDataItem>>()
    val categoriesLiveData = MutableLiveData<List<String>>()
    val contractInfoLiveData = MutableLiveData<List<CoinDataItem>>()
    val aboutTextLiveData = MutableLiveData<String>()
    val linksLiveData = MutableLiveData<List<CoinLink>>()
    val showFooterLiveData = MutableLiveData(false)

    val currency = service.currency

    private var latestRateText: String? = null
        set(value) {
            field = value
            syncSubtitle()
        }

    private var rateDiffValue: BigDecimal? = null
        set(value) {
            field = value
            syncSubtitle()
        }

    private var chartInfoData: ChartInfoData? = null
    private var showChartSpinner: Boolean = false
    private var showChartError: Boolean = false

    val coinType: CoinType
        get() = service.coinType

    val coinUid by service::coinUid

    private val disposable = CompositeDisposable()
    private val rateDiffPeriods = listOf(TimePeriod.DAY_7, TimePeriod.DAY_30)

    private val rateDiffCoinCodes: List<String> = mutableListOf(service.currency.code).apply {
        if (service.coinType != CoinType.Bitcoin) add("BTC")
        if (service.coinType != CoinType.Ethereum) add("ETH")
    }

    init {
        syncSubtitle()

        service.getCoinDetails(rateDiffCoinCodes, rateDiffPeriods)

        fetchChartInfo()

        service.coinPriceAsync
                .subscribeIO {
                    updateLatestRate(it)
                }
                .let {
                    disposable.add(it)
                }

        service.chartInfoUpdatedObservable
                .subscribeIO {
                    updateChartInfo()
                }
                .let {
                    disposable.add(it)
                }

        service.chartInfoErrorObservable
                .subscribeIO {
                    onChartError(it)
                }
                .let {
                    disposable.add(it)
                }

        service.marketInfoOverviewStateObservable
                .subscribeIO {
                    syncCoinDetailsState(it)
                }
                .let {
                    disposable.add(it)
                }

        service.chartSpinnerObservable
                .subscribeIO {
                    showChartSpinner = true
                    syncChartInfo()
                }
                .let {
                    disposable.add(it)
                }
    }

    private fun updateLatestRate(latestRate: CoinPrice) {
        val currencyValue = CurrencyValue(service.currency, latestRate.value)
        latestRateText = factory.getFormattedLatestRate(currencyValue)
    }

    private fun syncSubtitle() {
        val subtitle = CoinSubtitleAdapter.ViewItemWrapper(latestRateText, rateDiffValue)
        subtitleLiveData.postValue(subtitle)
    }

    fun onSelect(type: ChartView.ChartType) {
        val convertedChartType = getKitChartType(type)

        if (service.chartType == convertedChartType)
            return

        service.chartType = convertedChartType

        fetchChartInfo()
    }

    private fun onChartError(error: Throwable?) {
        showChartError = true
        showChartSpinner = false
        syncChartInfo()
    }

    private fun fetchChartInfo() {
        syncChartInfo()
        service.updateChartInfo()
    }

    private fun syncCoinDetailsState(state: CoinOverviewService.MarketInfoOverviewState) {
        loadingLiveData.postValue(state is CoinOverviewService.MarketInfoOverviewState.Loading)
        if (state is CoinOverviewService.MarketInfoOverviewState.Loaded) {
            updateCoinDetails()
        }

        coinInfoErrorLiveData.postValue(getError(state))
        showFooterLiveData.postValue(state !is CoinOverviewService.MarketInfoOverviewState.Loading)
    }

    private fun getError(state: CoinOverviewService.MarketInfoOverviewState): String {
        if (state !is CoinOverviewService.MarketInfoOverviewState.Error) {
            return ""
        }

        return if (state.error is HttpException && state.error.code() == 404) {
            Translator.getString(R.string.CoinPage_NoData)
        } else {
            Translator.getString(R.string.BalanceSyncError_Title)
        }
    }

    private fun updateCoinDetails() {
        val marketInfoOverview = service.marketInfoOverview ?: return

        roiLiveData.postValue(factory.getRoi(marketInfoOverview.performance))
        categoriesLiveData.postValue(marketInfoOverview.categories.map { it.name })
        contractInfoLiveData.postValue(getContractInfo().map {
            CoinDataItem(
                it.title,
                it.value,
                valueDecorated = true,
                listPosition = ListPosition.Single
            )
        })
        linksLiveData.postValue(factory.getLinks(marketInfoOverview, service.guideUrl))

        val marketData = factory.getMarketData(marketInfoOverview, service.currency, coinCode)
        marketData.apply {
            val items = mutableListOf<CoinDataItem>()
            marketCap?.let {
                items.add(CoinDataItem(Translator.getString(R.string.CoinPage_MarketCap), marketCap, rankLabel = marketCapRank))
            }
            totalSupply?.let {
                items.add(CoinDataItem(Translator.getString(R.string.CoinPage_TotalSupply), totalSupply))
            }
            circulatingSupply?.let {
                items.add(CoinDataItem(Translator.getString(R.string.CoinPage_inCirculation), circulatingSupply))
            }
            volume24h?.let {
                items.add(CoinDataItem(Translator.getString(R.string.CoinPage_TradingVolume), volume24h))
            }
            dilutedMarketCap?.let {
                items.add(CoinDataItem(Translator.getString(R.string.CoinPage_DilutedMarketCap), dilutedMarketCap))
            }
            tvl?.let {
                items.add(CoinDataItem(Translator.getString(R.string.CoinPage_Tvl), tvl))
            }
            genesisDate?.let {
                items.add(CoinDataItem(Translator.getString(R.string.CoinPage_LaunchDate), genesisDate))
            }

            marketDataLiveData.postValue(items)
        }

        if (marketInfoOverview.description.isNotBlank()) {
            aboutTextLiveData.postValue(marketInfoOverview.description)
        }

        showFooterLiveData.postValue(true)
    }

    private fun getContractInfo(): List<ContractInfo> {
        return service.fullCoin.platforms.mapNotNull { platform ->
            when (val coinType = platform.coinType) {
                is CoinType.Erc20 -> ContractInfo(Translator.getString(R.string.CoinPage_Contract, "ETH"), coinType.address)
                is CoinType.Bep20 -> ContractInfo(Translator.getString(R.string.CoinPage_Contract, "BSC"), coinType.address)
                is CoinType.Bep2 -> ContractInfo(Translator.getString(R.string.CoinPage_Bep2Symbol), coinType.symbol)
                else -> null
            }
        }
    }

    private fun updateChartInfo() {
        val info = service.chartInfo ?: return
        showChartSpinner = false
        chartInfoData = factory.createChartInfoData(service.chartType, info, service.lastPoint)
        syncChartInfo()

        rateDiffValue = chartInfoData?.chartData?.diff()
    }

    private fun syncChartInfo() {
        chartInfoLiveData.postValue(CoinChartAdapter.ViewItemWrapper(chartInfoData, showChartSpinner, showChartError))
    }

    private fun getKitChartType(type: ChartView.ChartType): ChartType {
        return when (type) {
            ChartView.ChartType.TODAY -> ChartType.TODAY
            ChartView.ChartType.DAILY -> ChartType.DAILY
            ChartView.ChartType.WEEKLY -> ChartType.WEEKLY
            ChartView.ChartType.WEEKLY2 -> ChartType.WEEKLY2
            ChartView.ChartType.MONTHLY -> ChartType.MONTHLY
            ChartView.ChartType.MONTHLY3 -> ChartType.MONTHLY3
            ChartView.ChartType.MONTHLY6 -> ChartType.MONTHLY6
            ChartView.ChartType.MONTHLY12 -> ChartType.MONTHLY12
            ChartView.ChartType.MONTHLY24 -> ChartType.MONTHLY24
        }
    }

    //  ViewModel

    override fun onCleared() {
        disposable.clear()
        clearables.forEach(Clearable::clear)
    }

}
