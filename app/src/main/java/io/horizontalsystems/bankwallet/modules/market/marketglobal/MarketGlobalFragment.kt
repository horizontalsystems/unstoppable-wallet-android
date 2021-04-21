package io.horizontalsystems.bankwallet.modules.market.marketglobal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_market_global.*

class MarketGlobalFragment : BaseFragment(), MarketGlobalChartAdapter.Listener {

    private val metricsType by lazy {
        requireArguments().getParcelable(METRICS_TYPE_KEY) ?: MetricsType.BtcDominance
    }

    private var canVerticallyScroll = true

    private val viewModel by viewModels<MarketGlobalViewModel> { MarketGlobalModule.Factory(metricsType) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_market_global, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setTitle(viewModel.title)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val chartAdapter = MarketGlobalChartAdapter(this, viewModel.chartType)

        metricsRecyclerView.itemAnimator = null
        metricsRecyclerView.adapter = ConcatAdapter(chartAdapter)

        metricsRecyclerView.layoutManager = object : LinearLayoutManager(context) {
            override fun canScrollVertically(): Boolean = canVerticallyScroll
        }

        viewModel.chartViewItem.observe(viewLifecycleOwner, {
            chartAdapter.setChartViewItem(it)
        })

    }

    override fun onChartTouchDown() {
        canVerticallyScroll = false
    }

    override fun onChartTouchUp() {
        canVerticallyScroll = true
    }

    override fun onTabSelected(chartType: ChartView.ChartType) {
        viewModel.onSelect(chartType)
    }

    companion object {
        private const val METRICS_TYPE_KEY = "metrics_type"

        fun prepareParams(metricsType: MetricsType): Bundle {
            return bundleOf(
                    METRICS_TYPE_KEY to metricsType
            )
        }

    }
}
