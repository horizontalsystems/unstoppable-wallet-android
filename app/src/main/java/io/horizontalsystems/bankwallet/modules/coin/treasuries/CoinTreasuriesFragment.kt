package io.horizontalsystems.bankwallet.modules.coin.treasuries

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.market.tvl.TvlModule
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.AlertGroup
import io.horizontalsystems.bankwallet.ui.compose.components.CellFooter
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderSorting
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.uiv3.components.BoxBordered
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellLeftImage
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.ImageType
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSDropdownButton
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSIconButton
import io.horizontalsystems.marketkit.models.Coin

class CoinTreasuriesFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Coin>(navController) { input ->
            CoinTreasuriesScreen(
                viewModel = viewModel(factory = CoinTreasuriesModule.Factory(input)),
                onBackClick = { findNavController().popBackStack() }
            )
        }
    }
}

@Composable
private fun CoinTreasuriesScreen(
    viewModel: CoinTreasuriesViewModel,
    onBackClick: (() -> Unit)
) {
    val viewState by viewModel.viewStateLiveData.observeAsState()
    val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
    val treasuriesData by viewModel.coinTreasuriesLiveData.observeAsState()
    val chainSelectorDialogState by viewModel.treasuryTypeSelectorDialogStateLiveData.observeAsState(
        TvlModule.SelectorDialogState.Closed
    )

    HSScaffold(
        title = stringResource(R.string.CoinPage_Treasuries),
        onBack = onBackClick,
    ) {
        HSSwipeRefresh(
            refreshing = isRefreshing,
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
                                    stickyHeader {
                                        HeaderSorting(borderTop = true, borderBottom = true) {
                                            HSpacer(16.dp)
                                            HSDropdownButton(
                                                variant = ButtonVariant.Secondary,
                                                title = treasuriesData.treasuryTypeSelect.selected.title.getString(),
                                                onClick = {
                                                    viewModel.onClickTreasuryTypeSelector()
                                                }
                                            )
                                            Spacer(Modifier.weight(1f))
                                            HSIconButton(
                                                variant = ButtonVariant.Secondary,
                                                size = ButtonSize.Small,
                                                icon = painterResource(if (treasuriesData.sortDescending) R.drawable.ic_sort_h2l_20 else R.drawable.ic_sort_l2h_20),
                                                onClick = {
                                                    viewModel.onToggleSortType()
                                                }
                                            )
                                            HSpacer(16.dp)
                                        }
                                    }

                                    items(treasuriesData.coinTreasuries) { item ->
                                        BoxBordered(bottom = true) {
                                            TreasuryItem(
                                                coinIconUrl = item.fundLogoUrl,
                                                title = item.fund,
                                                subtitle = item.country,
                                                value = item.amount,
                                                subvalue = item.amountInCurrency,
                                            )
                                        }
                                    }

                                    item {
                                        CellFooter(text = stringResource(id = R.string.CoinPage_Treasuries_PoweredBy))
                                    }
                                }
                            }
                        }

                        null -> {}
                    }
                }
            }
        )
        when (val option = chainSelectorDialogState) {
            is CoinTreasuriesModule.SelectorDialogState.Opened -> {
                AlertGroup(
                    stringResource(R.string.CoinPage_Treasuries_FilterTitle),
                    option.select,
                    viewModel::onSelectTreasuryType,
                    viewModel::onTreasuryTypeSelectorDialogDismiss
                )
            }
        }
    }
}

@Composable
private fun TreasuryItem(
    coinIconUrl: String,
    title: String,
    subtitle: String,
    value: String,
    subvalue: String,
) {
    CellPrimary(
        left = {
            CellLeftImage(
                type = ImageType.Ellipse,
                size = 32,
                painter = rememberAsyncImagePainter(
                    model = coinIconUrl,
                    error = rememberAsyncImagePainter(
                        model = coinIconUrl,
                        error = painterResource(R.drawable.ic_platform_placeholder_32)
                    )
                ),
            )
        },
        middle = {
            CellMiddleInfo(
                title = title.hs,
                subtitle = subtitle.hs,
            )
        },
        right = {
            CellRightInfo(
                title = value.hs,
                subtitle = subvalue.hs
            )
        },
    )
}
