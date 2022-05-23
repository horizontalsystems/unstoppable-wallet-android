package io.horizontalsystems.bankwallet.modules.coin.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.CoinViewModel
import io.horizontalsystems.bankwallet.modules.coin.audits.CoinAuditsFragment
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.SecurityViewItem
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.ViewItem
import io.horizontalsystems.bankwallet.modules.coin.investments.CoinInvestmentsFragment
import io.horizontalsystems.bankwallet.modules.coin.majorholders.CoinMajorHoldersFragment
import io.horizontalsystems.bankwallet.modules.coin.overview.Loading
import io.horizontalsystems.bankwallet.modules.coin.reports.CoinReportsFragment
import io.horizontalsystems.bankwallet.modules.coin.treasuries.CoinTreasuriesFragment
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartTvlFragment
import io.horizontalsystems.bankwallet.modules.metricchart.ProChartFragment
import io.horizontalsystems.bankwallet.modules.metricchart.ProChartModule
import io.horizontalsystems.bankwallet.modules.profeatures.yakauthorization.ProFeaturesBanner
import io.horizontalsystems.bankwallet.modules.profeatures.yakauthorization.YakAuthorizationModule
import io.horizontalsystems.bankwallet.modules.profeatures.yakauthorization.YakAuthorizationService
import io.horizontalsystems.bankwallet.modules.profeatures.yakauthorization.YakAuthorizationViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.snackbar.CustomSnackbar
import io.horizontalsystems.snackbar.SnackbarDuration

class CoinDetailsFragment : BaseFragment() {
    private val coinViewModel by navGraphViewModels<CoinViewModel>(R.id.coinFragment)
    private val viewModel by viewModels<CoinDetailsViewModel> { CoinDetailsModule.Factory(coinViewModel.fullCoin) }
    private val authorizationViewModel by navGraphViewModels<YakAuthorizationViewModel>(R.id.coinFragment) { YakAuthorizationModule.Factory() }

    private var snackbarInProcess: CustomSnackbar? = null

    override fun onDestroyView() {
        super.onDestroyView()
        snackbarInProcess?.dismiss()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        authorizationViewModel.stateLiveData.observe(viewLifecycleOwner) { state ->
            when (state) {
                YakAuthorizationService.State.Idle ->
                    snackbarInProcess?.dismiss()

                YakAuthorizationService.State.Authenticating ->
                    snackbarInProcess = HudHelper.showInProcessMessage(
                        requireView(),
                        R.string.ProUsersInfo_Features_Authenticating,
                        SnackbarDuration.INDEFINITE
                    )

                YakAuthorizationService.State.NoYakNft -> {
                    snackbarInProcess?.dismiss()
                    findNavController().slideFromBottom(
                        R.id.proUsersInfoDialog
                    )
                }

                YakAuthorizationService.State.Authenticated -> {}

                is YakAuthorizationService.State.SignMessageReceived -> {
                    snackbarInProcess?.dismiss()
                    findNavController().slideFromBottom(
                        R.id.proUsersActivateDialog
                    )
                }

                is YakAuthorizationService.State.Failed ->
                    snackbarInProcess = HudHelper.showErrorMessage(
                        requireView(),
                        state.exception.toString()
                    )
            }
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ComposeAppTheme {
                    CoinDetailsScreen(
                        viewModel,
                        onClickTvlChart = {
                            MetricChartTvlFragment.show(
                                childFragmentManager,
                                viewModel.coin.uid,
                                viewModel.coin.name
                            )
                        }
                    )
                }
            }
        }
    }

    private fun openMajorHolders() {
        val arguments = CoinMajorHoldersFragment.prepareParams(viewModel.coin.uid)
        findNavController().slideFromRight(R.id.coinMajorHoldersFragment, arguments)
    }

    private fun openTvlInDefi() {
        findNavController().slideFromBottom(R.id.tvlFragment)
    }

    private fun openCoinTreasuries() {
        val arguments = CoinTreasuriesFragment.prepareParams(viewModel.coin)
        findNavController().slideFromRight(R.id.coinTreasuriesFragment, arguments)
    }

    private fun openCoinInvestments() {
        val arguments = CoinInvestmentsFragment.prepareParams(viewModel.coin.uid)
        findNavController().slideFromRight(R.id.coinInvestmentsFragment, arguments)
    }

    private fun openCoinReports() {
        val arguments = CoinReportsFragment.prepareParams(viewModel.coin.uid)
        findNavController().slideFromRight(R.id.coinReportsFragment, arguments)
    }

    private fun openCoinAudits(addresses: List<String>) {
        val arguments = CoinAuditsFragment.prepareParams(addresses)
        findNavController().slideFromRight(R.id.coinAuditsFragment, arguments)
    }

    @Composable
    private fun CoinDetailsScreen(
        viewModel: CoinDetailsViewModel,
        onClickTvlChart: () -> Unit,
    ) {
        val viewState by viewModel.viewStateLiveData.observeAsState()
        val viewItem by viewModel.viewItemLiveData.observeAsState()
        val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)

        HSSwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing),
            onRefresh = { viewModel.refresh() },
        ) {
            Crossfade(viewState) { viewState ->
                when (viewState) {
                    is ViewState.Loading -> {
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
                                detailBlocks.add { borderTop -> TokenLiquidity(viewItem, it, borderTop) }
                            }

                            viewItem.tokenDistributionViewItem?.let {
                                detailBlocks.add { borderTop -> TokenDistribution(viewItem, it, borderTop) }
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
                        ListErrorView(stringResource(R.string.SyncError), viewModel::refresh)
                    }
                }
            }
        }
    }

    @Composable
    private fun TokenTvl(
        viewItem: ViewItem,
        borderTop: Boolean,
        onClick: () -> Unit
    ) {
        CellSingleLineClear(borderTop = borderTop) {
            Text(
                text = stringResource(R.string.CoinPage_TokenTvl),
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.oz,
            )
            Spacer(Modifier.weight(1f))
            HsIconButton(onClick = {
                findNavController().slideFromBottom(R.id.tokenTvlInfoFragment)
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
        Spacer(modifier = Modifier.height(24.dp))
    }

    @Composable
    private fun SecurityParameters(viewItem: ViewItem, borderTop: Boolean) {
        CellSingleLineClear(borderTop = borderTop) {
            Text(
                text = stringResource(R.string.CoinPage_SecurityParams),
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.oz,
            )
            Spacer(Modifier.weight(1f))
            HsIconButton(onClick = {
                findNavController().slideFromBottom(R.id.securityParamsInfoFragment)
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
                    openCoinAudits(viewItem.auditAddresses)
                }
            }
        }

        CellSingleLineLawrenceSection(securityParams)
        Spacer(modifier = Modifier.height(24.dp))
    }

    @Composable
    private fun TokenLiquidity(
        viewItem: ViewItem,
        tokenLiquidityViewItem: CoinDetailsModule.TokenLiquidityViewItem,
        borderTop: Boolean
    ) {
        if (tokenLiquidityViewItem.liquidity == null && tokenLiquidityViewItem.volume == null) return

        CellSingleLineClear(borderTop = borderTop) {
            Text(
                text = stringResource(R.string.CoinPage_TokenLiquidity),
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.oz,
            )
            Spacer(Modifier.weight(1f))
            HsIconButton(onClick = {
                findNavController().slideFromBottom(R.id.tokenLiquidityInfoFragment)
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
                .padding(start = 10.dp, end = 10.dp)
        ) {
            tokenLiquidityViewItem.volume?.let {
                miniProChartCard(
                    ProChartModule.ChartType.DexVolume,
                    tokenLiquidityViewItem.liquidity != null,
                    viewItem.proChartsActivated,
                    it,
                    stringResource(id = R.string.CoinPage_DetailsDexVolume),
                    stringResource(id = R.string.CoinPage_DetailsDexVolume_Description)
                )
            }

            tokenLiquidityViewItem.liquidity?.let {
                miniProChartCard(
                    ProChartModule.ChartType.DexLiquidity,
                    false,
                    viewItem.proChartsActivated,
                    it,
                    stringResource(id = R.string.CoinPage_DetailsDexLiquidity),
                    stringResource(id = R.string.CoinPage_DetailsDexLiquidity_Description)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    @Composable
    private fun TokenDistribution(
        viewItem: ViewItem,
        tokenDistributionViewItem: CoinDetailsModule.TokenDistributionViewItem,
        borderTop: Boolean
    ) {
        if (!tokenDistributionViewItem.hasMajorHolders && tokenDistributionViewItem.txCount == null && tokenDistributionViewItem.txVolume == null && tokenDistributionViewItem.activeAddresses == null) return

        CellSingleLineClear(borderTop = borderTop) {
            Text(
                text = stringResource(R.string.CoinPage_TokenDistribution),
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.oz,
            )
            Spacer(Modifier.weight(1f))
            HsIconButton(onClick = {
                findNavController().slideFromBottom(R.id.tokenDistributionInfoFragment)
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
                    .padding(start = 10.dp, end = 10.dp)
            ) {
                tokenDistributionViewItem.txCount?.let {
                    miniProChartCard(
                        ProChartModule.ChartType.TxCount,
                        tokenDistributionViewItem.txVolume != null,
                        viewItem.proChartsActivated,
                        it,
                        stringResource(id = R.string.CoinPage_DetailsTxCount),
                        stringResource(id = R.string.CoinPage_DetailsTxCount_Description)
                    )
                }

                tokenDistributionViewItem.txVolume?.let {
                    miniProChartCard(
                        ProChartModule.ChartType.TxVolume,
                        false,
                        viewItem.proChartsActivated,
                        it,
                        stringResource(id = R.string.CoinPage_DetailsTxVolume),
                        stringResource(id = R.string.CoinPage_DetailsTxVolume_Description)
                    )
                }
            }
        }

        tokenDistributionViewItem.activeAddresses?.let {
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp)
            ) {
                miniProChartCard(
                    ProChartModule.ChartType.AddressesCount,
                    false,
                    viewItem.proChartsActivated,
                    it,
                    stringResource(id = R.string.CoinPage_DetailsActiveAddresses),
                    stringResource(id = R.string.CoinPage_DetailsActiveAddresses_Description)
                )
            }
        }

        if (tokenDistributionViewItem.hasMajorHolders) {
            Spacer(modifier = Modifier.height(12.dp))

            CellSingleLineLawrenceSection {
                CoinDetailsCell(
                    title = stringResource(R.string.CoinPage_MajorHolders),
                    value = null,
                    onClick = this::openMajorHolders
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    @Composable
    private fun InvestorData(
        viewItem: ViewItem,
        borderTop: Boolean
    ) {
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
    private fun miniProChartCard(chartType: ProChartModule.ChartType, halfWidth: Boolean, proChartsActivated: Boolean,
                                 chartViewItem: CoinDetailsModule.ChartViewItem, title: String, description: String) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (halfWidth) 0.5F else 1F)
                .clickable {
                    if (proChartsActivated) {
                        ProChartFragment.show(
                            childFragmentManager,
                            viewModel.coin.uid,
                            chartType,
                            title,
                            description
                        )
                    } else {
                        authorizationViewModel.onBannerClick()
                    }
                }
        ) {
            MiniChartCard(
                title = title,
                chartViewItem = chartViewItem,
                PaddingValues(start = 6.dp, end = 6.dp)
            )
        }
    }

}
