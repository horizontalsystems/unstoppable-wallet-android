package cash.p.terminal.modules.coin.ranks

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
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
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.getInput
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.stat
import cash.p.terminal.core.stats.statPage
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.CoinFragment
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule.RankType
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.Select
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.ButtonSecondaryCircle
import cash.p.terminal.ui.compose.components.ButtonSecondaryToggle
import cash.p.terminal.ui.compose.components.DescriptionCard
import cash.p.terminal.ui.compose.components.HSpacer
import cash.p.terminal.ui.compose.components.HeaderSorting
import cash.p.terminal.ui.compose.components.ListErrorView
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.ScreenMessageWithAction
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.body_leah
import cash.p.terminal.ui.compose.components.captionSB_grey
import cash.p.terminal.ui.compose.components.subhead2_grey

class CoinRankFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val type = navController.getInput<RankType>()

        type?.let { rankType ->
            CoinRankScreen(
                rankType,
                navController,
            )
        } ?: run {
            ScreenMessageWithAction(
                text = stringResource(R.string.Error),
                icon = R.drawable.ic_error_48
            ) {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .padding(horizontal = 48.dp)
                        .fillMaxWidth(),
                    title = stringResource(R.string.Button_Close),
                    onClick = { navController.popBackStack() }
                )
            }
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

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Close),
                    icon = R.drawable.ic_close,
                    onClick = { navController.popBackStack() }
                )
            )
        )
        Crossfade(uiState.viewState, label = "") { viewItemState ->
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
        Divider(
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10,
        )
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
                body_leah(
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
        Divider(
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10,
        )
    }
}
