package io.horizontalsystems.bankwallet.modules.coin.analytics

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatPremiumTrigger
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule.AnalyticsViewItem
import io.horizontalsystems.bankwallet.modules.coin.analytics.ui.AnalyticsBlockHeader
import io.horizontalsystems.bankwallet.modules.coin.analytics.ui.AnalyticsChart
import io.horizontalsystems.bankwallet.modules.coin.analytics.ui.AnalyticsContainer
import io.horizontalsystems.bankwallet.modules.coin.analytics.ui.AnalyticsContentNumber
import io.horizontalsystems.bankwallet.modules.coin.analytics.ui.AnalyticsFooterCell
import io.horizontalsystems.bankwallet.modules.coin.analytics.ui.TechnicalAdviceBlock
import io.horizontalsystems.bankwallet.modules.coin.audits.CoinAuditsScreen
import io.horizontalsystems.bankwallet.modules.coin.detectors.DetectorsScreen
import io.horizontalsystems.bankwallet.modules.coin.investments.CoinInvestmentsScreen
import io.horizontalsystems.bankwallet.modules.coin.majorholders.CoinMajorHoldersScreen
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.coin.ranks.CoinRankScreen
import io.horizontalsystems.bankwallet.modules.coin.reports.CoinReportsScreen
import io.horizontalsystems.bankwallet.modules.coin.treasuries.CoinTreasuriesScreen
import io.horizontalsystems.bankwallet.modules.info.CoinAnalyticsInfoScreen
import io.horizontalsystems.bankwallet.modules.info.OverallScoreInfoScreen
import io.horizontalsystems.bankwallet.modules.market.tvl.TvlScreen
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.premium.DefenseSystemFeatureScreen
import io.horizontalsystems.bankwallet.modules.premium.PremiumFeature
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.StackBarSlice
import io.horizontalsystems.bankwallet.ui.compose.components.StackedBarChart
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_lucian
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_remus
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.marketkit.models.FullCoin

@Composable
fun CoinAnalyticsScreen(
    fullCoin: FullCoin,
    backStack: NavBackStack<HSScreen>
) {
    val viewModel =
        viewModel<CoinAnalyticsViewModel>(factory = CoinAnalyticsModule.Factory(fullCoin))
    val uiState = viewModel.uiState

    HSSwipeRefresh(
        refreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() },
    ) {
        Crossfade(uiState.viewState, label = "") { viewState ->
            when (viewState) {
                ViewState.Loading -> {
                    Loading()
                }

                ViewState.Success -> {
                    when (val item = uiState.viewItem) {
                        AnalyticsViewItem.NoData -> {
                            ListEmptyView(
                                text = stringResource(R.string.CoinAnalytics_ProjectNoAnalyticData),
                                icon = R.drawable.ic_not_available
                            )
                        }

                        is AnalyticsViewItem.Analytics -> {
                            AnalyticsData(
                                item.blocks,
                                backStack
                            )
                        }

                        null -> {

                        }
                    }
                }

                is ViewState.Error -> {
                    ListErrorView(stringResource(R.string.SyncError), viewModel::refresh)
                }
            }
        }
    }
}

@Composable
private fun AnalyticsData(
    blocks: List<CoinAnalyticsModule.BlockViewItem>,
    backStack: NavBackStack<HSScreen>
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(blocks) { block ->
            if (block.showAsPreview) {
                AnalyticsPreviewBlock(block, backStack)
            } else {
                AnalyticsBlock(
                    block,
                    backStack
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AnalyticsBlock(
    block: CoinAnalyticsModule.BlockViewItem,
    backStack: NavBackStack<HSScreen>
) {
    AnalyticsContainer(
        showFooterDivider = block.showFooterDivider,
        sectionTitle = block.sectionTitle?.let {
            {
                body_leah(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(it)
                )
            }
        },
        titleRow = {
            block.title?.let {
                AnalyticsBlockHeader(
                    title = stringResource(it),
                    isPreview = false,
                    onInfoClick = block.info?.let { info ->
                        {
                            backStack.add(CoinAnalyticsInfoScreen(info))
                        }
                    }
                )
            }
        },
        sectionDescription = {
            block.sectionDescription?.let {
                InfoText(it)
            }
        },
        bottomRows = {
            block.footerItems.forEachIndexed { index, item ->
                FooterCell(item, index, backStack)
            }
        }
    ) {
        Column(
            modifier = Modifier.clickable(
                enabled = block.analyticChart?.chartType != null,
                onClick = {
                    val coinUid = block.analyticChart?.coinUid
                    val chartType = block.analyticChart?.chartType
                    if (coinUid != null && chartType != null) {
//                        TODO("xxx nav3")
//                        ProChartFragment.show(
//                            fragmentManager,
//                            coinUid,
//                            Translator.getString(chartType.titleRes),
//                            chartType,
//                        )
                    }
                }
            )
        ) {
            block.value?.let {
                AnalyticsContentNumber(number = it, period = block.valuePeriod)
            }
            block.analyticChart?.let { chartViewItem ->
                VSpacer(12.dp)
                AnalyticsChart(chartViewItem.analyticChart)
            }
        }
    }
}

@Composable
private fun FooterCell(
    item: CoinAnalyticsModule.FooterType,
    index: Int,
    backStack: NavBackStack<HSScreen>,
) {
    when (item) {
        is CoinAnalyticsModule.FooterType.FooterItem -> {
            AnalyticsFooterCell(
                title = item.title,
                value = item.value,
                showTopDivider = index != 0,
                showRightArrow = item.action != null,
                cellAction = item.action,
                onActionClick = { action ->
                    handleActionClick(action, backStack)
                }
            )
        }

        is CoinAnalyticsModule.FooterType.DetectorFooterItem -> {
            Column(
                modifier = Modifier.clickable {
                    item.action?.let {
                        handleActionClick(it, backStack)
                    }
                }
            ) {
                AnalyticsFooterCell(
                    title = item.title,
                    value = item.value,
                    showTopDivider = index != 0,
                    showRightArrow = item.action != null,
                    cellAction = null,
                    onActionClick = {}
                )

                if (item.issues.isNotEmpty()) {
                    item.issues.forEach { snippet ->
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            subhead2_grey(
                                text = stringResource(snippet.title),
                                modifier = Modifier.weight(1f)
                            )
                            when (snippet.type) {
                                CoinAnalyticsModule.IssueType.High -> {
                                    subhead1_lucian(text = snippet.count)
                                }

                                CoinAnalyticsModule.IssueType.Medium -> {
                                    subhead1_jacob(text = snippet.count)
                                }

                                CoinAnalyticsModule.IssueType.Attention -> {
                                    subhead1_remus(text = snippet.count)
                                }
                            }
                        }
                    }
                    VSpacer(12.dp)
                }
            }
        }
    }
}

@Composable
private fun AnalyticsPreviewBlock(
    block: CoinAnalyticsModule.BlockViewItem,
    backStack: NavBackStack<HSScreen>
) {
    AnalyticsContainer(
        showFooterDivider = block.showFooterDivider,
        sectionTitle = block.sectionTitle?.let {
            {
                body_leah(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(it)
                )
            }
        },
        titleRow = {
            block.title?.let {
                AnalyticsBlockHeader(
                    title = stringResource(it),
                    isPreview = true,
                    onInfoClick = block.info?.let { info ->
                        {
                            backStack.add(CoinAnalyticsInfoScreen(info))
                        }
                    }
                )
            }
        },
        bottomRows = {
            block.footerItems.forEachIndexed { index, item ->
                if (item is CoinAnalyticsModule.FooterType.FooterItem) {
                    PreviewFooterCell(item.title, item.action != null, index)
                } else if (item is CoinAnalyticsModule.FooterType.DetectorFooterItem) {
                    PreviewFooterCell(item.title, item.action != null, index)
                }
            }
        },
        onClick = {
            backStack.add(DefenseSystemFeatureScreen(PremiumFeature.TokenInsightsFeature))
            stat(
                page = StatPage.CoinAnalytics,
                event = StatEvent.OpenPremium(block.statTrigger ?: StatPremiumTrigger.Other)
            )
        }
    ) {
        if (block.value != null) {
            AnalyticsContentNumber(
                number = stringResource(R.string.CoinAnalytics_ThreeDots),
            )
        }
        block.analyticChart?.let { chart ->
            VSpacer(12.dp)
            when (chart.analyticChart) {
                is CoinAnalyticsModule.AnalyticChart.StackedBars -> {
                    val lockedSlices = listOf(
                        StackBarSlice(value = 50.34f, color = Color(0xBF808085)),
                        StackBarSlice(value = 37.75f, color = Color(0x80808085)),
                        StackBarSlice(value = 11.9f, color = Color(0x40808085)),
                    )
                    StackedBarChart(lockedSlices, modifier = Modifier.padding(horizontal = 16.dp))
                }

                is CoinAnalyticsModule.AnalyticChart.Bars -> {
                    AnalyticsChart(
                        CoinAnalyticsModule.zigzagPlaceholderAnalyticChart(false),
                    )
                }

                is CoinAnalyticsModule.AnalyticChart.Line -> {
                    AnalyticsChart(
                        CoinAnalyticsModule.zigzagPlaceholderAnalyticChart(true),
                    )
                }

                is CoinAnalyticsModule.AnalyticChart.TechAdvice -> {
                    TechnicalAdviceBlock(
                        detailText = "",
                        advice = null
                    )
                }
            }
            VSpacer(12.dp)
        }
    }
}

@Composable
private fun PreviewFooterCell(
    title: CoinAnalyticsModule.BoxItem,
    showRightArrow: Boolean,
    index: Int
) {
    AnalyticsFooterCell(
        title = title,
        value = CoinAnalyticsModule.BoxItem.Dots,
        showTopDivider = index != 0,
        showRightArrow = showRightArrow,
        cellAction = null,
        onActionClick = { }
    )
}

private fun handleActionClick(
    action: CoinAnalyticsModule.ActionType,
    backStack: NavBackStack<HSScreen>
) {
    when (action) {
        is CoinAnalyticsModule.ActionType.OpenTokenHolders -> {
            backStack.add(CoinMajorHoldersScreen(action.coin.uid, action.blockchain))
        }

        is CoinAnalyticsModule.ActionType.OpenAudits -> {
            backStack.add(CoinAuditsScreen(action.audits))
        }

        is CoinAnalyticsModule.ActionType.OpenTreasuries -> {
            backStack.add(CoinTreasuriesScreen(action.coin))
        }

        is CoinAnalyticsModule.ActionType.OpenReports -> {
            backStack.add(CoinReportsScreen(action.coinUid))
        }

        is CoinAnalyticsModule.ActionType.OpenInvestors -> {
            backStack.add(CoinInvestmentsScreen(action.coinUid))
        }

        is CoinAnalyticsModule.ActionType.OpenRank -> {
            backStack.add(CoinRankScreen(action.type))
        }

        is CoinAnalyticsModule.ActionType.OpenOverallScoreInfo -> {
            backStack.add(OverallScoreInfoScreen(action.scoreCategory))
        }

        CoinAnalyticsModule.ActionType.OpenTvl -> {
            backStack.add(TvlScreen)
        }

        is CoinAnalyticsModule.ActionType.OpenDetectorsDetails -> {
            backStack.add(DetectorsScreen(action.title, action.issues))
        }
    }
}