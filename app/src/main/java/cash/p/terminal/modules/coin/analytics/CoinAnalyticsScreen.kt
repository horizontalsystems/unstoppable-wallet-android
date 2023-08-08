package cash.p.terminal.modules.coin.analytics

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule.AnalyticsViewItem
import cash.p.terminal.modules.coin.analytics.ui.AnalyticsBlockHeader
import cash.p.terminal.modules.coin.analytics.ui.AnalyticsChart
import cash.p.terminal.modules.coin.analytics.ui.AnalyticsContainer
import cash.p.terminal.modules.coin.analytics.ui.AnalyticsContentNumber
import cash.p.terminal.modules.coin.analytics.ui.AnalyticsFooterCell
import cash.p.terminal.modules.coin.audits.CoinAuditsFragment
import cash.p.terminal.modules.coin.investments.CoinInvestmentsFragment
import cash.p.terminal.modules.coin.majorholders.CoinMajorHoldersFragment
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.modules.coin.ranks.CoinRankFragment
import cash.p.terminal.modules.coin.reports.CoinReportsFragment
import cash.p.terminal.modules.coin.technicalindicators.TechnicalIndicatorsDetailsFragment
import cash.p.terminal.modules.coin.treasuries.CoinTreasuriesFragment
import cash.p.terminal.modules.info.CoinAnalyticsInfoFragment
import cash.p.terminal.modules.info.OverallScoreInfoFragment
import cash.p.terminal.modules.metricchart.ProChartFragment
import cash.p.terminal.ui.compose.HSSwipeRefresh
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui.compose.components.ListEmptyView
import cash.p.terminal.ui.compose.components.ListErrorView
import cash.p.terminal.ui.compose.components.StackBarSlice
import cash.p.terminal.ui.compose.components.StackedBarChart
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.body_leah
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.HsPointTimePeriod

@Composable
fun CoinAnalyticsScreen(
    fullCoin: FullCoin,
    navController: NavController,
    fragmentManager: FragmentManager
) {
    val viewModel = viewModel<CoinAnalyticsViewModel>(factory = CoinAnalyticsModule.Factory(fullCoin))
    val uiState = viewModel.uiState

    HSSwipeRefresh(
        refreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() },
    ) {
        Crossfade(uiState.viewState) { viewState ->
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

                        is AnalyticsViewItem.Preview -> {
                            AnalyticsDataPreview(
                                previewBlocks = item.blocks,
                                navController = navController
                            )
                        }

                        is AnalyticsViewItem.Analytics -> {
                            AnalyticsData(
                                item.blocks,
                                navController,
                                fragmentManager,
                                { viewModel.onPeriodChange(it) }
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
    navController: NavController,
    fragmentManager: FragmentManager,
    onPeriodChange: (HsPointTimePeriod) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(blocks) { block ->
            AnalyticsBlock(
                block,
                navController,
                fragmentManager,
                onPeriodChange
            )
        }
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AnalyticsDataPreview(
    previewBlocks: List<CoinAnalyticsModule.PreviewBlockViewItem>,
    navController: NavController,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(previewBlocks) { block ->
            AnalyticsPreviewBlock(block, navController)
        }
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AnalyticsBlock(
    block: CoinAnalyticsModule.BlockViewItem,
    navController: NavController,
    fragmentManager: FragmentManager,
    onPeriodChange: (HsPointTimePeriod) -> Unit
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
                    onInfoClick = block.info?.let { info ->
                        {
                            val params = CoinAnalyticsInfoFragment.prepareParams(info)
                            navController.slideFromRight(R.id.coinAnalyticsInfoFragment, params)
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
                FooterCell(item, index, navController)
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
                        ProChartFragment.show(
                            fragmentManager,
                            coinUid,
                            Translator.getString(chartType.titleRes),
                            chartType,
                        )
                    }
                }
            )
        ) {
            block.value?.let {
                AnalyticsContentNumber(number = it, period = block.valuePeriod)
            }
            block.analyticChart?.let { chartViewItem ->
                VSpacer(12.dp)
                AnalyticsChart(
                    chartViewItem.analyticChart,
                    navController,
                    onPeriodChange
                )
            }
        }
    }
}

@Composable
private fun FooterCell(
    item: CoinAnalyticsModule.FooterItem,
    index: Int,
    navController: NavController
) {
    AnalyticsFooterCell(
        title = item.title,
        value = item.value,
        showTopDivider = index != 0,
        cellAction = item.action,
        onActionClick = { action ->
            when (action) {
                is CoinAnalyticsModule.ActionType.OpenTokenHolders -> {
                    val arguments =
                        CoinMajorHoldersFragment.prepareParams(action.coin.uid, action.blockchain)
                    navController.slideFromBottom(R.id.coinMajorHoldersFragment, arguments)
                }

                is CoinAnalyticsModule.ActionType.OpenAudits -> {
                    val arguments = CoinAuditsFragment.prepareParams(action.auditAddresses)
                    navController.slideFromRight(R.id.coinAuditsFragment, arguments)
                }

                is CoinAnalyticsModule.ActionType.OpenTreasuries -> {
                    val arguments = CoinTreasuriesFragment.prepareParams(action.coin)
                    navController.slideFromRight(R.id.coinTreasuriesFragment, arguments)
                }

                is CoinAnalyticsModule.ActionType.OpenReports -> {
                    val arguments = CoinReportsFragment.prepareParams(action.coinUid)
                    navController.slideFromRight(R.id.coinReportsFragment, arguments)
                }

                is CoinAnalyticsModule.ActionType.OpenInvestors -> {
                    val arguments = CoinInvestmentsFragment.prepareParams(action.coinUid)
                    navController.slideFromRight(R.id.coinInvestmentsFragment, arguments)
                }

                is CoinAnalyticsModule.ActionType.OpenRank -> {
                    val arguments = CoinRankFragment.prepareParams(action.type)
                    navController.slideFromBottom(R.id.coinRankFragment, arguments)
                }

                is CoinAnalyticsModule.ActionType.OpenOverallScoreInfo -> {
                    val params = OverallScoreInfoFragment.prepareParams(action.scoreCategory)
                    navController.slideFromRight(R.id.overallScoreInfoFragment, params)
                }

                CoinAnalyticsModule.ActionType.OpenTvl -> {
                    navController.slideFromBottom(R.id.tvlFragment)
                }

                CoinAnalyticsModule.ActionType.Preview -> {
                    navController.slideFromBottom(R.id.subscriptionInfoFragment)
                }

                is CoinAnalyticsModule.ActionType.OpenTechnicalIndicatorsDetails -> {
                    val params = TechnicalIndicatorsDetailsFragment.prepareParams(action.coinUid, action.period)
                    navController.slideFromRight(R.id.technicalIndicatorsDetailsFragment, params)
                }
            }
        }
    )
}

@Composable
private fun AnalyticsPreviewBlock(block: CoinAnalyticsModule.PreviewBlockViewItem, navController: NavController) {
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
                    onInfoClick = block.info?.let { info ->
                        {
                            val params = CoinAnalyticsInfoFragment.prepareParams(info)
                            navController.slideFromRight(R.id.coinAnalyticsInfoFragment, params)
                        }
                    }
                )
            }
        },
        bottomRows = {
            block.footerItems.forEachIndexed { index, item ->
                FooterCell(item, index, navController)
            }
        }
    ) {
        if (block.showValueDots) {
            AnalyticsContentNumber(
                number = stringResource(R.string.CoinAnalytics_ThreeDots),
            )
        }
        block.chartType?.let { chartType ->
            VSpacer(12.dp)
            if (chartType == CoinAnalyticsModule.PreviewChartType.StackedBars) {
                val lockedSlices = listOf(
                    StackBarSlice(value = 50.34f, color = Color(0xBF808085)),
                    StackBarSlice(value = 37.75f, color = Color(0x80808085)),
                    StackBarSlice(value = 11.9f, color = Color(0x40808085)),
                )
                StackedBarChart(lockedSlices, modifier = Modifier.padding(horizontal = 16.dp))
            } else {
                AnalyticsChart(
                    CoinAnalyticsModule.zigzagPlaceholderAnalyticChart(chartType == CoinAnalyticsModule.PreviewChartType.Line),
                    navController,
                    {},
                )
            }
        }
        if (block.showValueDots || block.chartType != null) {
            VSpacer(12.dp)
        }
    }
}
