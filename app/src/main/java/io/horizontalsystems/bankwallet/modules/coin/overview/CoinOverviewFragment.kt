package io.horizontalsystems.bankwallet.modules.coin.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.CoinLink
import io.horizontalsystems.bankwallet.modules.coin.CoinViewModel
import io.horizontalsystems.bankwallet.modules.markdown.MarkdownFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.LinkType

class CoinOverviewFragment : BaseFragment() {

    private val vmFactory by lazy { CoinOverviewModule.Factory(coinViewModel.fullCoin) }

    private val coinViewModel by navGraphViewModels<CoinViewModel>(R.id.coinFragment)
    private val viewModel by viewModels<CoinOverviewViewModel> { vmFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?, ): View {
        return ComposeView(requireContext()).apply {
            // Dispose the Composition when the view's LifecycleOwner
            // is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ComposeAppTheme {
                    CoinOverviewScreen(
                        viewModel,
                        {
                            TextHelper.copyText(it.rawValue)
                            HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Copied)
                        },
                        {
                            LinkHelper.openLinkInAppBrowser(requireContext(), it.explorerUrl)
                        }
                    ) {
                        onClick(it)
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
