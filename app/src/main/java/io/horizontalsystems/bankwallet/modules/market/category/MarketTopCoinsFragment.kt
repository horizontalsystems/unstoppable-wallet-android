package io.horizontalsystems.bankwallet.modules.market.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import coil.compose.rememberImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.category.MarketTopCoinsModule.ViewState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.CoinCategory

class MarketTopCoinsFragment : BaseFragment() {

    private val coinCategoryUid by lazy {
        requireArguments().getString(CATEGORY_UID_KEY) ?: ""
    }

    val viewModel by viewModels<MarketTopCoinsViewModel> {
        MarketTopCoinsModule.Factory(coinCategoryUid)
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

                CategoryScreen(
                    viewModel,
                    { findNavController().popBackStack() },
                    { onSortingClick() },
                    { coinUid -> onCoinClick(coinUid) }
                )
            }
        }
    }

    private fun onSortingClick() {
        val selected = viewModel.sortingField.value

        val items = viewModel.sortingFields.map {
            SelectorItem(getString(it.titleResId), it == selected)
        }

        SelectorDialog
            .newInstance(items, getString(R.string.Market_Sort_PopupTitle)) { position ->
                val selectedSortingField = SortingField.values()[position]
                viewModel.updateSorting(sortingField = selectedSortingField)
            }
            .show(childFragmentManager, "sorting_field_selector")
    }

    private fun onCoinClick(coinUid: String) {
        val arguments = CoinFragment.prepareParams(coinUid)

        findNavController().navigate(R.id.coinFragment, arguments, navOptions())
    }

    companion object {
        private const val CATEGORY_UID_KEY = "category_uid_key"

        fun prepareParams(categoryUid: String?): Bundle {
            return bundleOf(
                CATEGORY_UID_KEY to categoryUid,
            )
        }
    }

}

@Composable
fun CategoryScreen(
    viewModel: MarketTopCoinsViewModel,
    onCloseButtonClick: () -> Unit,
    onSortMenuClick: () -> Unit,
    onCoinClick: (String) -> Unit,
) {

    val state by viewModel.stateLiveData.observeAsState()
    val sortingMenuTitle by viewModel.sortingField.observeAsState(SortingField.HighestCap)
    val marketFieldButton by viewModel.marketFieldButton.observeAsState()
    val topMarketButton by viewModel.topMarketButton.observeAsState()
    val coinCategory by viewModel.coinCategory.observeAsState()
    val interactionSource = remember { MutableInteractionSource() }

    ComposeAppTheme {
        Surface(
            color = ComposeAppTheme.colors.tyler
        ) {
            Column {
                TopCloseButton(interactionSource, onCloseButtonClick)
                LazyColumn {
                    CategoryInfo(coinCategory)
                    CoinListHeader(
                        sortingMenuTitle.titleResId,
                        onSortMenuClick,
                        marketFieldButton,
                        { viewModel.onMarketFieldButtonClick() },
                        topMarketButton,
                        { viewModel.onTopMarketButtonClick() }
                    )
                    ListView(state, onCoinClick)
                }
            }
        }
    }
}

@Composable
private fun TopCloseButton(
    interactionSource: MutableInteractionSource,
    onCloseButtonClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onCloseButtonClick.invoke()
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = "close icon",
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(24.dp),
                tint = ComposeAppTheme.colors.jacob
            )
        }
    }
}

fun LazyListScope.CategoryInfo(coinCategory: CoinCategory?) {
    val category = coinCategory ?: return
    item {
        Column {
            Row(
                modifier = Modifier.height(108.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 12.dp, end = 8.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = category.name,
                        style = ComposeAppTheme.typography.headline1,
                        color = ComposeAppTheme.colors.oz,
                    )
                    Text(
                        text = category.description["en"] ?: "",
                        modifier = Modifier.padding(top = 6.dp),
                        style = ComposeAppTheme.typography.subhead2,
                        color = ComposeAppTheme.colors.grey,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Image(
                    painter = rememberImagePainter(category.imageUrl),
                    contentDescription = "category image",
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(76.dp),
                )
            }
        }
    }

}

fun LazyListScope.CoinListHeader(
    sortMenuTitleRes: Int,
    sortMenuClick: () -> Unit,
    marketFieldButton: MarketListHeaderView.ToggleButton?,
    marketFieldButtonClick: () -> Unit,
    topMarketButton: MarketListHeaderView.ToggleButton?,
    topMarketButtonClick: () -> Unit,
) {
    item {
        Column(modifier = Modifier.fillMaxWidth()) {
            Divider(thickness = 1.dp, color = ComposeAppTheme.colors.steel10)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp)
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SortMenu(sortMenuTitleRes, sortMenuClick)
                }
                Box(modifier = Modifier.padding(start = 8.dp)) {
                    MarketFieldMenu(topMarketButton, topMarketButtonClick)
                }
                Box(modifier = Modifier.padding(start = 8.dp)) {
                    MarketFieldMenu(marketFieldButton, marketFieldButtonClick)
                }
            }
            Divider(thickness = 1.dp, color = ComposeAppTheme.colors.steel10)
        }
    }
}

@Composable
private fun MarketFieldMenu(
    toggleButton: MarketListHeaderView.ToggleButton?,
    onClick: () -> Unit
) {
    toggleButton?.let {
        ButtonSecondaryToggle(
            toggleIndicators = toggleButton.indicators,
            title = toggleButton.title,
            onClick = onClick
        )
    }
}

@Composable
private fun SortMenu(titleRes: Int, onClick: () -> Unit) {
    ButtonSecondaryTransparent(
        title = stringResource(titleRes),
        iconRight = R.drawable.ic_down_arrow_20,
        onClick = onClick
    )
}

fun LazyListScope.ListView(state: ViewState?, onCoinClick: (String) -> Unit) {
    when (state) {
        ViewState.Loading -> {
            item { ListLoadingView() }
        }
        is ViewState.Error -> {
            item { ListErrorView(state.errorText) { } }
        }
        is ViewState.Data -> CoinList(state.items, onCoinClick)
    }
}

fun LazyListScope.CoinList(
    items: List<MarketTopCoinsModule.ViewItem>,
    onCoinClick: (String) -> Unit
) {
    items(items) { item ->
        MarketListCoin(
            item.fullCoin.coin.name,
            item.fullCoin.coin.code,
            item.coinRate,
            item.fullCoin.coin.iconUrl,
            item.fullCoin.iconPlaceholder,
            item.marketDataValue,
            item.rank
        ) { onCoinClick.invoke(item.fullCoin.coin.uid)}
    }
}
