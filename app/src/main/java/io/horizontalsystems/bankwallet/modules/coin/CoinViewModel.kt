package io.horizontalsystems.bankwallet.modules.coin

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinChartAdapter
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinSubtitleAdapter
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.xrateskit.entities.*
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal

class CoinViewModel(
        private val service: CoinService,
        val coinCode: String,
        private val coinTitle: String,
        private val factory: CoinViewFactory,
        private val clearables: List<Clearable>
) : ViewModel() {

    val marketSpinner = MutableLiveData<Boolean>()
    val subtitleLiveData = MutableLiveData<CoinSubtitleAdapter.ViewItemWrapper>()
    val chartInfoLiveData = MutableLiveData<CoinChartAdapter.ViewItemWrapper>()
    val roiLiveData = MutableLiveData<List<RoiViewItem>>()
    val marketDataLiveData = MutableLiveData<List<CoinDataItem>>()
    val tvlDataLiveData = MutableLiveData<List<CoinDataItem>>()
    val tradingVolumeLiveData = MutableLiveData<List<CoinDataItem>>()
    val categoriesLiveData = MutableLiveData<String>()
    val contractInfoLiveData = MutableLiveData<List<CoinDataItem>>()
    val aboutTextLiveData = MutableLiveData<AboutText>()
    val linksLiveData =  MutableLiveData<List<CoinLink>>()
    val showFooterLiveData = MutableLiveData(false)

    val alertNotificationUpdated = MutableLiveData<Unit>()
    val showNotificationMenu = SingleLiveEvent<Pair<CoinType, String>>()
    val isFavorite = MutableLiveData<Boolean>()
    val coinMarkets = MutableLiveData<List<MarketTickerViewItem>>()
    val coinInvestors = MutableLiveData<List<InvestorItem>>()

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

    private var ratingValue: String? = null
        set(value) {
            field = value
            syncSubtitle()
        }

    private var chartInfoData: ChartInfoData? = null
    private var showChartSpinner: Boolean = false
    private var showChartError: Boolean = false


    var notificationIconVisible = service.notificationsAreEnabled
    var notificationIconActive = false

    val coinType: CoinType
        get() = service.coinType

    private val disposable = CompositeDisposable()
    private val rateDiffPeriods = listOf(TimePeriod.DAY_7, TimePeriod.DAY_30)

    private val rateDiffCoinCodes: List<String> = mutableListOf(service.currency.code).apply {
        if (service.coinType != CoinType.Bitcoin) add("BTC")
        if (service.coinType != CoinType.Ethereum) add("ETH")
        if (service.coinType != CoinType.BinanceSmartChain) add("BNB")
    }

    init {
        syncSubtitle()

        service.getCoinDetails(rateDiffCoinCodes, rateDiffPeriods)

        fetchChartInfo()

        updateAlertNotificationIconState()

        updateFavoriteNotificationItemState()

        service.latestRateAsync
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

        service.coinDetailsStateObservable
                .subscribeIO {
                    syncCoinDetailsState(it)
                }
                .let {
                    disposable.add(it)
                }

        service.alertNotificationUpdatedObservable
                .subscribeIO {
                    updateAlertNotificationIconState()
                }
                .let {
                    disposable.add(it)
                }
    }

    private fun updateLatestRate(latestRate: LatestRate) {
        val currencyValue = CurrencyValue(service.currency, latestRate.rate)
        latestRateText = factory.getFormattedLatestRate(currencyValue)
    }

    private fun syncSubtitle() {
        val subtitle = CoinSubtitleAdapter.ViewItemWrapper(
                coinTitle,
                coinType,
                ratingValue,
                latestRateText,
                rateDiffValue,
                null
        )
        subtitleLiveData.postValue(subtitle)
    }

    fun onSelect(type: ChartView.ChartType) {
        val convertedChartType = getKitChartType(type)

        if (service.chartType == convertedChartType)
            return

        service.chartType = convertedChartType

        fetchChartInfo()
    }

    fun onNotificationClick() {
        showNotificationMenu.postValue(Pair(service.coinType, coinTitle))
    }

    fun onFavoriteClick() {
        service.favorite()
        updateFavoriteNotificationItemState()
    }

    fun onUnfavoriteClick() {
        service.unfavorite()
        updateFavoriteNotificationItemState()
    }

    private fun onChartError(error: Throwable?) {
        showChartError = true
        syncChartInfo()
    }

    private fun fetchChartInfo() {
        showChartSpinner = true
        syncChartInfo()
        service.updateChartInfo()
    }

    private fun syncCoinDetailsState(state: CoinService.CoinDetailsState) {
        marketSpinner.postValue(state is CoinService.CoinDetailsState.Loading)
        if (state is CoinService.CoinDetailsState.Loaded) {
            updateCoinDetails()
        }
    }

    private fun updateAlertNotificationIconState() {
        notificationIconActive = service.hasPriceAlert
        alertNotificationUpdated.postValue(Unit)
    }

    private fun updateFavoriteNotificationItemState() {
        isFavorite.postValue(service.isCoinFavorite())
    }

    private fun updateCoinDetails() {
        val coinDetails = service.coinMarketDetails ?: return
        coinDetails.meta.rating?.let { ratingValue = it }

        roiLiveData.postValue(factory.getRoi(coinDetails.rateDiffs, rateDiffCoinCodes, rateDiffPeriods))

        marketDataLiveData.postValue(factory.getMarketData(coinDetails, service.currency, coinCode))

        tvlDataLiveData.postValue(factory.getTvlInfo(coinDetails, service.currency))

        tradingVolumeLiveData.postValue(factory.getTradingVolume(coinDetails, service.currency))

        categoriesLiveData.postValue(coinDetails.meta.categories.joinToString(", ") { it.name })

        getContractInfo(coinDetails)?.let {
            contractInfoLiveData.postValue(listOf(CoinDataItem(it.title, it.value, valueDecorated = true, listPosition = ListPosition.Single)))
        }

        if (coinDetails.meta.description.isNotBlank()){
            aboutTextLiveData.postValue(AboutText(coinDetails.meta.description, coinDetails.meta.descriptionType))
        }

        linksLiveData.postValue(factory.getLinks(coinDetails, service.guideUrl))

        showFooterLiveData.postValue(true)

        coinMarkets.postValue(factory.getCoinMarketItems(coinDetails.tickers))
        coinInvestors.postValue(factory.getCoinInvestorItems(coinDetails.meta.fundCategories))
    }

    private fun getContractInfo(coinDetails: CoinMarketDetails): ContractInfo? =
            when (val coinType = coinDetails.data.type) {
                is CoinType.Erc20 -> ContractInfo(Translator.getString(R.string.CoinPage_Contract, "ETH"), coinType.address)
                is CoinType.Bep20 -> ContractInfo(Translator.getString(R.string.CoinPage_Contract, "BSC"), coinType.address)
                is CoinType.Bep2 -> ContractInfo(Translator.getString(R.string.CoinPage_Bep2Symbol), coinType.symbol)
                else -> null
            }

    private fun updateChartInfo() {
        val info = service.chartInfo ?: return
        showChartSpinner = false
        chartInfoData = factory.createChartInfoData(service.chartType, info, service.lastPoint)
        syncChartInfo()

        rateDiffValue = chartInfoData?.chartData?.diff()
    }

    private fun syncChartInfo() {
        chartInfoLiveData.postValue(
                CoinChartAdapter.ViewItemWrapper(
                        chartInfoData,
                        showChartSpinner,
                        showChartError
                )
        )
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
