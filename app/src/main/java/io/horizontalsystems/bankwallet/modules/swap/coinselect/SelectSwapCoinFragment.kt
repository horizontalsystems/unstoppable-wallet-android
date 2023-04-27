package io.horizontalsystems.bankwallet.modules.swap.coinselect

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.CoinBalanceItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.setNavigationResult

class SelectSwapCoinFragment : BaseFragment() {

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
                val dex = arguments?.getParcelable<SwapMainModule.Dex>(dexKey)
                val requestId = arguments?.getLong(requestIdKey)
                if (dex == null || requestId == null) {
                    findNavController().popBackStack()
                } else {
                    SelectSwapCoinDialogScreen(
                        navController = findNavController(),
                        dex = dex,
                        onClickItem = {
                            closeWithResult(it, requestId)
                        }
                    )
                }
            }
        }
    }

    private fun closeWithResult(coinBalanceItem: CoinBalanceItem, requestId: Long) {
        setNavigationResult(
            resultBundleKey, bundleOf(
                requestIdKey to requestId,
                coinBalanceItemResultKey to coinBalanceItem
            )
        )
        Handler(Looper.getMainLooper()).postDelayed({
            findNavController().popBackStack()
        }, 100)
    }

    companion object {
        const val resultBundleKey = "selectSwapCoinResultKey"
        const val dexKey = "dexKey"
        const val requestIdKey = "requestIdKey"
        const val coinBalanceItemResultKey = "coinBalanceItemResultKey"

        fun prepareParams(requestId: Long, dex: SwapMainModule.Dex) = bundleOf(requestIdKey to requestId, dexKey to dex)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SelectSwapCoinDialogScreen(
    navController: NavController,
    dex: SwapMainModule.Dex,
    onClickItem: (CoinBalanceItem) -> Unit
) {
    val viewModel = viewModel<SelectSwapCoinViewModel>(factory = SelectSwapCoinModule.Factory(dex))
    val coinItems = viewModel.coinItems

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            SearchBar(
                title = stringResource(R.string.Select_Coins),
                searchHintText = stringResource(R.string.ManageCoins_Search),
                navController = navController,
                onSearchTextChanged = {
                    viewModel.onEnterQuery(it)
                }
            )

            LazyColumn {
                items(coinItems) { coinItem ->
                    SectionUniversalItem(borderTop = true) {
                        RowUniversal(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            onClick = {
                                onClickItem.invoke(coinItem)
                            }
                        ) {
                            CoinImage(
                                iconUrl = coinItem.token.coin.imageUrl,
                                placeholder = coinItem.token.iconPlaceholder,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.size(16.dp))
                            MultitextM1(
                                title = { B2(text = coinItem.token.coin.name) },
                                subtitle = { D1(text = coinItem.token.coin.code) }
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            MultitextM1(
                                title = {
                                    coinItem.balance?.let {
                                        App.numberFormatter.formatCoinFull(it, coinItem.token.coin.code, 8)
                                    }?.let {
                                        B2(text = it)
                                    }
                                },
                                subtitle = {
                                    coinItem.fiatBalanceValue?.let { fiatBalanceValue ->
                                        App.numberFormatter.formatFiatFull(
                                            fiatBalanceValue.value,
                                            fiatBalanceValue.currency.symbol
                                        )
                                    }?.let {
                                        D1(
                                            modifier = Modifier.align(Alignment.End),
                                            text = it
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
