package io.horizontalsystems.bankwallet.modules.coin.treasuries

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.market.tvl.TvlModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.components.AlertGroup
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.CellFooter
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderSorting
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsImage
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.MarketCoinFirstRow
import io.horizontalsystems.bankwallet.ui.compose.components.SectionItemBorderedRowUniversalClear
import io.horizontalsystems.bankwallet.ui.compose.components.SortMenu
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_jacob
import io.horizontalsystems.marketkit.models.Coin

class CoinTreasuriesFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Coin>(navController) { input ->
            CoinTreasuriesScreen(
                viewModel(factory = CoinTreasuriesModule.Factory(input))
            )
        }
    }

    @Composable
    private fun CoinTreasuriesScreen(
        viewModel: CoinTreasuriesViewModel
    ) {
        val viewState by viewModel.viewStateLiveData.observeAsState()
        val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
        val treasuriesData by viewModel.coinTreasuriesLiveData.observeAsState()
        val chainSelectorDialogState by viewModel.treasuryTypeSelectorDialogStateLiveData.observeAsState(
            TvlModule.SelectorDialogState.Closed
        )

        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = stringResource(R.string.CoinPage_Treasuries),
                    navigationIcon = {
                        HsBackButton(onClick = { findNavController().popBackStack() })
                    }
                )
            }
        ) { paddingValues ->
            HSSwipeRefresh(
                refreshing = isRefreshing,
                modifier = Modifier.padding(paddingValues),
                onRefresh = {
                    viewModel.refresh()
                },
                content = {
                    Crossfade(viewState, label = "") { viewState ->
                        when (viewState) {
                            ViewState.Loading -> {
                                Loading()
                            }

                            is ViewState.Error -> {
                                ListErrorView(
                                    stringResource(R.string.SyncError),
                                    viewModel::onErrorClick
                                )
                            }

                            ViewState.Success -> {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    treasuriesData?.let { treasuriesData ->
                                        item {
                                            CoinTreasuriesMenu(
                                                treasuryTypeSelect = treasuriesData.treasuryTypeSelect,
                                                sortDescending = treasuriesData.sortDescending,
                                                onClickTreasuryTypeSelector = viewModel::onClickTreasuryTypeSelector,
                                                onToggleSortType = viewModel::onToggleSortType
                                            )
                                        }

                                        items(treasuriesData.coinTreasuries) { item ->
                                            SectionItemBorderedRowUniversalClear(
                                                borderBottom = true
                                            ) {
                                                HsImage(
                                                    url = item.fundLogoUrl,
                                                    modifier = Modifier
                                                        .padding(end = 16.dp)
                                                        .size(32.dp)
                                                )
                                                Column(
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    MarketCoinFirstRow(item.fund, item.amount)
                                                    VSpacer(3.dp)
                                                    CoinTreasurySecondRow(
                                                        item.country,
                                                        item.amountInCurrency
                                                    )
                                                }
                                            }
                                        }

                                        item {
                                            VSpacer(32.dp)
                                            CellFooter(text = stringResource(id = R.string.CoinPage_Treasuries_PoweredBy))
                                        }
                                    }
                                }
                            }

                            null -> {}
                        }
                    }

                    // chain selector dialog
                    when (val option = chainSelectorDialogState) {
                        is CoinTreasuriesModule.SelectorDialogState.Opened -> {
                            AlertGroup(
                                R.string.CoinPage_Treasuries_FilterTitle,
                                option.select,
                                viewModel::onSelectTreasuryType,
                                viewModel::onTreasuryTypeSelectorDialogDismiss
                            )
                        }
                    }
                }
            )
        }
    }

    @Composable
    private fun CoinTreasuriesMenu(
        treasuryTypeSelect: Select<CoinTreasuriesModule.TreasuryTypeFilter>,
        sortDescending: Boolean,
        onClickTreasuryTypeSelector: () -> Unit,
        onToggleSortType: () -> Unit
    ) {
        HeaderSorting(borderTop = true, borderBottom = true) {
            Box(modifier = Modifier.weight(1f)) {
                SortMenu(treasuryTypeSelect.selected.title, onClickTreasuryTypeSelector)
            }
            ButtonSecondaryCircle(
                modifier = Modifier.padding(end = 16.dp),
                icon = if (sortDescending) R.drawable.ic_sort_h2l_20 else R.drawable.ic_sort_l2h_20,
                onClick = { onToggleSortType() }
            )
        }
    }

    @Composable
    private fun CoinTreasurySecondRow(
        country: String,
        fiatAmount: String
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            subhead2_grey(
                text = country,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.weight(1f))
            subhead2_jacob(
                text = fiatAmount,
                maxLines = 1,
            )
        }
    }
}
