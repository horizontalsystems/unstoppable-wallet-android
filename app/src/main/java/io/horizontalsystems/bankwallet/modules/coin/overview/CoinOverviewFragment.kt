package io.horizontalsystems.bankwallet.modules.coin.overview

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.CoinLink
import io.horizontalsystems.bankwallet.modules.coin.CoinViewModel
import io.horizontalsystems.bankwallet.modules.coin.PoweredByAdapter
import io.horizontalsystems.bankwallet.modules.coin.adapters.*
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinChartAdapter.ChartViewType
import io.horizontalsystems.bankwallet.modules.markdown.MarkdownFragment
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

        val subtitleAdapter = CoinSubtitleAdapter(viewModel.subtitleLiveData, viewLifecycleOwner)
        val chartAdapter = CoinChartAdapter(
            viewModel.chartInfoLiveData,
            viewModel.currency,
            ChartViewType.CoinChart,
            this,
            viewLifecycleOwner
        )
        val coinRoiAdapter = CoinRoiAdapter(viewModel.roiLiveData, viewLifecycleOwner)
        val marketDataAdapter = CoinDataAdapter(viewModel.marketDataLiveData, viewLifecycleOwner)
        val investorDataAdapter = CoinDataAdapter(viewModel.investorDataLiveData, viewLifecycleOwner, R.string.CoinPage_InvestorData)
        val securityParamsAdapter = CoinDataAdapter(viewModel.securityParamsLiveData, viewLifecycleOwner, R.string.CoinPage_SecurityParams)
        val categoriesAdapter = CoinCategoryAdapter(viewModel.categoriesLiveData, viewLifecycleOwner)
        val contractInfoAdapter = CoinDataAdapter(viewModel.contractInfoLiveData, viewLifecycleOwner)
        val aboutAdapter = CoinAboutAdapter(viewModel.aboutTextLiveData, viewLifecycleOwner)
        val linksAdapter = CoinLinksAdapter(viewModel.linksLiveData, viewLifecycleOwner, this)
        val footerAdapter = PoweredByAdapter(viewModel.showFooterLiveData, viewLifecycleOwner, getString(R.string.Market_PoweredByApi))

        val loadingAdapter = CoinLoadingAdapter(viewModel.loadingLiveData, viewLifecycleOwner)
        val errorAdapter = CoinInfoErrorAdapter(viewModel.coinInfoErrorLiveData, viewLifecycleOwner)

        val concatAdapter = ConcatAdapter(
                subtitleAdapter,
                chartAdapter,
                marketDataAdapter,
                coinRoiAdapter,
                investorDataAdapter,
                securityParamsAdapter,
                categoriesAdapter,
                contractInfoAdapter,
                aboutAdapter,
                linksAdapter,
                loadingAdapter,
                errorAdapter,
                footerAdapter
        )

        controlledRecyclerView.adapter = concatAdapter

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
