package io.horizontalsystems.bankwallet.modules.swap.oneinch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
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
import io.horizontalsystems.bankwallet.modules.swap.SwapActionState
import io.horizontalsystems.bankwallet.modules.swap.SwapBaseFragment
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainViewModel
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceViewModel
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveModule
import io.horizontalsystems.bankwallet.modules.swap.approve.confirmation.SwapApproveConfirmationModule
import io.horizontalsystems.bankwallet.modules.swap.coincard.SwapCoinCardView
import io.horizontalsystems.bankwallet.modules.swap.coincard.SwapCoinCardViewModel
import io.horizontalsystems.bankwallet.modules.swap.confirmation.oneinch.OneInchConfirmationModule
import io.horizontalsystems.bankwallet.modules.swap.ui.*
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Keyboard
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.observeKeyboardState
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationResult
import java.util.*

private val uuidFrom = UUID.randomUUID().leastSignificantBits
private val uuidTo = UUID.randomUUID().leastSignificantBits

class OneInchFragment : SwapBaseFragment() {

    private val vmFactory by lazy { OneInchModule.Factory(dex) }
    private val oneInchViewModel by navGraphViewModels<OneInchSwapViewModel>(R.id.swapFragment) { vmFactory }
    private val allowanceViewModelFactory by lazy { OneInchModule.AllowanceViewModelFactory(oneInchViewModel.service) }
    private val mainViewModel by navGraphViewModels<SwapMainViewModel>(R.id.swapFragment)
    private val allowanceViewModel by viewModels<SwapAllowanceViewModel> { allowanceViewModelFactory }
    private val cardsFactory by lazy {
        SwapMainModule.CoinCardViewModelFactory(
            this,
            dex,
            oneInchViewModel.service,
            oneInchViewModel.tradeService,
            mainViewModel.switchService
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
    val tradeViewItem by viewModel.tradeViewItemLiveData().observeAsState()
    val isLoading by viewModel.isLoadingLiveData().observeAsState(false)
    val swapError by viewModel.swapErrorLiveData().observeAsState()
    val availableBalance by fromCoinCardViewModel.balanceLiveData().observeAsState()
    val hasNonZeroBalance by fromCoinCardViewModel.hasNonZeroBalance().observeAsState()
    val keyboardState by observeKeyboardState()
    val fromAmount by fromCoinCardViewModel.amountLiveData().observeAsState()
    val tradeTimeoutProgress by viewModel.tradeTimeoutProgressLiveData().observeAsState()

    ComposeAppTheme {
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current
        var showSuggestions by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        Box {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {

                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ComposeAppTheme.colors.lawrence)
                ) {
                    SwapCoinCardView(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp),
                        viewModel = fromCoinCardViewModel,
                        uuid = uuidFrom,
                        amountEnabled = true,
                        navController = navController,
                        focusRequester = focusRequester
                    ) { isFocused ->
                        showSuggestions = isFocused
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    SwitchCoinsSection { viewModel.onTapSwitch() }
                    Spacer(modifier = Modifier.height(8.dp))

                    SwapCoinCardView(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 22.dp),
                        viewModel = toCoinCardViewModel,
                        uuid = uuidTo,
                        amountEnabled = false,
                        navController = navController,
                        isLoading = isLoading
                    )
                }

                if (swapError != null) {
                    swapError?.let {
                        SwapError(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), text = it)
                    }
                } else {
                    val infoItems = mutableListOf<@Composable () -> Unit>()
                    tradeViewItem?.primaryPrice?.let { primaryPrice ->
                        tradeViewItem?.secondaryPrice?.let { secondaryPrice ->
                            infoItems.add { Price(primaryPrice, secondaryPrice, tradeTimeoutProgress ?: 1f, tradeViewItem?.expired ?: false) }
                        }
                    }
                    if (allowanceViewModel.uiState.isVisible && !allowanceViewModel.uiState.revokeRequired) {
                        infoItems.add { SwapAllowance(allowanceViewModel, navController) }
                    }
                    if (infoItems.isEmpty()) {
                        availableBalance?.let { infoItems.add { AvailableBalance(it) } }
                    }
                    if (infoItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SingleLineGroup(infoItems)
                    }

                    if (buttons?.revoke is SwapActionState.Enabled && allowanceViewModel.uiState.revokeRequired) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TextImportantWarning(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = stringResource(R.string.Approve_RevokeAndApproveInfo, allowanceViewModel.uiState.allowance ?: "")
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

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

                Spacer(Modifier.height(32.dp))
            }

            if (hasNonZeroBalance == true && fromAmount?.second.isNullOrEmpty() && showSuggestions && keyboardState == Keyboard.Opened) {
                SuggestionsBar(modifier = Modifier.align(Alignment.BottomCenter)) {
                    focusManager.clearFocus()
                    fromCoinCardViewModel.onSetAmountInBalancePercent(it)
                }
            }
        }
    }
}
