package io.horizontalsystems.bankwallet.modules.coin.details

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.audits.CoinAuditsFragment
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.SecurityViewItem
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.ViewItem
import io.horizontalsystems.bankwallet.modules.coin.investments.CoinInvestmentsFragment
import io.horizontalsystems.bankwallet.modules.coin.majorholders.CoinMajorHoldersFragment
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.coin.reports.CoinReportsFragment
import io.horizontalsystems.bankwallet.modules.coin.treasuries.CoinTreasuriesFragment
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartTvlFragment
import io.horizontalsystems.bankwallet.modules.metricchart.ProChartFragment
import io.horizontalsystems.bankwallet.modules.metricchart.ProChartModule
import io.horizontalsystems.bankwallet.modules.profeatures.yakauthorization.ProFeaturesBanner
import io.horizontalsystems.bankwallet.modules.profeatures.yakauthorization.YakAuthorizationViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.FullCoin

@Composable
fun CoinDetailsScreen(
    fullCoin: FullCoin,
    authorizationViewModel: YakAuthorizationViewModel,
    navController: NavController,
    fragmentManager: FragmentManager
) {
    val viewModel = viewModel<CoinDetailsViewModel>(factory = CoinDetailsModule.Factory(fullCoin))

    val viewState by viewModel.viewStateLiveData.observeAsState()
    val viewItem by viewModel.viewItemLiveData.observeAsState()
    val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)

    HSSwipeRefresh(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
    ) {
        Crossfade(viewState) { viewState ->
            when (viewState) {
                ViewState.Loading -> {
                    Loading()
                }
                ViewState.Success -> {
                    val detailBlocks: MutableList<@Composable (borderTop: Boolean) -> Unit> = mutableListOf()

                    viewItem?.let { viewItem ->
                        if (!viewItem.proChartsActivated) {
                            detailBlocks.add {
                                ProFeaturesBanner(
                                    stringResource(R.string.CoinPage_NftBannerTitle),
                                    stringResource(R.string.CoinPage_NftBannerDescription)
                                ) { authorizationViewModel.onBannerClick() }
                            }
                        }

                        viewItem.tokenLiquidityViewItem?.let {
                            detailBlocks.add { borderTop ->
                                TokenLiquidity(
                                    coinUid = fullCoin.coin.uid,
                                    viewItem = viewItem,
                                    tokenLiquidityViewItem = it,
                                    borderTop = borderTop,
                                    navController = navController,
                                    fragmentManager = fragmentManager,
                                    onBannerClick = { authorizationViewModel.onBannerClick() }
                                )
                            }
                        }

                        viewItem.tokenDistributionViewItem?.let {
                            detailBlocks.add { borderTop ->
                                TokenDistribution(
                                    coinUid = fullCoin.coin.uid,
                                    viewItem = viewItem,
                                    tokenDistributionViewItem = it,
                                    borderTop = borderTop,
                                    navController = navController,
                                    fragmentManager = fragmentManager,
                                    onBannerClick = { authorizationViewModel.onBannerClick() }
                                )
                            }
                        }

                        if (viewItem.tvlChart != null || viewItem.tvlRank != null || viewItem.tvlRatio != null) {
                            detailBlocks.add { borderTop ->
                                TokenTvl(viewItem, borderTop, navController) {
                                    MetricChartTvlFragment.show(
                                        fragmentManager,
                                        fullCoin.coin.uid,
                                        fullCoin.coin.name
                                    )
                                }
                            }
                        }

                        if (viewItem.treasuries != null || viewItem.fundsInvested != null || viewItem.reportsCount != null) {
                            detailBlocks.add { borderTop -> InvestorData(fullCoin.coin, viewItem, borderTop, navController) }
                        }

                        if (viewItem.securityViewItems.isNotEmpty() || viewItem.auditAddresses.isNotEmpty()) {
                            detailBlocks.add { borderTop -> SecurityParameters(viewItem, borderTop, navController) }
                        }
                    }

                    if (detailBlocks.size > 0) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(detailBlocks.size) { index ->
                                detailBlocks[index].invoke(index != 0)
                            }
                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                }
                is ViewState.Error -> {
                    ListErrorView(stringResource(R.string.SyncError), viewModel::refresh)
                }
                null -> {}
            }
        }
    }


}

@Composable
private fun TokenTvl(
    viewItem: ViewItem,
    borderTop: Boolean,
    navController: NavController,
    onClick: () -> Unit
) {
    CellSingleLineClear(borderTop = borderTop) {
        Text(
            text = stringResource(R.string.CoinPage_TokenTvl),
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
        )
        Spacer(Modifier.weight(1f))
        HsIconButton(
            modifier = Modifier.size(20.dp),
            onClick = {
                navController.slideFromBottom(R.id.tokenTvlInfoFragment)
            }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_info_20),
                contentDescription = "info button",
                tint = ComposeAppTheme.colors.grey
            )
        }
    }

    viewItem.tvlChart?.let { tvlChart ->
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            MiniChartCard(
                title = stringResource(id = R.string.CoinPage_DetailsTvl),
                chartViewItem = tvlChart,
                onClick = onClick
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    val tokenTvls = mutableListOf<@Composable () -> Unit>()

    viewItem.tvlRank?.let {
        tokenTvls.add {
            CoinDetailsCell(
                title = stringResource(R.string.TvlRank_Title),
                value = it,
                onClick = {
                    navController.slideFromBottom(R.id.tvlFragment)
                }
            )
        }
    }

    viewItem.tvlRatio?.let {
        tokenTvls.add {
            CoinDetailsCell(title = stringResource(R.string.CoinPage_TvlMCapRatio), value = it)
        }
    }

    CellSingleLineLawrenceSection(tokenTvls)
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun SecurityParameters(
    viewItem: ViewItem,
    borderTop: Boolean,
    navController: NavController
) {
    CellSingleLineClear(borderTop = borderTop) {
        Text(
            text = stringResource(R.string.CoinPage_SecurityParams),
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
        )
        Spacer(Modifier.weight(1f))
        HsIconButton(
            modifier = Modifier.size(20.dp),
            onClick = {
                navController.slideFromBottom(R.id.securityParamsInfoFragment)
            }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_info_20),
                contentDescription = "info button",
                tint = ComposeAppTheme.colors.grey
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    val securityParams = mutableListOf<@Composable () -> Unit>()

    viewItem.securityViewItems.forEach {
        securityParams.add { SecurityParamsCell(it) }
    }

    if (viewItem.auditAddresses.isNotEmpty()) {
        securityParams.add {
            CoinDetailsCell(title = stringResource(R.string.CoinPage_SecurityParams_Audits)) {
                val arguments = CoinAuditsFragment.prepareParams(viewItem.auditAddresses)
                navController.slideFromRight(R.id.coinAuditsFragment, arguments)
            }
        }
    }

    CellSingleLineLawrenceSection(securityParams)
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun TokenLiquidity(
    coinUid: String,
    viewItem: ViewItem,
    tokenLiquidityViewItem: CoinDetailsModule.TokenLiquidityViewItem,
    borderTop: Boolean,
    navController: NavController,
    fragmentManager: FragmentManager,
    onBannerClick: () -> Unit
) {
    if (tokenLiquidityViewItem.liquidity == null && tokenLiquidityViewItem.volume == null) return

    CellSingleLineClear(borderTop = borderTop) {
        Text(
            text = stringResource(R.string.CoinPage_TokenLiquidity),
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
        )
        Spacer(Modifier.weight(1f))
        HsIconButton(
            modifier = Modifier.size(20.dp),
            onClick = {
                navController.slideFromBottom(R.id.tokenLiquidityInfoFragment)
            }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_info_20),
                contentDescription = "info button",
                tint = ComposeAppTheme.colors.grey
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp)
    ) {
        tokenLiquidityViewItem.volume?.let {
            MiniProChartCard(
                chartType = ProChartModule.ChartType.DexVolume,
                halfWidth = tokenLiquidityViewItem.liquidity != null,
                proChartsActivated = viewItem.proChartsActivated,
                chartViewItem = it,
                title = stringResource(R.string.CoinPage_DetailsDexVolume),
                description = stringResource(id = R.string.CoinPage_DetailsDexVolume_Description),
                fragmentManager = fragmentManager,
                coinUid = coinUid,
                onBannerClick = onBannerClick,
                timePeriodSuffix = it.timePeriodString
            )
        }

        tokenLiquidityViewItem.liquidity?.let {
            MiniProChartCard(
                chartType = ProChartModule.ChartType.DexLiquidity,
                halfWidth = false,
                proChartsActivated = viewItem.proChartsActivated,
                chartViewItem = it,
                title = stringResource(id = R.string.CoinPage_DetailsDexLiquidity),
                description = stringResource(id = R.string.CoinPage_DetailsDexLiquidity_Description),
                fragmentManager = fragmentManager,
                coinUid = coinUid,
                onBannerClick = onBannerClick,
                timePeriodSuffix = null
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun TokenDistribution(
    coinUid: String,
    viewItem: ViewItem,
    tokenDistributionViewItem: CoinDetailsModule.TokenDistributionViewItem,
    borderTop: Boolean,
    navController: NavController,
    fragmentManager: FragmentManager,
    onBannerClick: () -> Unit
) {
    if (!tokenDistributionViewItem.hasMajorHolders && tokenDistributionViewItem.txCount == null && tokenDistributionViewItem.txVolume == null && tokenDistributionViewItem.activeAddresses == null) return

    CellSingleLineClear(borderTop = borderTop) {
        Text(
            text = stringResource(R.string.CoinPage_TokenDistribution),
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
        )
        Spacer(Modifier.weight(1f))
        HsIconButton(
            modifier = Modifier.size(20.dp),
            onClick = {
                navController.slideFromBottom(R.id.tokenDistributionInfoFragment)
            }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_info_20),
                contentDescription = "info button",
                tint = ComposeAppTheme.colors.grey
            )
        }
    }

    if (tokenDistributionViewItem.txCount != null || tokenDistributionViewItem.txVolume != null) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp)
        ) {
            tokenDistributionViewItem.txCount?.let {
                MiniProChartCard(
                    chartType = ProChartModule.ChartType.TxCount,
                    halfWidth = tokenDistributionViewItem.txVolume != null,
                    proChartsActivated = viewItem.proChartsActivated,
                    chartViewItem = it,
                    title = stringResource(R.string.CoinPage_DetailsTxCount),
                    description = stringResource(id = R.string.CoinPage_DetailsTxCount_Description),
                    fragmentManager = fragmentManager,
                    coinUid = coinUid,
                    onBannerClick = onBannerClick,
                    timePeriodSuffix = it.timePeriodString
                )
            }

            tokenDistributionViewItem.txVolume?.let {
                MiniProChartCard(
                    chartType = ProChartModule.ChartType.TxVolume,
                    halfWidth = false,
                    proChartsActivated = viewItem.proChartsActivated,
                    chartViewItem = it,
                    title = stringResource(R.string.CoinPage_DetailsTxVolume),
                    description = stringResource(id = R.string.CoinPage_DetailsTxVolume_Description),
                    fragmentManager = fragmentManager,
                    coinUid = coinUid,
                    onBannerClick = onBannerClick,
                    timePeriodSuffix = it.timePeriodString
                )
            }
        }
    }

    tokenDistributionViewItem.activeAddresses?.let {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp)
        ) {
            MiniProChartCard(
                chartType = ProChartModule.ChartType.AddressesCount,
                halfWidth = false,
                proChartsActivated = viewItem.proChartsActivated,
                chartViewItem = it,
                title = stringResource(id = R.string.CoinPage_DetailsActiveAddresses),
                description = stringResource(id = R.string.CoinPage_DetailsActiveAddresses_Description),
                fragmentManager = fragmentManager,
                coinUid = coinUid,
                onBannerClick = onBannerClick,
                timePeriodSuffix = null
            )
        }
    }

    if (tokenDistributionViewItem.hasMajorHolders) {
        Spacer(modifier = Modifier.height(12.dp))

        CellSingleLineLawrenceSection {
            CoinDetailsCell(
                title = stringResource(R.string.CoinPage_MajorHolders),
                value = null,
                onClick = {
                    val arguments = CoinMajorHoldersFragment.prepareParams(coinUid)
                    navController.slideFromRight(R.id.coinMajorHoldersFragment, arguments)
                }
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun InvestorData(
    coin: Coin,
    viewItem: ViewItem,
    borderTop: Boolean,
    navController: NavController
) {
    CellSingleLineClear(borderTop = borderTop) {
        Text(
            text = stringResource(R.string.CoinPage_InvestorData),
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    val investorDataList = mutableListOf<@Composable () -> Unit>()

    viewItem.treasuries?.let {
        investorDataList.add {
            CoinDetailsCell(stringResource(R.string.CoinPage_Treasuries), it) {
                val arguments = CoinTreasuriesFragment.prepareParams(coin)
                navController.slideFromRight(R.id.coinTreasuriesFragment, arguments)
            }
        }
    }
    viewItem.fundsInvested?.let {
        investorDataList.add {
            CoinDetailsCell(stringResource(R.string.CoinPage_FundsInvested), it) {
                val arguments = CoinInvestmentsFragment.prepareParams(coin.uid)
                navController.slideFromRight(R.id.coinInvestmentsFragment, arguments)

            }
        }
    }
    viewItem.reportsCount?.let {
        investorDataList.add {
            CoinDetailsCell(stringResource(R.string.CoinPage_Reports), it) {
                val arguments = CoinReportsFragment.prepareParams(coin.uid)
                navController.slideFromRight(R.id.coinReportsFragment, arguments)
            }
        }
    }

    CellSingleLineLawrenceSection(investorDataList)
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun CoinDetailsCell(title: String, value: String? = null, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = ComposeAppTheme.typography.subhead2,
            color = ComposeAppTheme.colors.grey
        )

        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            text = value ?: "",
            style = ComposeAppTheme.typography.subhead1,
            color = ComposeAppTheme.colors.leah,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        onClick?.let {
            Image(painter = painterResource(id = R.drawable.ic_arrow_right), contentDescription = "")
        }
    }
}

@Composable
private fun SecurityParamsCell(viewItem: SecurityViewItem) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(viewItem.type.title),
            style = ComposeAppTheme.typography.subhead2,
            color = ComposeAppTheme.colors.grey
        )

        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            text = stringResource(viewItem.value),
            style = ComposeAppTheme.typography.subhead1,
            color = viewItem.grade.securityGradeColor(),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MiniProChartCard(
    chartType: ProChartModule.ChartType,
    halfWidth: Boolean,
    proChartsActivated: Boolean,
    chartViewItem: CoinDetailsModule.ChartViewItem,
    title: String,
    description: String,
    fragmentManager: FragmentManager,
    coinUid: String,
    timePeriodSuffix: Int?,
    onBannerClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(if (halfWidth) 0.5F else 1F)
    ) {
        MiniChartCard(
            title = "$title${timePeriodSuffix?.let { " (${stringResource(id = it)})" } ?: ""}",
            chartViewItem = chartViewItem,
            paddingValues = PaddingValues(start = 4.dp, end = 4.dp),
            onClick = {
                if (proChartsActivated) {
                    ProChartFragment.show(
                        fragmentManager,
                        coinUid,
                        chartType,
                        title,
                        description
                    )
                } else {
                    onBannerClick()
                }
            }
        )
    }
}
