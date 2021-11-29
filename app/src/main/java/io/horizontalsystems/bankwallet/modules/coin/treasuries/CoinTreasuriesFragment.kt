package io.horizontalsystems.bankwallet.modules.coin.treasuries

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.tvl.TvlModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.marketkit.models.Coin

class CoinTreasuriesFragment : BaseFragment() {

    private val viewModel by viewModels<CoinTreasuriesViewModel> {
        CoinTreasuriesModule.Factory(requireArguments().getParcelable(COIN_KEY)!!)
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
                    CoinTreasuriesScreen(viewModel)
                }
            }
        }
    }

    @Composable
    private fun CoinTreasuriesScreen(
        viewModel: CoinTreasuriesViewModel
    ) {
        val viewState by viewModel.viewStateLiveData.observeAsState()
        val loading by viewModel.loadingLiveData.observeAsState(false)
        val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
        val treasuriesData by viewModel.coinTreasuriesLiveData.observeAsState()
        val chainSelectorDialogState by viewModel.treasuryTypeSelectorDialogStateLiveData.observeAsState(TvlModule.SelectorDialogState.Closed)

        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.CoinPage_Treasuries),
                navigationIcon = {
                    IconButton(onClick = { findNavController().popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                }
            )
            HSSwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing || loading),
                onRefresh = {
                    viewModel.refresh()
                }
            ) {
                when (viewState) {
                    is ViewState.Error -> {
                        ListErrorView(
                            stringResource(R.string.Market_SyncError)
                        ) {
                            viewModel.onErrorClick()
                        }
                    }
                    ViewState.Success -> {
                        LazyColumn {
                            treasuriesData?.let { treasuriesData ->
                                item {
                                    CoinTreasuriesMenu(
                                        treasuryTypeSelect = treasuriesData.treasuryTypeSelect,
                                        sortDescending = treasuriesData.sortDescending,
                                        onClickTreasuryTypeSelector = viewModel::onClickTreasuryTypeSelector,
                                        onToggleSortType = viewModel::onToggleSortType
                                    )
                                }

                                items(treasuriesData.coinTreasuries) { item ->
                                    MultilineClear(
                                        borderBottom = true
                                    ) {
                                        CoinImage(
                                            iconUrl = item.fundLogoUrl,
                                            modifier = Modifier
                                                .padding(end = 16.dp)
                                                .size(24.dp)
                                        )
                                        Column(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            MarketCoinFirstRow(item.fund, item.amount)
                                            Spacer(modifier = Modifier.height(3.dp))
                                            CoinTreasurySecondRow(item.country, item.amountInCurrency)
                                        }
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(32.dp))
                                    CellFooter(text = stringResource(id = R.string.CoinPage_Treasuries_PoweredBy))
                                }
                            }
                        }
                    }
                }

                // chain selector dialog
                when (val option = chainSelectorDialogState) {
                    is CoinTreasuriesModule.SelectorDialogState.Opened -> {
                        AlertGroup(
                            R.string.CoinPage_Treasuries_FilterTitle,
                            option.select,
                            viewModel::onSelectTreasuryType,
                            viewModel::onTreasuryTypeSelectorDialogDismiss
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun CoinTreasuriesMenu(
        treasuryTypeSelect: Select<CoinTreasuriesModule.TreasuryTypeFilter>,
        sortDescending: Boolean,
        onClickTreasuryTypeSelector: () -> Unit,
        onToggleSortType: () -> Unit
    ) {
        Header(borderTop = true, borderBottom = true) {
            Box(modifier = Modifier.weight(1f)) {
                SortMenu(treasuryTypeSelect.selected.title, onClickTreasuryTypeSelector)
            }
            ButtonSecondaryCircle(
                modifier = Modifier.padding(end = 16.dp),
                icon = if (sortDescending) R.drawable.ic_arrow_down_20 else R.drawable.ic_arrow_up_20,
                onClick = { onToggleSortType() }
            )
        }
    }

    @Composable
    private fun CoinTreasurySecondRow(
        country: String,
        fiatAmount: String
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = country,
                color = ComposeAppTheme.colors.grey,
                style = ComposeAppTheme.typography.subhead2,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = fiatAmount,
                color = ComposeAppTheme.colors.jacob,
                style = ComposeAppTheme.typography.subhead2,
                maxLines = 1,
            )
        }
    }

    companion object {
        private const val COIN_KEY = "coin_key"

        fun prepareParams(coin: Coin) = bundleOf(COIN_KEY to coin)
    }
}
