package io.horizontalsystems.bankwallet.modules.market.filtersresult

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.coin.overview.Loading
import io.horizontalsystems.bankwallet.modules.market.advancedsearch.MarketAdvancedSearchViewModel
import io.horizontalsystems.bankwallet.modules.market.category.MarketCategoryModule
import io.horizontalsystems.bankwallet.modules.market.topcoins.SelectorDialogState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController

class MarketAdvancedSearchResultsFragment : BaseFragment() {

    private val marketSearchFilterViewModel by navGraphViewModels<MarketAdvancedSearchViewModel>(R.id.marketAdvancedSearchFragment)
    private val viewModel by viewModels<MarketFiltersResultViewModel> {
        MarketFiltersResultsModule.Factory(marketSearchFilterViewModel.service)
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
                    SearchResultsScreen(viewModel, findNavController())
                }
            }
        }
    }

}

@Composable
private fun SearchResultsScreen(
    viewModel: MarketFiltersResultViewModel,
    navController: NavController
) {

    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }
    val viewItemState by viewModel.viewStateLiveData.observeAsState(ViewState.Loading)
    val viewItems by viewModel.viewItemsLiveData.observeAsState()
    val menu by viewModel.menuLiveData.observeAsState()
    val selectorDialogState by viewModel.selectorDialogStateLiveData.observeAsState()

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                TranslatableString.ResString(R.string.Market_AdvancedSearch_Results),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                },
            )

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
                            menu?.let {
                                ListHeaderMenu(it, viewModel)
                            }

                            viewItems?.let {
                                CoinList(
                                    items = it,
                                    scrollToTop = scrollToTopAfterUpdate,
                                    onAddFavorite = { uid ->
                                        viewModel.onAddFavorite(uid)
                                    },
                                    onRemoveFavorite = { uid ->
                                        viewModel.onRemoveFavorite(uid)
                                    },
                                    onCoinClick = { coinUid ->
                                        val arguments = CoinFragment.prepareParams(coinUid)
                                        navController.slideFromRight(R.id.coinFragment, arguments)
                                    }
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

        //Dialog
        (selectorDialogState as? SelectorDialogState.Opened)?.let { state ->
            AlertGroup(
                title = R.string.Market_Sort_PopupTitle,
                select = state.select,
                onSelect = { selected ->
                    scrollToTopAfterUpdate = true
                    viewModel.onSelectSortingField(selected)
                },
                onDismiss = { viewModel.onSelectorDialogDismiss() }
            )
        }
    }

}

@Composable
private fun ListHeaderMenu(
    menu: MarketCategoryModule.Menu,
    viewModel: MarketFiltersResultViewModel,
) {
    Header(borderTop = true, borderBottom = true) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp)
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SortMenu(
                    titleRes = menu.sortingFieldSelect.selected.titleResId,
                    onClick = viewModel::showSelectorMenu
                )
            }

            Box(modifier = Modifier.padding(start = 8.dp)) {
                ButtonSecondaryToggle(
                    select = menu.marketFieldSelect,
                    onSelect = viewModel::marketFieldSelected
                )
            }
        }
    }
}
