package io.horizontalsystems.bankwallet.modules.coin.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import coil.annotation.ExperimentalCoilApi
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.CoinViewModel
import io.horizontalsystems.bankwallet.modules.coin.audits.CoinAuditsFragment
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.SecurityType
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.SecurityViewItem
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.ViewItem
import io.horizontalsystems.bankwallet.modules.coin.investments.CoinInvestmentsFragment
import io.horizontalsystems.bankwallet.modules.coin.majorholders.CoinMajorHoldersFragment
import io.horizontalsystems.bankwallet.modules.coin.reports.CoinReportsFragment
import io.horizontalsystems.bankwallet.modules.coin.treasuries.CoinTreasuriesFragment
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartFragment
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineClear
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.MiniChartCard
import io.horizontalsystems.core.findNavController

@ExperimentalCoilApi
class CoinDetailsFragment : BaseFragment() {
    private val coinViewModel by navGraphViewModels<CoinViewModel>(R.id.coinFragment)
    private val viewModel by viewModels<CoinDetailsViewModel> { CoinDetailsModule.Factory(coinViewModel.fullCoin) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ComposeAppTheme {
                    CoinDetailsScreen(
                        viewModel,
                        onClickVolumeChart = {
                            MetricChartFragment.show(
                                childFragmentManager,
                                viewModel.coin.uid,
                                viewModel.coin.name,
                                MetricChartModule.MetricChartType.TradingVolume
                            )
                        },
                        onClickTvlChart = {
                            MetricChartFragment.show(
                                childFragmentManager,
                                viewModel.coin.uid,
                                viewModel.coin.name,
                                MetricChartModule.MetricChartType.Tvl
                            )
                        }
                    )
                }
            }
        }
    }

    private fun openMajorHolders() {
        val arguments = CoinMajorHoldersFragment.prepareParams(viewModel.coin.uid)
        findNavController().navigate(R.id.coinMajorHoldersFragment, arguments, navOptions())
    }

    private fun openTvlInDefi() {
        findNavController().navigate(R.id.tvlFragment, null, navOptionsFromBottom())
    }

    private fun openCoinTreasuries() {
        val arguments = CoinTreasuriesFragment.prepareParams(viewModel.coin)
        findNavController().navigate(R.id.coinTreasuriesFragment, arguments, navOptions())
    }

    private fun openCoinInvestments() {
        val arguments = CoinInvestmentsFragment.prepareParams(viewModel.coin.uid)
        findNavController().navigate(R.id.coinInvestmentsFragment, arguments, navOptions())
    }

    private fun openCoinReports() {
        val arguments = CoinReportsFragment.prepareParams(viewModel.coin.uid)
        findNavController().navigate(R.id.coinReportsFragment, arguments, navOptions())
    }

    private fun openCoinAudits(addresses: List<String>) {
        val arguments = CoinAuditsFragment.prepareParams(addresses)
        findNavController().navigate(R.id.coinAuditsFragment, arguments, navOptions())
    }

    private fun openSecurityInfo(type: SecurityType) {
        val arguments = CoinSecurityInfoFragment.prepareParams(type.title, viewModel.securityInfoViewItems(type))
        findNavController().navigate(R.id.coinSecurityInfoFragment, arguments, navOptionsFromBottom())
    }

    @Composable
    private fun CoinDetailsScreen(
        viewModel: CoinDetailsViewModel,
        onClickVolumeChart: () -> Unit,
        onClickTvlChart: () -> Unit,
    ) {
        val viewState by viewModel.viewStateLiveData.observeAsState(ViewState.Success)
        val viewItem by viewModel.viewItemLiveData.observeAsState()
        val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
        val loading by viewModel.loadingLiveData.observeAsState(false)

        HSSwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing || loading),
            onRefresh = { viewModel.refresh() },
        ) {
            when (viewState) {
                ViewState.Success -> {
                    val detailBlocks: MutableList<@Composable (borderTop: Boolean) -> Unit> = mutableListOf()

                    viewItem?.let { viewItem ->
                        viewItem.volumeChart?.let { volumeChart ->
                            detailBlocks.add { borderTop -> TokenVolume(volumeChart, borderTop, onClickVolumeChart) }
                        }

                        if (viewItem.hasMajorHolders) {
                            detailBlocks.add { borderTop -> TokenDistribution(viewItem, borderTop) }
                        }

                        if (viewItem.treasuries != null || viewItem.fundsInvested != null || viewItem.reportsCount != null) {
                            detailBlocks.add { borderTop -> InvestorData(viewItem, borderTop) }
                        }

                        if (viewItem.tvlChart != null || viewItem.tvlRank != null || viewItem.tvlRatio != null) {
                            detailBlocks.add { borderTop -> TokenTvl(viewItem, borderTop, onClickTvlChart) }
                        }

                        if (viewItem.securityViewItems.isNotEmpty() || viewItem.auditAddresses.isNotEmpty()) {
                            detailBlocks.add { borderTop -> SecurityParameters(viewItem, borderTop) }
                        }
                    }

                    if (detailBlocks.size > 0) {
                        LazyColumn {
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
                    ListErrorView(
                        stringResource(R.string.Market_SyncError)
                    ) {
                        viewModel.refresh()
                    }
                }
            }
        }
    }

    @Composable
    private fun TokenVolume(
        volumeChart: CoinDetailsModule.ChartViewItem,
        borderTop: Boolean,
        onClick: () -> Unit
    ) {
        if (borderTop) {
            Spacer(modifier = Modifier.height(24.dp))
        }

        CellSingleLineClear(borderTop = borderTop) {
            Text(
                text = stringResource(R.string.CoinPage_TokenLiquidity),
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.oz,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onClick.invoke()
                }
        ) {
            MiniChartCard(
                title = stringResource(id = R.string.CoinPage_TotalVolume),
                chartViewItem = volumeChart
            )
        }
    }

    @Composable
    private fun TokenTvl(
        viewItem: ViewItem,
        borderTop: Boolean,
        onClick: () -> Unit
    ) {
        if (borderTop) {
            Spacer(modifier = Modifier.height(24.dp))
        }

        CellSingleLineClear(borderTop = borderTop) {
            Text(
                text = stringResource(R.string.CoinPage_TokenTvl),
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.oz,
            )
        }

        viewItem.tvlChart?.let { tvlChart ->
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onClick.invoke()
                    }
            ) {
                MiniChartCard(
                    title = stringResource(id = R.string.CoinPage_DetailsTvl),
                    chartViewItem = tvlChart
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
                    onClick = this::openTvlInDefi
                )
            }
        }

        viewItem.tvlRatio?.let {
            tokenTvls.add {
                CoinDetailsCell(title = stringResource(R.string.CoinPage_TvlMCapRatio), value = it)
            }
        }

        CellSingleLineLawrenceSection(tokenTvls)
    }

    @Composable
    private fun SecurityParameters(viewItem: ViewItem, borderTop: Boolean) {

        if (borderTop) {
            Spacer(modifier = Modifier.height(24.dp))
        }

        CellSingleLineClear(borderTop = borderTop) {
            Text(
                text = stringResource(R.string.CoinPage_SecurityParams),
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.oz,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val securityParams = mutableListOf<@Composable () -> Unit>()

        viewItem.securityViewItems.forEach {
            securityParams.add {
                SecurityParamsCell(it) {
                    openSecurityInfo(it.type)
                }
            }
        }

        if (viewItem.auditAddresses.isNotEmpty()) {
            securityParams.add {
                CoinDetailsCell(title = stringResource(R.string.CoinPage_SecurityParams_Audits)) {
                    openCoinAudits(viewItem.auditAddresses)
                }
            }
        }

        CellSingleLineLawrenceSection(securityParams)
    }

    @Composable
    private fun TokenDistribution(
        viewItem: ViewItem,
        borderTop: Boolean
    ) {
        if (borderTop) {
            Spacer(modifier = Modifier.height(24.dp))
        }

        CellSingleLineClear(borderTop = borderTop) {
            Text(
                text = stringResource(R.string.CoinPage_TokenDistribution),
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.oz,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val distributionItems = mutableListOf<@Composable () -> Unit>()

        distributionItems.add {
            CoinDetailsCell(
                title = stringResource(R.string.CoinPage_MajorHolders),
                value = null,
                onClick = this::openMajorHolders
            )
        }

        CellSingleLineLawrenceSection(distributionItems)
    }

    @Composable
    private fun InvestorData(
        viewItem: ViewItem,
        borderTop: Boolean
    ) {
        if (borderTop) {
            Spacer(modifier = Modifier.height(24.dp))
        }

        CellSingleLineClear(borderTop = borderTop) {
            Text(
                text = stringResource(R.string.CoinPage_InvestorData),
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.oz,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val investorDataList = mutableListOf<@Composable () -> Unit>()

        viewItem.treasuries?.let {
            investorDataList.add {
                CoinDetailsCell(stringResource(R.string.CoinPage_Treasuries), it) {
                    openCoinTreasuries()
                }
            }
        }
        viewItem.fundsInvested?.let {
            investorDataList.add {
                CoinDetailsCell(stringResource(R.string.CoinPage_FundsInvested), it) {
                    openCoinInvestments()
                }
            }
        }
        viewItem.reportsCount?.let {
            investorDataList.add {
                CoinDetailsCell(stringResource(R.string.CoinPage_Reports), it) {
                    openCoinReports()
                }
            }
        }

        CellSingleLineLawrenceSection(investorDataList)
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
                color = ComposeAppTheme.colors.oz,
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
    private fun SecurityParamsCell(viewItem: SecurityViewItem, onClick: (() -> Unit)) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
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

            Image(painter = painterResource(id = R.drawable.ic_info_20), contentDescription = "")
        }
    }
}
