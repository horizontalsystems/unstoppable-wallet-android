package io.horizontalsystems.bankwallet.modules.swap.uniswap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.swap.SwapBaseFragment
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceViewModel
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveModule
import io.horizontalsystems.bankwallet.modules.swap.approve.confirmation.SwapApproveConfirmationModule
import io.horizontalsystems.bankwallet.modules.swap.coincard.SwapCoinCardViewComposable
import io.horizontalsystems.bankwallet.modules.swap.coincard.SwapCoinCardViewModel
import io.horizontalsystems.bankwallet.modules.swap.confirmation.uniswap.UniswapConfirmationModule
import io.horizontalsystems.bankwallet.modules.swap.ui.*
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapTradeService.PriceImpactLevel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AdditionalDataCell2
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationResult
import java.util.*

private val uuidFrom = UUID.randomUUID().leastSignificantBits
private val uuidTo = UUID.randomUUID().leastSignificantBits

class UniswapFragment : SwapBaseFragment() {

    private val vmFactory by lazy { UniswapModule.Factory(dex) }
    private val uniswapViewModel by navGraphViewModels<UniswapViewModel>(R.id.swapFragment) { vmFactory }
    private val allowanceViewModelFactory by lazy {
        UniswapModule.AllowanceViewModelFactory(
            uniswapViewModel.service
        )
    }
    private val allowanceViewModel by viewModels<SwapAllowanceViewModel> { allowanceViewModelFactory }
    private val coinCardViewModelFactory by lazy {
        SwapMainModule.CoinCardViewModelFactory(
            this,
            dex,
            uniswapViewModel.service,
            uniswapViewModel.tradeService
        )
    }

    private val fromCoinCardViewModel by lazy {
        ViewModelProvider(this, coinCardViewModelFactory).get(
            SwapMainModule.coinCardTypeFrom,
            SwapCoinCardViewModel::class.java
        )
    }

    private val toCoinCardViewModel by lazy {
        ViewModelProvider(this, coinCardViewModelFactory).get(
            SwapMainModule.coinCardTypeTo,
            SwapCoinCardViewModel::class.java
        )
    }

    override fun restoreProviderState(providerState: SwapMainModule.SwapProviderState) {
        uniswapViewModel.restoreProviderState(providerState)
    }

    override fun getProviderState(): SwapMainModule.SwapProviderState {
        return uniswapViewModel.getProviderState()
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
                UniswapScreen(
                    viewModel = uniswapViewModel,
                    fromCoinCardViewModel = fromCoinCardViewModel,
                    toCoinCardViewModel = toCoinCardViewModel,
                    allowanceViewModel = allowanceViewModel,
                    navController = findNavController(),
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()

        uniswapViewModel.onStart()
    }

    override fun onStop() {
        super.onStop()

        uniswapViewModel.onStop()
    }

}

@Composable
private fun UniswapScreen(
    viewModel: UniswapViewModel,
    fromCoinCardViewModel: SwapCoinCardViewModel,
    toCoinCardViewModel: SwapCoinCardViewModel,
    allowanceViewModel: SwapAllowanceViewModel,
    navController: NavController
) {
    val buttons by viewModel.buttonsLiveData().observeAsState()
    val showProgressbar by viewModel.isLoadingLiveData().observeAsState(false)
    val swapError by viewModel.swapErrorLiveData().observeAsState()
    val approveStep by viewModel.approveStepLiveData().observeAsState()
    val tradeViewItem by viewModel.tradeViewItemLiveData().observeAsState()

    ComposeAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            Spacer(Modifier.height(12.dp))

            SwapCoinCardViewComposable(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = stringResource(R.string.Swap_FromAmountTitle),
                viewModel = fromCoinCardViewModel,
                uuid = uuidFrom,
                amountEnabled = true,
                navController = navController
            )

            SwitchCoinsSection(showProgressbar) { viewModel.onTapSwitch() }

            SwapCoinCardViewComposable(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = stringResource(R.string.Swap_ToAmountTitle),
                viewModel = toCoinCardViewModel,
                uuid = uuidTo,
                amountEnabled = false,
                navController = navController
            )

            SwapAllowance(allowanceViewModel)

            TradeView(tradeViewItem)

            SwapError(swapError)

            ActionButtons(
                buttons = buttons,
                onTapRevoke = {
                    navController.getNavigationResult(SwapApproveModule.requestKey) {
                        if (it.getBoolean(SwapApproveModule.resultKey)) {
                            viewModel.didApprove()
                        }
                    }

                    viewModel.revokeEvmData?.let { revokeEvmData ->
                        navController.slideFromBottom(
                            R.id.swapApproveConfirmationFragment,
                            SwapApproveConfirmationModule.prepareParams(revokeEvmData, viewModel.blockchainType, false)
                        )
                    }
                },
                onTapApprove = {
                    navController.getNavigationResult(SwapApproveModule.requestKey) {
                        if (it.getBoolean(SwapApproveModule.resultKey)) {
                            viewModel.didApprove()
                        }
                    }

                    viewModel.approveData?.let { data ->
                        navController.slideFromBottom(
                            R.id.swapApproveFragment,
                            SwapApproveModule.prepareParams(data)
                        )
                    }
                },
                onTapProceed = {
                    viewModel.proceedParams?.let { sendEvmData ->
                        navController.slideFromRight(
                            R.id.uniswapConfirmationFragment,
                            UniswapConfirmationModule.prepareParams(sendEvmData)
                        )
                    }
                }
            )

            SwapAllowanceSteps(approveStep)

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TradeView(data: UniswapViewModel.TradeViewItem?) {
    data?.let { tradeViewItem ->
        Spacer(Modifier.height(12.dp))
        if (tradeViewItem.buyPrice != null && tradeViewItem.sellPrice != null) {
            AdditionalDataCell2 {
                subhead2_grey(text = stringResource(R.string.Swap_BuyPrice))
                Spacer(Modifier.weight(1f))
                subhead2_grey(text = tradeViewItem.buyPrice)
            }
            AdditionalDataCell2 {
                subhead2_grey(text = stringResource(R.string.Swap_SellPrice))
                Spacer(Modifier.weight(1f))
                subhead2_grey(text = tradeViewItem.sellPrice)
            }
        }
        tradeViewItem.priceImpact?.let { priceImpact ->
            AdditionalDataCell2 {
                subhead2_grey(text = stringResource(R.string.Swap_PriceImpact))
                Spacer(Modifier.weight(1f))
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = priceImpact.value,
                    style = ComposeAppTheme.typography.subhead2,
                    color = getPriceImpactColor(priceImpact.level),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        tradeViewItem.guaranteedAmount?.let { guaranteedAmount ->
            AdditionalDataCell2 {
                subhead2_grey(text = guaranteedAmount.title)
                Spacer(Modifier.weight(1f))
                subhead2_grey(text = guaranteedAmount.value)
            }
        }
    }
}

@Composable
private fun getPriceImpactColor(
    priceImpactLevel: PriceImpactLevel?
): Color {
    return when (priceImpactLevel) {
        PriceImpactLevel.Normal -> ComposeAppTheme.colors.remus
        PriceImpactLevel.Warning -> ComposeAppTheme.colors.jacob
        PriceImpactLevel.Forbidden -> ComposeAppTheme.colors.lucian
        else -> ComposeAppTheme.colors.grey
    }
}
