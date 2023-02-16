package cash.p.terminal.modules.coin.overview.ui

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.iconPlaceholder
import cash.p.terminal.core.iconUrl
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.chart.ChartViewModel
import cash.p.terminal.modules.coin.CoinLink
import cash.p.terminal.modules.coin.overview.CoinOverviewModule
import cash.p.terminal.modules.coin.overview.CoinOverviewViewModel
import cash.p.terminal.modules.coin.ui.CoinScreenTitle
import cash.p.terminal.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import cash.p.terminal.modules.enablecoin.restoresettings.ZCashConfig
import cash.p.terminal.modules.managewallets.ManageWalletsModule
import cash.p.terminal.modules.managewallets.ManageWalletsViewModel
import cash.p.terminal.modules.markdown.MarkdownFragment
import cash.p.terminal.modules.zcashconfigure.ZcashConfigure
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.HSSwipeRefresh
import cash.p.terminal.ui.compose.components.CellFooter
import cash.p.terminal.ui.compose.components.ListErrorView
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.helpers.LinkHelper
import io.horizontalsystems.core.getNavigationResult
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.LinkType

@Composable
fun CoinOverviewScreen(
    fullCoin: FullCoin,
    navController: NavController
) {
    val vmFactory by lazy { CoinOverviewModule.Factory(fullCoin) }
    val viewModel = viewModel<CoinOverviewViewModel>(factory = vmFactory)
    val chartViewModel = viewModel<ChartViewModel>(factory = vmFactory)

    val refreshing by viewModel.isRefreshingLiveData.observeAsState(false)
    val overview by viewModel.overviewLiveData.observeAsState()
    val viewState by viewModel.viewStateLiveData.observeAsState()

    val view = LocalView.current
    val context = LocalContext.current

//                when (viewModel.coinState) {
//                    null,
//                    CoinState.Unsupported,
//                    CoinState.NoActiveAccount,
//                    CoinState.WatchAccount -> {
//                    }
//                    CoinState.AddedToWallet,
//                    CoinState.InWallet -> {
//                        add(
//                            MenuItem(
//                                title = TranslatableString.ResString(R.string.CoinPage_InWallet),
//                                icon = R.drawable.ic_in_wallet_dark_24,
//                                onClick = { HudHelper.showInProcessMessage(view, R.string.Hud_Already_In_Wallet, showProgressBar = false) }
//                            )
//                        )
//                    }
//                    CoinState.NotInWallet -> {
//                        add(
//                            MenuItem(
//                                title = TranslatableString.ResString(R.string.CoinPage_AddToWallet),
//                                icon = R.drawable.ic_add_to_wallet_2_24,
//                                onClick = {
//                                    manageWalletsViewModel.enable(TODO())
//                                }
//                            )
//                        )
//                    }
//                }

    val vmFactory1 = remember { ManageWalletsModule.Factory() }
    val manageWalletsViewModel = viewModel<ManageWalletsViewModel>(factory = vmFactory1)
    val restoreSettingsViewModel = viewModel<RestoreSettingsViewModel>(factory = vmFactory1)

    if (restoreSettingsViewModel.openZcashConfigure != null) {
        restoreSettingsViewModel.zcashConfigureOpened()

        navController.getNavigationResult(ZcashConfigure.resultBundleKey) { bundle ->
            val requestResult = bundle.getInt(ZcashConfigure.requestResultKey)

            if (requestResult == ZcashConfigure.RESULT_OK) {
                val zcashConfig = bundle.getParcelable<ZCashConfig>(ZcashConfigure.zcashConfigKey)
                zcashConfig?.let { config ->
                    restoreSettingsViewModel.onEnter(config)
                }
            } else {
                restoreSettingsViewModel.onCancelEnterBirthdayHeight()
            }
        }

        navController.slideFromBottom(R.id.zcashConfigure)
    }


    HSSwipeRefresh(
        refreshing = refreshing,
        onRefresh = {
            viewModel.refresh()
            chartViewModel.refresh()
        },
        content = {
            Crossfade(viewState) { viewState ->
                when (viewState) {
                    ViewState.Loading -> {
                        Loading()
                    }
                    ViewState.Success -> {
                        overview?.let { overview ->
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                CoinScreenTitle(
                                    fullCoin.coin.name,
                                    fullCoin.coin.marketCapRank,
                                    fullCoin.coin.iconUrl,
                                    fullCoin.iconPlaceholder
                                )

                                Chart(chartViewModel = chartViewModel)

                                if (overview.marketData.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    MarketData(overview.marketData)
                                }

                                if (overview.roi.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Roi(overview.roi)
                                }

                                if (overview.categories.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Categories(overview.categories)
                                }

                                if (viewModel.xxxTokens.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Tokens(
                                        tokens = viewModel.xxxTokens,
                                        onClickAddToWallet = {
                                            manageWalletsViewModel.enable(it)
                                        },
                                        onClickExplorer = {
                                            LinkHelper.openLinkInAppBrowser(context, it)
                                        },
                                    )
                                }

                                if (overview.about.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    About(overview.about)
                                }

                                if (overview.links.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Links(overview.links) { onClick(it, context, navController) }
                                }

                                Spacer(modifier = Modifier.height(32.dp))
                                CellFooter(text = stringResource(id = R.string.Market_PoweredByApi))
                            }
                        }

                    }
                    is ViewState.Error -> {
                        ListErrorView(stringResource(id = R.string.BalanceSyncError_Title)) {
                            viewModel.retry()
                            chartViewModel.refresh()
                        }
                    }
                    null -> {}
                }
            }
        },
    )
}

private fun onClick(coinLink: CoinLink, context: Context, navController: NavController) {
    val absoluteUrl = getAbsoluteUrl(coinLink)

    when (coinLink.linkType) {
        LinkType.Guide -> {
            val arguments = bundleOf(
                MarkdownFragment.markdownUrlKey to absoluteUrl,
                MarkdownFragment.handleRelativeUrlKey to true
            )
            navController.slideFromRight(
                R.id.markdownFragment,
                arguments
            )
        }
        else -> LinkHelper.openLinkInAppBrowser(context, absoluteUrl)
    }
}

private fun getAbsoluteUrl(coinLink: CoinLink) = when (coinLink.linkType) {
    LinkType.Twitter -> "https://twitter.com/${coinLink.url}"
    LinkType.Telegram -> "https://t.me/${coinLink.url}"
    else -> coinLink.url
}

@Preview
@Composable
fun LoadingPreview() {
    ComposeAppTheme {
        Loading()
    }
}

@Composable
fun Error(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        subhead2_grey(text = message)
    }
}

@Composable
fun Loading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = ComposeAppTheme.colors.grey,
            strokeWidth = 2.dp
        )
    }
}
