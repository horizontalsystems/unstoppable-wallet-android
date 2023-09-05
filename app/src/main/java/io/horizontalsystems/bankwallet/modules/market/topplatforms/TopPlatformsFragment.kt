package io.horizontalsystems.bankwallet.modules.market.topplatforms

import android.os.Bundle
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.modules.market.platform.MarketPlatformFragment
import io.horizontalsystems.bankwallet.modules.market.topcoins.SelectorDialogState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.parcelable
import java.math.BigDecimal

class TopPlatformsFragment : BaseComposeFragment() {

    private val timeDuration by lazy { arguments?.parcelable<TimeDuration>(timeDurationKey) }
    val viewModel by viewModels<TopPlatformsViewModel> {
        TopPlatformsModule.Factory(timeDuration)
    }

    @Composable
    override fun GetContent() {
        ComposeAppTheme {
            TopPlatformsScreen(
                viewModel,
                findNavController(),
            )
        }
    }

    companion object {
        private const val timeDurationKey = "time_duration"

        fun prepareParams(timeDuration: TimeDuration): Bundle {
            return bundleOf(timeDurationKey to timeDuration)
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopPlatformsScreen(
    viewModel: TopPlatformsViewModel,
    navController: NavController,
) {

    val interactionSource = remember { MutableInteractionSource() }

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            TopCloseButton(interactionSource) { navController.popBackStack() }

            HSSwipeRefresh(
                refreshing = viewModel.isRefreshing,
                onRefresh = {
                    viewModel.refresh()
                }
            ) {
                Crossfade(viewModel.viewState) { state ->
                    when (state) {
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
                            viewModel.viewItems.let { viewItems ->
                                TopPlatformsList(
                                    viewItems = viewItems,
                                    sortingField = viewModel.sortingField,
                                    timeDuration = viewModel.timePeriod,
                                    onItemClick = {
                                        val args = MarketPlatformFragment.prepareParams(it)
                                        navController.slideFromRight(
                                            R.id.marketPlatformFragment,
                                            args
                                        )
                                    },
                                    preItems = {
                                        item {
                                            DescriptionCard(
                                                stringResource(R.string.MarketTopPlatforms_PlatofrmsRank),
                                                stringResource(R.string.MarketTopPlatforms_Description),
                                                ImageSource.Local(R.drawable.ic_platforms)
                                            )
                                        }

                                        stickyHeader {
                                            var timePeriodMenu by remember {
                                                mutableStateOf(viewModel.timePeriodSelect)
                                            }

                                            HeaderSorting(borderTop = true, borderBottom = true) {
                                                SortMenu(
                                                    viewModel.sortingSelect.selected.titleResId,
                                                    viewModel::showSelectorMenu
                                                )
                                                Spacer(modifier = Modifier.weight(1f))
                                                ButtonSecondaryToggle(
                                                    select = timePeriodMenu,
                                                    onSelect = {
                                                        viewModel.onTimePeriodSelect(it)
                                                        timePeriodMenu = Select(
                                                            it,
                                                            viewModel.periodOptions
                                                        )
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        //Dialog
        when (val option = viewModel.selectorDialogState) {
            is SelectorDialogState.Opened -> {
                AlertGroup(
                    R.string.Market_Sort_PopupTitle,
                    option.select,
                    { selected ->
                        viewModel.onSelectSortingField(selected)
                    },
                    { viewModel.onSelectorDialogDismiss() }
                )
            }
            SelectorDialogState.Closed -> {}
        }
    }
}

@Composable
private fun TopPlatformsList(
    viewItems: List<TopPlatformViewItem>,
    sortingField: SortingField,
    timeDuration: TimeDuration,
    onItemClick: (Platform) -> Unit,
    preItems: LazyListScope.() -> Unit
) {
    val state = rememberSaveable(sortingField, timeDuration, saver = LazyListState.Saver) {
        LazyListState(0, 0)
    }

    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize()
    ) {
        preItems.invoke(this)
        items(viewItems) { item ->
            TopPlatformItem(item, onItemClick)
        }
    }
}

@Composable
private fun TopPlatformSecondRow(
    subtitle: String,
    marketDataValue: MarketDataValue?,
    rank: String,
    rankDiff: BigDecimal?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        BadgeWithDiff(
            modifier = Modifier.padding(end = 8.dp),
            text = rank,
            diff = rankDiff
        )
        subhead2_grey(
            text = subtitle,
            maxLines = 1,
        )
        marketDataValue?.let {
            Spacer(modifier = Modifier.weight(1f))
            MarketDataValueComponent(marketDataValue)
        }
    }
}

@Composable
fun TopPlatformItem(item: TopPlatformViewItem, onItemClick: (Platform) -> Unit) {
    SectionItemBorderedRowUniversalClear(
        borderBottom = true,
        onClick = { onItemClick(item.platform) }
    ) {
        CoinImage(
            iconUrl = item.iconUrl,
            placeholder = item.iconPlaceHolder,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(32.dp)
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            MarketCoinFirstRow(item.platform.name, item.marketCap)
            Spacer(modifier = Modifier.height(3.dp))
            TopPlatformSecondRow(
                subtitle = item.subtitle,
                marketDataValue = MarketDataValue.Diff(item.marketCapDiff),
                rank = item.rank.toString(),
                rankDiff = item.rankDiff?.toBigDecimal()
            )
        }
    }
}
