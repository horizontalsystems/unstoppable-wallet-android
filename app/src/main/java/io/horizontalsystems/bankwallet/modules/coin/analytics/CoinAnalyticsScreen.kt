package io.horizontalsystems.bankwallet.modules.coin.analytics

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule.AnalyticsViewItem
import io.horizontalsystems.bankwallet.modules.coin.analytics.ui.AnalyticsBlockHeader
import io.horizontalsystems.bankwallet.modules.coin.analytics.ui.AnalyticsChart
import io.horizontalsystems.bankwallet.modules.coin.analytics.ui.AnalyticsContainer
import io.horizontalsystems.bankwallet.modules.coin.analytics.ui.AnalyticsContentNumber
import io.horizontalsystems.bankwallet.modules.coin.analytics.ui.AnalyticsDataLockedBlockNoSubscription
import io.horizontalsystems.bankwallet.modules.coin.analytics.ui.AnalyticsDataLockedBlockNotActivated
import io.horizontalsystems.bankwallet.modules.coin.analytics.ui.AnalyticsFooterCell
import io.horizontalsystems.bankwallet.modules.coin.audits.CoinAuditsFragment
import io.horizontalsystems.bankwallet.modules.coin.investments.CoinInvestmentsFragment
import io.horizontalsystems.bankwallet.modules.coin.majorholders.CoinMajorHoldersFragment
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.coin.ranks.CoinRankFragment
import io.horizontalsystems.bankwallet.modules.coin.reports.CoinReportsFragment
import io.horizontalsystems.bankwallet.modules.coin.treasuries.CoinTreasuriesFragment
import io.horizontalsystems.bankwallet.modules.info.CoinAnalyticsInfoFragment
import io.horizontalsystems.bankwallet.modules.metricchart.ProChartFragment
import io.horizontalsystems.bankwallet.modules.subscription.ActivateSubscriptionFragment
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.StackBarSlice
import io.horizontalsystems.bankwallet.ui.compose.components.StackedBarChart
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.marketkit.models.FullCoin

@Composable
fun CoinAnalyticsScreen(
    fullCoin: FullCoin,
    navController: NavController,
    fragmentManager: FragmentManager
) {
    val viewModel = viewModel<CoinAnalyticsViewModel>(factory = CoinAnalyticsModule.Factory(fullCoin))

    val uiState = viewModel.uiState
    val context = LocalContext.current

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
                                subscriptionAddress = item.subscriptionAddress,
                                onClickLearnMore = {
                                    LinkHelper.openLinkInAppBrowser(context, viewModel.analyticsLink)
                                },
                                onClickActivate = {
                                    navController.slideFromBottom(R.id.activateSubscription, ActivateSubscriptionFragment.prepareParams(it))
                                },
                                navController = navController
                            )
                        }
                        is AnalyticsViewItem.Analytics -> {
                            AnalyticsData(item.blocks, navController, fragmentManager)
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
    fragmentManager: FragmentManager
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(blocks) { block ->
            AnalyticsBlock(
                block,
                navController,
                fragmentManager
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
    subscriptionAddress: String?,
    onClickLearnMore: () -> Unit,
    onClickActivate: (String) -> Unit,
    navController: NavController,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            if (subscriptionAddress != null) {
                AnalyticsDataLockedBlockNotActivated(
                    onClickActivate = {
                        onClickActivate.invoke(subscriptionAddress)
                    }
                )
            } else {
                AnalyticsDataLockedBlockNoSubscription(
                    onClickLearnMore = onClickLearnMore
                )
            }
        }
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
    fragmentManager: FragmentManager
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
        bottomRows = {
            block.footerItems.forEachIndexed { index, item ->
                AnalyticsFooterCell(
                    title = item.title.getString(),
                    value = item.value,
                    leftIcon = item.image,
                    showTopDivider = index != 0,
                    onClick = item.action?.let { action ->
                        {
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
                                CoinAnalyticsModule.ActionType.OpenTvl -> {
                                    navController.slideFromBottom(R.id.tvlFragment)
                                }
                            }
                        }
                    }
                )
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
                AnalyticsChart(chartViewItem.analyticChart)
            }
            if (block.value != null || block.analyticChart != null) {
                VSpacer(12.dp)
            }
        }
    }
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
                AnalyticsFooterCell(
                    title = stringResource(item.title),
                    value = if (item.hasValue) stringResource(R.string.CoinAnalytics_ThreeDots) else null,
                    showTopDivider = index != 0,
                    onClick = if (item.clickable) {
                        { }
                    } else {
                        null
                    }
                )
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
                )
            }
        }
        if (block.showValueDots || block.chartType != null) {
            VSpacer(12.dp)
        }
    }
}
