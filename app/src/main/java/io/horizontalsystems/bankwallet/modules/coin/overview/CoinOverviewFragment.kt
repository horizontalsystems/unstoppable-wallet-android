package io.horizontalsystems.bankwallet.modules.coin.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.CoinLink
import io.horizontalsystems.bankwallet.modules.coin.CoinViewModel
import io.horizontalsystems.bankwallet.modules.markdown.MarkdownFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.LinkType

class CoinOverviewFragment : BaseFragment() {

    private val vmFactory by lazy { CoinOverviewModule.Factory(coinViewModel.fullCoin) }

    private val coinViewModel by navGraphViewModels<CoinViewModel>(R.id.coinFragment)
    private val viewModel by navGraphViewModels<CoinOverviewViewModel>(R.id.coinFragment) { vmFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?, ): View {
        return ComposeView(requireContext()).apply {
            // Dispose the Composition when the view's LifecycleOwner
            // is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ComposeAppTheme {
                    val title by viewModel.titleLiveData.observeAsState(TitleViewItem(null, null))
                    val marketData by viewModel.marketDataLiveData.observeAsState(listOf())
                    val roi by viewModel.roiLiveData.observeAsState(listOf())
                    val categories by viewModel.categoriesLiveData.observeAsState(listOf())
                    val contractInfo by viewModel.contractInfoLiveData.observeAsState(listOf())
                    val aboutText by viewModel.aboutTextLiveData.observeAsState("")
                    val links by viewModel.linksLiveData.observeAsState(listOf())
                    val showFooter by viewModel.showFooterLiveData.observeAsState(false)
                    val loading by viewModel.loadingLiveData.observeAsState(false)
                    val coinInfoError by viewModel.coinInfoErrorLiveData.observeAsState("")
                    val chartInfo by viewModel.chartInfoLiveData.observeAsState()

                    CoinOverviewScreen(
                        title,
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
                        coinInfoError,
                        chartInfo,
                        viewModel.currency
                    ) {
                        viewModel.onSelect(it)
                    }
                }
            }
        }
    }

    private fun onClick(coinLink: CoinLink) {
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
