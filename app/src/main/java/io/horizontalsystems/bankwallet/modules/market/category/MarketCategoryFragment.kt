package io.horizontalsystems.bankwallet.modules.market.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import coil.annotation.ExperimentalCoilApi
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.market.MarketModule.ViewItemState
import io.horizontalsystems.bankwallet.modules.market.topcoins.SelectorDialogState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.CoinCategory

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
    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }
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
                        CoinList(state.items, scrollToTopAfterUpdate, onCoinClick)
                        if (scrollToTopAfterUpdate) {
                            scrollToTopAfterUpdate = false
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
