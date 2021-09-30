package io.horizontalsystems.bankwallet.modules.market.metricspage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinChartAdapter
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinChartAdapter.ChartViewType
import io.horizontalsystems.bankwallet.modules.market.MarketItemsAdapter
import io.horizontalsystems.bankwallet.modules.market.MarketLoadingAdapter
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.ViewHolderMarketItem
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_metrics_page.*

class MetricsPageFragment : BaseFragment(), CoinChartAdapter.Listener,
    ViewHolderMarketItem.Listener,
    MetricsPageListHeaderAdapter.Listener {

    private val metricsType by lazy {
        requireArguments().getParcelable<MetricsType>(METRICS_TYPE_KEY)
    }

    private val vmFactory by lazy { MetricsPageModule.Factory(metricsType!!) }
    private val metricsViewModel by viewModels<MetricsPageViewModel> { vmFactory }
    private val metricsListViewModel by viewModels<MetricsPageListViewModel> { vmFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_metrics_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setTitle(metricsType?.title!!)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuClose -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        val subtitleAdapter =
            MetricsPageSubtitleAdapter(metricsViewModel.subtitleLiveData, viewLifecycleOwner)
        val chartAdapter = CoinChartAdapter(
            metricsViewModel.chartInfoLiveData,
            metricsViewModel.currency,
            ChartViewType.MarketMetricChart,
            this,
            viewLifecycleOwner
        )
        val listHeaderAdapter = MetricsPageListHeaderAdapter(
            metricsListViewModel.listHeaderMenu,
            viewLifecycleOwner,
            this
        )
        val marketItemsAdapter = MarketItemsAdapter(
            this,
            metricsListViewModel.marketViewItemsLiveData,
            metricsListViewModel.loadingLiveData,
            metricsListViewModel.errorLiveData,
            viewLifecycleOwner
        )
        val marketLoadingAdapter = MarketLoadingAdapter(
            metricsListViewModel.loadingLiveData,
            metricsListViewModel.errorLiveData,
            metricsListViewModel::onErrorClick,
            viewLifecycleOwner
        )

        val concatAdapter = ConcatAdapter(
            subtitleAdapter,
            chartAdapter,
            listHeaderAdapter,
            marketItemsAdapter,
            marketLoadingAdapter
        )

        controlledRecyclerView.adapter = concatAdapter

        observeData()

        activity?.onBackPressedDispatcher?.addCallback(this) {
            findNavController().popBackStack()
        }

    }

    //ViewHolderMarketItem.Listener

    override fun onItemClick(marketViewItem: MarketViewItem) {
        val arguments = CoinFragment.prepareParams(
            marketViewItem.coinUid,
            marketViewItem.coinCode,
            marketViewItem.coinName
        )

        findNavController().navigate(R.id.coinFragment, arguments, navOptions())
    }

    //MetricsPageListHeaderAdapter.Listener

    override fun onSortingClick() {
        metricsListViewModel.onChangeSorting()
    }

    override fun onToggleButtonClick() {
        metricsListViewModel.onToggleButtonClick()
    }

    //  CoinChartAdapter Listener

    override fun onChartTouchDown() {
        controlledRecyclerView.enableVerticalScroll(false)
    }

    override fun onChartTouchUp() {
        controlledRecyclerView.enableVerticalScroll(true)
    }

    override fun onTabSelect(chartType: ChartView.ChartType) {
        metricsViewModel.onChartTypeSelect(chartType)
    }

    //  Private

    private fun observeData() {
        metricsListViewModel.networkNotAvailable.observe(viewLifecycleOwner, {
            HudHelper.showErrorMessage(requireView(), R.string.Hud_Text_NoInternet)
        })
    }

    companion object {
        private const val METRICS_TYPE_KEY = "metric_type"

        fun prepareParams(metricType: MetricsType): Bundle {
            return bundleOf(METRICS_TYPE_KEY to metricType)
        }
    }
}
