package io.horizontalsystems.bankwallet.modules.swap.oneinch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
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
import io.horizontalsystems.bankwallet.modules.swap.confirmation.oneinch.OneInchConfirmationModule
import io.horizontalsystems.bankwallet.modules.swap.ui.*
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationResult
import java.util.*

private val uuidFrom = UUID.randomUUID().leastSignificantBits
private val uuidTo = UUID.randomUUID().leastSignificantBits

class OneInchFragment : SwapBaseFragment() {

    private val vmFactory by lazy { OneInchModule.Factory(dex) }
    private val oneInchViewModel by navGraphViewModels<OneInchSwapViewModel>(R.id.swapFragment) { vmFactory }
    private val allowanceViewModelFactory by lazy {
        OneInchModule.AllowanceViewModelFactory(
            oneInchViewModel.service
        )
    }
    private val allowanceViewModel by viewModels<SwapAllowanceViewModel> { allowanceViewModelFactory }
    private val cardsFactory by lazy {
        SwapMainModule.CoinCardViewModelFactory(
            this,
            dex,
            oneInchViewModel.service,
            oneInchViewModel.tradeService
        )
    }

    override fun restoreProviderState(providerState: SwapMainModule.SwapProviderState) {
        oneInchViewModel.restoreProviderState(providerState)
    }

    override fun getProviderState(): SwapMainModule.SwapProviderState {
        return oneInchViewModel.getProviderState()
    }

    private val fromCoinCardViewModel by lazy {
        ViewModelProvider(
            this,
            cardsFactory
        )[SwapMainModule.coinCardTypeFrom, SwapCoinCardViewModel::class.java]
    }

    private val toCoinCardViewModel by lazy {
        ViewModelProvider(
            this,
            cardsFactory
        )[SwapMainModule.coinCardTypeTo, SwapCoinCardViewModel::class.java]
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
                OneInchScreen(
                    viewModel = oneInchViewModel,
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

        oneInchViewModel.onStart()
    }

    override fun onStop() {
        super.onStop()

        oneInchViewModel.onStop()
    }

}

@Composable
private fun OneInchScreen(
    viewModel: OneInchSwapViewModel,
    fromCoinCardViewModel: SwapCoinCardViewModel,
    toCoinCardViewModel: SwapCoinCardViewModel,
    allowanceViewModel: SwapAllowanceViewModel,
    navController: NavController
) {
    val buttons by viewModel.buttonsLiveData().observeAsState()
    val showProgressbar by viewModel.isLoadingLiveData().observeAsState(false)
    val swapError by viewModel.swapErrorLiveData().observeAsState()
    val approveStep by viewModel.approveStepLiveData().observeAsState()

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
                    viewModel.proceedParams?.let { params ->
                        navController.slideFromRight(
                            R.id.oneInchConfirmationFragment,
                            OneInchConfirmationModule.prepareParams(params)
                        )
                    }
                }
            )

            SwapAllowanceSteps(approveStep)

            Spacer(Modifier.height(32.dp))
        }
    }
}
