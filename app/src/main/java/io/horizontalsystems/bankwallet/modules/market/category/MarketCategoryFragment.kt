package io.horizontalsystems.bankwallet.modules.market.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.viewModels
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.coin.overview.Loading
import io.horizontalsystems.bankwallet.modules.market.topcoins.SelectorDialogState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.CoinCategory

class MarketCategoryFragment : BaseFragment() {

    private val coinCategory by lazy {
        arguments?.get(categoryKey) as CoinCategory
    }

    val viewModel by viewModels<MarketCategoryViewModel> {
        MarketCategoryModule.Factory(coinCategory)
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

        findNavController().slideFromRight(R.id.coinFragment, arguments)
    }

    companion object {
        const val categoryKey = "coin_category"
    }

}

@Composable
fun CategoryScreen(
    viewModel: MarketCategoryViewModel,
    onCloseButtonClick: () -> Unit,
    onCoinClick: (String) -> Unit,
) {
    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }
    val viewItemState by viewModel.viewStateLiveData.observeAsState(ViewState.Loading)
    val viewItems by viewModel.viewItemsLiveData.observeAsState()
    val header by viewModel.headerLiveData.observeAsState()
    val menu by viewModel.menuLiveData.observeAsState()
    val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
    val selectorDialogState by viewModel.selectorDialogStateLiveData.observeAsState()

    val interactionSource = remember { MutableInteractionSource() }

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            TopCloseButton(interactionSource, onCloseButtonClick)
            header?.let { header ->
                DescriptionCard(header.title, header.description, header.icon)
            }

            HSSwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing),
                onRefresh = {
                    viewModel.refresh()
                }
            ) {
                Crossfade(viewItemState) { state ->
                    when (state) {
                        is ViewState.Loading -> {
                            Loading()
                        }
                        is ViewState.Error -> {
                            ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                        }
                        is ViewState.Success -> {
                            Column {
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

                                viewItems?.let {
                                    CoinList(
                                        items = it,
                                        scrollToTop = scrollToTopAfterUpdate,
                                        onAddFavorite = { uid -> viewModel.onAddFavorite(uid) },
                                        onRemoveFavorite = { uid -> viewModel.onRemoveFavorite(uid) },
                                        onCoinClick = onCoinClick
                                    )
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
        //Dialog
        when (val option = selectorDialogState) {
            is SelectorDialogState.Opened -> {
                AlertGroup(
                    R.string.Market_Sort_PopupTitle,
                    option.select,
                    { selected ->
                        scrollToTopAfterUpdate = true
                        viewModel.onSelectSortingField(selected)
                    },
                    { viewModel.onSelectorDialogDismiss() }
                )
            }
        }
    }
}
