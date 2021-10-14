package io.horizontalsystems.bankwallet.modules.coin.overview

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.CoinLink
import io.horizontalsystems.bankwallet.modules.coin.CoinViewModel
import io.horizontalsystems.bankwallet.modules.coin.adapters.*
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinChartAdapter.ChartViewType
import io.horizontalsystems.bankwallet.modules.markdown.MarkdownFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.LinkType
import kotlinx.android.synthetic.main.fragment_coin_overview.*

class CoinOverviewFragment : BaseFragment(R.layout.fragment_coin_overview), CoinChartAdapter.Listener, CoinLinksAdapter.Listener {

    private val vmFactory by lazy { CoinOverviewModule.Factory(coinViewModel.fullCoin) }

    private val coinViewModel by navGraphViewModels<CoinViewModel>(R.id.coinFragment)
    private val viewModel by navGraphViewModels<CoinOverviewViewModel>(R.id.coinFragment) { vmFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        compose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        compose.setContent {
            ComposeAppTheme {
                val subtitle by viewModel.subtitleLiveData.observeAsState(CoinSubtitleAdapter.ViewItemWrapper(null, null))
                val marketData by viewModel.marketDataLiveData.observeAsState(listOf())
                val roi by viewModel.roiLiveData.observeAsState(listOf())
                val categories by viewModel.categoriesLiveData.observeAsState(listOf())
                val contractInfo by viewModel.contractInfoLiveData.observeAsState(listOf())
                val aboutText by viewModel.aboutTextLiveData.observeAsState("")
                val links by viewModel.linksLiveData.observeAsState(listOf())
                val showFooter by viewModel.showFooterLiveData.observeAsState(false)
                val loading by viewModel.loadingLiveData.observeAsState(false)
                val coinInfoError by viewModel.coinInfoErrorLiveData.observeAsState("")

                CoinOverviewScreen(subtitle,
                    marketData,
                    roi,
                    categories,
                    contractInfo,
                    aboutText,
                    links,
                    {
                        onClick(it)
                    },
                    showFooter,
                    loading,
                    coinInfoError)
            }
        }

        val chartAdapter = CoinChartAdapter(
            viewModel.chartInfoLiveData,
            viewModel.currency,
            ChartViewType.CoinChart,
            this,
            viewLifecycleOwner
        )
        val aboutAdapter = CoinAboutAdapter(viewModel.aboutTextLiveData, viewLifecycleOwner)

        val concatAdapter = ConcatAdapter(
                chartAdapter,
                aboutAdapter,
        )

//        controlledRecyclerView.adapter = concatAdapter
        controlledRecyclerView.isVisible = false

        activity?.onBackPressedDispatcher?.addCallback(this) {
            findNavController().popBackStack()
        }

    }

    //  CoinChartAdapter Listener

    override fun onChartTouchDown() {
        controlledRecyclerView.enableVerticalScroll(false)
    }

    override fun onChartTouchUp() {
        controlledRecyclerView.enableVerticalScroll(true)
    }

    override fun onTabSelect(chartType: ChartView.ChartType) {
        viewModel.onSelect(chartType)
    }

    //  CoinLinksAdapter Listener

    override fun onClick(coinLink: CoinLink) {
        val absoluteUrl = getAbsoluteUrl(coinLink)

        when (coinLink.linkType) {
            LinkType.Guide -> {
                val arguments = bundleOf(
                    MarkdownFragment.markdownUrlKey to absoluteUrl,
                    MarkdownFragment.handleRelativeUrlKey to true
                )
                findNavController().navigate(R.id.coinFragment_to_markdownFragment,
                    arguments,
                    navOptions())
            }
            else -> LinkHelper.openLinkInAppBrowser(requireContext(), absoluteUrl)
        }
    }

    private fun getAbsoluteUrl(coinLink: CoinLink) = when (coinLink.linkType) {
        LinkType.Twitter -> "https://twitter.com/${coinLink.url}"
        LinkType.Telegram -> "https://t.me/${coinLink.url}"
        else -> coinLink.url
    }

}
