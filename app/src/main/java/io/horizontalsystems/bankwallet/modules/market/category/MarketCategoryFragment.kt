package io.horizontalsystems.bankwallet.modules.market.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import coil.annotation.ExperimentalCoilApi
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.MarketModule.ViewItemState
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.topcoins.SelectorDialogState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.CoinCategory
import kotlinx.coroutines.launch

class MarketCategoryFragment : BaseFragment() {

    private val categoryUid by lazy {
        arguments?.getString(categoryUidKey)
    }
    private val categoryName by lazy {
        arguments?.getString(categoryNameKey)
    }
    private val categoryDescription by lazy {
        arguments?.getString(categoryDescriptionKey)
    }
    private val categoryImageUrl by lazy {
        arguments?.getString(categoryImageUrlKey)
    }

    val viewModel by viewModels<MarketCategoryViewModel> {
        MarketCategoryModule.Factory(
            categoryUid!!,
            categoryName!!,
            categoryDescription!!,
            categoryImageUrl!!
        )
    }

    @ExperimentalCoilApi
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
                    CategoryScreen(
                        viewModel,
                        { findNavController().popBackStack() },
                        { coinUid -> onCoinClick(coinUid) }
                    )
                }
            }
        }
    }

    private fun onCoinClick(coinUid: String) {
        val arguments = CoinFragment.prepareParams(coinUid)

        findNavController().navigate(R.id.coinFragment, arguments, navOptions())
    }

    companion object {
        private const val categoryUidKey = "category_uid_field"
        private const val categoryNameKey = "category_name_field"
        private const val categoryDescriptionKey = "category_description_field"
        private const val categoryImageUrlKey = "category_image_url_field"

        fun prepareParams(coinCategory: CoinCategory): Bundle {
            return bundleOf(
                categoryUidKey to coinCategory.uid,
                categoryNameKey to coinCategory.name,
                categoryDescriptionKey to coinCategory.description["en"],
                categoryImageUrlKey to coinCategory.imageUrl,
            )
        }
    }

}

@ExperimentalCoilApi
@Composable
fun CategoryScreen(
    viewModel: MarketCategoryViewModel,
    onCloseButtonClick: () -> Unit,
    onCoinClick: (String) -> Unit,
) {
    val viewItemState by viewModel.viewStateLiveData.observeAsState()
    val header by viewModel.headerLiveData.observeAsState()
    val menu by viewModel.menuLiveData.observeAsState()
    val loading by viewModel.loadingLiveData.observeAsState()
    val isRefreshing by viewModel.isRefreshingLiveData.observeAsState()
    val selectorDialogState by viewModel.selectorDialogStateLiveData.observeAsState()

    val interactionSource = remember { MutableInteractionSource() }

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            TopCloseButton(interactionSource, onCloseButtonClick)
            header?.let { header ->
                DescriptionCard(header.title, header.description, header.icon)
            }
            menu?.let { menu ->
                HeaderWithSorting(
                    menu.sortingFieldSelect.selected.titleResId,
                    null,
                    null,
                    menu.marketFieldSelect,
                    viewModel::onSelectMarketField,
                    viewModel::showSelectorMenu
                )
            }

            HSSwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing ?: false || loading ?: false),
                onRefresh = {
                    viewModel.refresh()
                }
            ) {
                when (val state = viewItemState) {
                    is ViewItemState.Error -> {
                        ListErrorView(
                            stringResource(R.string.Market_SyncError)
                        ) {
                            viewModel.onErrorClick()
                        }
                    }
                    is ViewItemState.Data -> {
                        CoinList(state.items, state.scrollToTop, onCoinClick)
                    }
                }
            }
        }
        //Dialog
        when (val option = selectorDialogState) {
            is SelectorDialogState.Opened -> {
                AlertGroup(
                    R.string.Market_Sort_PopupTitle,
                    option.select,
                    { selected -> viewModel.onSelectSortingField(selected) },
                    { viewModel.onSelectorDialogDismiss() }
                )
            }
        }
    }
}

@Composable
private fun CoinList(
    items: List<MarketViewItem>,
    scrollToTop: Boolean,
    onCoinClick: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LazyColumn(state = listState) {
        items(items) { item ->
            MarketCoin(
                item.fullCoin.coin.name,
                item.fullCoin.coin.code,
                item.fullCoin.coin.iconUrl,
                item.fullCoin.iconPlaceholder,
                item.coinRate,
                item.marketDataValue,
                item.rank
            ) { onCoinClick.invoke(item.fullCoin.coin.uid) }
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
private fun MarketCoin(
    coinName: String,
    coinCode: String,
    coinIconUrl: String,
    coinIconPlaceholder: Int,
    coinRate: String? = null,
    marketDataValue: MarketDataValue? = null,
    label: String? = null,
    onClick: (() -> Unit)? = null
) {
    MultilineClear(
        onClick = onClick,
        borderBottom = true
    ) {
        CoinImage(
            iconUrl = coinIconUrl,
            placeholder = coinIconPlaceholder,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp)
        )
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            MarketCoinFirstRow(coinName, coinRate)
            Spacer(modifier = Modifier.height(3.dp))
            MarketCoinSecondRow(coinCode, marketDataValue, label)
        }
    }
}
