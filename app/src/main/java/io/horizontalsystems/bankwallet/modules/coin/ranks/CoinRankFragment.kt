package cash.p.terminal.modules.coin.ranks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule.RankType
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.Select
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import kotlinx.coroutines.launch

class CoinRankFragment : BaseFragment() {

    private val type by lazy {
        requireArguments().getParcelable<RankType>(rankTypeKey)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    type?.let { rankType ->
                        CoinRankScreen(
                            rankType,
                            findNavController(),
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
                                onClick = { findNavController().popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val rankTypeKey = "rank_type_key"

        fun prepareParams(coinUid: RankType) = bundleOf(rankTypeKey to coinUid)
    }
}

@Composable
private fun CoinRankScreen(
    type: RankType,
    navController: NavController,
    viewModel: CoinRankViewModel = viewModel(
        factory = CoinRankModule.Factory(type)
    )
) {

    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }
    val uiState = viewModel.uiState
    val viewItems = viewModel.uiState.rankViewItems

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = TranslatableString.ResString(type.title),
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Close),
                    icon = R.drawable.ic_close,
                    onClick = { navController.popBackStack() }
                )
            )
        )
        Crossfade(uiState.viewState) { viewItemState ->
            when (viewItemState) {
                ViewState.Loading -> {
                    Loading()
                }
                is ViewState.Error -> {
                    ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                }
                ViewState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (viewItems.isEmpty()) {
                            ListEmptyView(
                                text = stringResource(R.string.CoinPage_NoDataAvailable),
                                icon = R.drawable.ic_no_data
                            )
                        } else {
                            if (uiState.showPeriodMenu) {
                                var periodType by remember { mutableStateOf(uiState.periodMenu) }
                                HeaderSorting {
                                    Spacer(Modifier.weight(1f))
                                    ButtonSecondaryToggle(
                                        modifier = Modifier.padding(end = 16.dp),
                                        select = periodType,
                                        onSelect = {
                                            scrollToTopAfterUpdate = true
                                            viewModel.toggle(it)
                                            periodType = Select(it, uiState.periodMenu.options)
                                        }
                                    )
                                }
                            }
                            CoinRankList(viewItems, scrollToTopAfterUpdate)
                            if (scrollToTopAfterUpdate) {
                                scrollToTopAfterUpdate = false
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoinRankList(
    items: List<CoinRankModule.RankViewItem>,
    scrollToTop: Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LazyColumn(state = listState) {
        item {
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
            )
        }
        items(items) { item ->
            CoinRankCell(
                item.rank,
                item.title,
                item.subTitle,
                item.iconUrl,
                item.value
            )
        }
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
        if (scrollToTop) {
            coroutineScope.launch {
                listState.scrollToItem(0)
            }
        }
    }
}

@Composable
private fun CoinRankCell(
    rank: String,
    name: String,
    subtitle: String,
    iconUrl: String?,
    value: String? = null,
) {
    Column {
        RowUniversal {
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
