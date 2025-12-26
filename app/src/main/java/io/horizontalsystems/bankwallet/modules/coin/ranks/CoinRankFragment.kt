package io.horizontalsystems.bankwallet.modules.coin.ranks

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statPage
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule.RankType
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryToggle
import io.horizontalsystems.bankwallet.ui.compose.components.DescriptionCard
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderSorting
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.captionSB_grey
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold

class CoinRankFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<RankType>(navController) { type ->
            CoinRankScreen(type, navController)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CoinRankScreen(
    type: RankType,
    navController: NavController,
    viewModel: CoinRankViewModel = viewModel(
        factory = CoinRankModule.Factory(type)
    )
) {
    val uiState = viewModel.uiState
    val viewItems = viewModel.uiState.rankViewItems

    HSScaffold(
        title = "",
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = { navController.popBackStack() }
            )
        )
    ) {
        Crossfade(
            targetState = uiState.viewState,
            label = ""
        ) { viewItemState ->
            when (viewItemState) {
                ViewState.Loading -> {
                    Loading()
                }

                is ViewState.Error -> {
                    ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                }

                ViewState.Success -> {
                    var periodSelect by remember { mutableStateOf(uiState.periodSelect) }
                    val listState = rememberSaveable(
                        uiState.periodSelect?.selected,
                        uiState.sortDescending,
                        saver = LazyListState.Saver
                    ) {
                        LazyListState()
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp),
                    ) {
                        item {
                            uiState.header.let { header ->
                                DescriptionCard(header.title, header.description, header.icon)
                            }
                        }

                        stickyHeader {
                            HeaderSorting {
                                ButtonSecondaryCircle(
                                    modifier = Modifier.padding(start = 16.dp),
                                    icon = if (uiState.sortDescending) R.drawable.ic_sort_l2h_20 else R.drawable.ic_sort_h2l_20,
                                    onClick = { viewModel.toggleSortType() }
                                )
                                Spacer(Modifier.weight(1f))
                                periodSelect?.let {
                                    ButtonSecondaryToggle(
                                        modifier = Modifier.padding(end = 16.dp),
                                        select = it,
                                        onSelect = { selectedDuration ->
                                            viewModel.toggle(selectedDuration)
                                            periodSelect = Select(selectedDuration, it.options)
                                        }
                                    )
                                }
                            }
                        }
                        coinRankList(viewItems, type, navController)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.coinRankList(
    items: List<CoinRankModule.RankViewItem>,
    type: RankType,
    navController: NavController
) {
    item {
        HsDivider()
    }
    items(items) { item ->
        CoinRankCell(
            rank = item.rank,
            name = item.title,
            subtitle = item.subTitle,
            iconUrl = item.iconUrl,
            value = item.value,
            onClick = {
                val arguments = CoinFragment.Input(item.coinUid)
                navController.slideFromRight(R.id.coinFragment, arguments)

                stat(page = type.statPage, event = StatEvent.OpenCoin(item.coinUid))
            }
        )
    }
    item {
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun CoinRankCell(
    rank: String,
    name: String,
    subtitle: String,
    iconUrl: String?,
    value: String? = null,
    onClick: () -> Unit = {}
) {
    Column {
        RowUniversal(
            onClick = onClick,
        ) {
            captionSB_grey(
                modifier = Modifier.width(56.dp),
                textAlign = TextAlign.Center,
                maxLines = 1,
                text = rank
            )
            Image(
                painter = rememberAsyncImagePainter(
                    model = iconUrl,
                    error = painterResource(R.drawable.coin_placeholder)
                ),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                headline2_leah(
                    text = name,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                VSpacer(1.dp)
                subhead2_grey(
                    text = subtitle,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }
            value?.let {
                HSpacer(8.dp)
                body_leah(text = it)
            }
            HSpacer(16.dp)
        }
        HsDivider()
    }
}
