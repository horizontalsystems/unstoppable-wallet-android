package io.horizontalsystems.walletkit.modules.coin.overview.ui

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.LinkType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.alternativeImageUrl
import io.horizontalsystems.walletkit.core.iconPlaceholder
import io.horizontalsystems.walletkit.core.imageUrl
import io.horizontalsystems.walletkit.core.stats.StatEntity
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.entities.ViewState
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.chart.ChartViewModel
import io.horizontalsystems.walletkit.modules.coin.CoinLink
import io.horizontalsystems.walletkit.modules.coin.indicators.IndicatorsPage
import io.horizontalsystems.walletkit.modules.coin.overview.CoinOverviewModule
import io.horizontalsystems.walletkit.modules.coin.overview.CoinOverviewViewModel
import io.horizontalsystems.walletkit.modules.coin.overview.HudMessageType
import io.horizontalsystems.walletkit.modules.coin.ui.CoinScreenTitle
import io.horizontalsystems.walletkit.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import io.horizontalsystems.walletkit.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.walletkit.modules.managewallets.ManageWalletsModule
import io.horizontalsystems.walletkit.modules.managewallets.ManageWalletsViewModel
import io.horizontalsystems.walletkit.modules.markdown.MarkdownPage
import io.horizontalsystems.walletkit.modules.multiswap.SwapPage
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.restoreconfig.BirthdayHeightConfigPage
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.HSSwipeRefresh
import io.horizontalsystems.walletkit.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.walletkit.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.walletkit.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.walletkit.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.walletkit.ui.compose.components.CellFooter
import io.horizontalsystems.walletkit.ui.compose.components.CellSingleLineClear
import io.horizontalsystems.walletkit.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.walletkit.ui.compose.components.HSpacer
import io.horizontalsystems.walletkit.ui.compose.components.ListErrorView
import io.horizontalsystems.walletkit.ui.compose.components.RowUniversal
import io.horizontalsystems.walletkit.ui.compose.components.body_leah
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_grey
import io.horizontalsystems.walletkit.ui.helpers.LinkHelper
import io.horizontalsystems.walletkit.ui.helpers.TextHelper

@Composable
fun CoinOverviewScreen(
    fullCoin: FullCoin,
    navigation: HSNavigation,
    coinToken: Token?,
    popularToken: Token?
) {
    val vmFactory by lazy { CoinOverviewModule.Factory(fullCoin) }
    val viewModel = viewModel<CoinOverviewViewModel>(factory = vmFactory)
    val chartViewModel = viewModel<ChartViewModel>(factory = vmFactory)

    val refreshing by viewModel.isRefreshingLiveData.observeAsState(false)
    val overview by viewModel.overviewLiveData.observeAsState()
    val viewState by viewModel.viewStateLiveData.observeAsState()
    val chartIndicatorsState = viewModel.chartIndicatorsState

    val view = LocalView.current
    val context = LocalContext.current

    viewModel.showHudMessage?.let {
        when (it.type) {
            HudMessageType.Error -> HudHelper.showErrorMessage(
                contenView = view,
                resId = it.text,
                icon = it.iconRes,
                iconTint = R.color.white
            )

            HudMessageType.Success -> HudHelper.showSuccessMessage(
                contenView = view,
                resId = it.text,
                icon = it.iconRes,
                iconTint = R.color.white
            )
        }

        viewModel.onHudMessageShown()
    }

    val vmFactory1 = remember { ManageWalletsModule.Factory() }
    val manageWalletsViewModel = viewModel<ManageWalletsViewModel>(factory = vmFactory1)
    val restoreSettingsViewModel = viewModel<RestoreSettingsViewModel>(factory = vmFactory1)

    restoreSettingsViewModel.openBirthdayHeightConfig?.let { token ->
        restoreSettingsViewModel.birthdayHeightConfigOpened()

        val forResult = navigation.slideFromRightForResult<BirthdayHeightConfigPage.Result>(
            { BirthdayHeightConfigPage(token.blockchainType) }
        ) {
            if (it.config != null) {
                restoreSettingsViewModel.onEnter(it.config)
            } else {
                restoreSettingsViewModel.onCancelEnterBirthdayHeight()
            }
        }
        forResult()
    }


    HSSwipeRefresh(
        refreshing = refreshing,
        onRefresh = {
            viewModel.refresh()
            chartViewModel.refresh()
        },
        content = {
            Crossfade(viewState, label = "") { viewState ->
                when (viewState) {
                    ViewState.Loading -> {
                        Loading()
                    }

                    ViewState.Success -> {
                        overview?.let { overview ->
                            Column {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    CoinScreenTitle(
                                        fullCoin.coin.name,
                                        overview.marketCapRank,
                                        fullCoin.coin.imageUrl,
                                        fullCoin.coin.alternativeImageUrl,
                                        fullCoin.iconPlaceholder
                                    )

                                    Chart(chartViewModel = chartViewModel)

                                    Spacer(modifier = Modifier.height(12.dp))

                                    CellUniversalLawrenceSection {
                                        RowUniversal(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp),
                                        ) {
                                            subhead2_grey(text = stringResource(R.string.CoinPage_Indicators))
                                            Spacer(modifier = Modifier.weight(1f))

                                            if (chartIndicatorsState.hasActiveSubscription) {
                                                if (chartIndicatorsState.enabled) {
                                                    ButtonSecondaryDefault(
                                                        modifier = Modifier.height(28.dp),
                                                        title = stringResource(id = R.string.Button_Hide),
                                                        onClick = {
                                                            viewModel.disableChartIndicators()

                                                            stat(
                                                                page = StatPage.CoinOverview,
                                                                event = StatEvent.ToggleIndicators(
                                                                    false
                                                                )
                                                            )
                                                        }
                                                    )
                                                } else {
                                                    ButtonSecondaryDefault(
                                                        modifier = Modifier.height(28.dp),
                                                        title = stringResource(id = R.string.Button_Show),
                                                        onClick = {
                                                            viewModel.enableChartIndicators()

                                                            stat(
                                                                page = StatPage.CoinOverview,
                                                                event = StatEvent.ToggleIndicators(
                                                                    true
                                                                )
                                                            )
                                                        }
                                                    )
                                                }
                                                HSpacer(width = 8.dp)
                                                ButtonSecondaryCircle(
                                                    modifier = Modifier.height(28.dp),
                                                    icon = R.drawable.ic_setting_20
                                                ) {
                                                    navigation.slideFromRight(IndicatorsPage)

                                                    stat(
                                                        page = StatPage.CoinOverview,
                                                        event = StatEvent.Open(StatPage.Indicators)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (overview.marketData.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        MarketData(overview.marketData)
                                    }

                                    if (overview.roi.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(24.dp))
                                        CellSingleLineClear(borderTop = true) {
                                            body_leah(
                                                text = stringResource(
                                                    R.string.CoinPage_ROI_Title,
                                                    viewModel.fullCoin.coin.code
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Roi(overview.roi, navigation)
                                    }

                                    viewModel.tokenVariants?.let { tokenVariants ->
                                        Spacer(modifier = Modifier.height(24.dp))
                                        TokenVariants(
                                            tokenVariants = tokenVariants,
                                            // The page is browsable while locked; changing the
                                            // wallet is not.
                                            onClickAddToWallet = { token ->
                                                App.lockGate.requireUnlocked {
                                                    manageWalletsViewModel.enable(token)

                                                    stat(
                                                        page = StatPage.CoinOverview,
                                                        event = StatEvent.AddToWallet
                                                    )
                                                }
                                            },
                                            onClickRemoveWallet = { token ->
                                                App.lockGate.requireUnlocked {
                                                    manageWalletsViewModel.disable(token)

                                                    stat(
                                                        page = StatPage.CoinOverview,
                                                        event = StatEvent.RemoveFromWallet
                                                    )
                                                }
                                            },
                                            onClickCopy = {
                                                TextHelper.copyText(it)
                                                HudHelper.showSuccessMessage(
                                                    view,
                                                    R.string.Hud_Text_Copied
                                                )

                                                stat(
                                                    page = StatPage.CoinOverview,
                                                    event = StatEvent.Copy(StatEntity.ContractAddress)
                                                )
                                            },
                                            onClickExplorer = {
                                                LinkHelper.openLinkInAppBrowser(context, it)

                                                stat(
                                                    page = StatPage.CoinOverview,
                                                    event = StatEvent.Open(StatPage.ExternalBlockExplorer)
                                                )
                                            },
                                        )
                                    }

                                    if (overview.about.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(24.dp))
                                        About(overview.about)
                                    }

                                    if (overview.links.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Links(overview.links) { onClick(it, context, navigation) }
                                    }

                                    Spacer(modifier = Modifier.height(32.dp))
                                    CellFooter(text = stringResource(id = R.string.Market_PoweredByApi))
                                }
                                coinToken?.let {
                                    CoinBottomButtons(coinToken, popularToken, navigation)
                                }
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

private fun onClick(coinLink: CoinLink, context: Context, navigation: HSNavigation) {
    val absoluteUrl = getAbsoluteUrl(coinLink)

    when (coinLink.linkType) {
        LinkType.Guide -> {
            navigation.slideFromRight(
                MarkdownPage(MarkdownPage.Input(absoluteUrl, true))
            )
        }

        else -> LinkHelper.openLinkInAppBrowser(context, absoluteUrl)
    }

    when (coinLink.linkType) {
        LinkType.Guide -> stat(page = StatPage.CoinOverview, event = StatEvent.Open(StatPage.Guide))
        LinkType.Website -> stat(
            page = StatPage.CoinOverview,
            event = StatEvent.Open(StatPage.ExternalCoinWebsite)
        )

        LinkType.Whitepaper -> stat(
            page = StatPage.CoinOverview,
            event = StatEvent.Open(StatPage.ExternalCoinWhitePaper)
        )

        LinkType.Twitter -> stat(
            page = StatPage.CoinOverview,
            event = StatEvent.Open(StatPage.ExternalTwitter)
        )

        LinkType.Telegram -> stat(
            page = StatPage.CoinOverview,
            event = StatEvent.Open(StatPage.ExternalTelegram)
        )

        LinkType.Reddit -> stat(
            page = StatPage.CoinOverview,
            event = StatEvent.Open(StatPage.ExternalReddit)
        )

        LinkType.Github -> stat(
            page = StatPage.CoinOverview,
            event = StatEvent.Open(StatPage.ExternalGithub)
        )
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

@Composable
private fun CoinBottomButtons(
    coinToken: Token,
    popularToken: Token?,
    navigation: HSNavigation
) {
    ButtonsGroupWithShade {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            ButtonPrimaryYellow(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.CoinPage_Buy),
                onClick = {
                    navigation.slideFromRight(
                        SwapPage(
                            SwapPage.Input(
                                tokenIn = popularToken,
                                tokenOut = coinToken
                            )
                        )
                    )
                }
            )

            HSpacer(8.dp)

            ButtonPrimaryDefault(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.CoinPage_Sell),
                onClick = {
                    navigation.slideFromRight(
                        SwapPage(
                            SwapPage.Input(
                                tokenIn = coinToken,
                                tokenOut = popularToken
                            )
                        )
                    )
                }
            )
        }
    }
}