package io.horizontalsystems.bankwallet.modules.swap.uniswap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
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
import io.horizontalsystems.bankwallet.modules.evmfee.FeeSettingsInfoDialog
import io.horizontalsystems.bankwallet.modules.swap.SwapBaseFragment
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceViewModel
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveModule
import io.horizontalsystems.bankwallet.modules.swap.approve.confirmation.SwapApproveConfirmationModule
import io.horizontalsystems.bankwallet.modules.swap.coincard.SwapCoinCardViewModel
import io.horizontalsystems.bankwallet.modules.swap.coincard.SwapCoinCardViewNew
import io.horizontalsystems.bankwallet.modules.swap.confirmation.uniswap.UniswapConfirmationModule
import io.horizontalsystems.bankwallet.modules.swap.ui.ActionButtons
import io.horizontalsystems.bankwallet.modules.swap.ui.SwitchCoinsSection
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapTradeService.PriceImpactLevel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Keyboard
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.compose.components.SecondaryButtonDefaults.buttonColors
import io.horizontalsystems.bankwallet.ui.compose.observeKeyboardState
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
                    navController = findNavController()
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
fun SuggestionsBar(
    modifier: Modifier = Modifier,
    percents: List<Int>,
    onClick: (Int) -> Unit
) {
    Box(modifier = modifier) {
        BoxTyler44(borderTop = true) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                percents.forEach { percent ->
                    ButtonSecondary(
                        onClick = { onClick.invoke(percent) }
                    ) {
                        subhead1_leah(text = "$percent%")
                    }
                }
            }
        }
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
    val tradeViewItem by viewModel.tradeViewItemLiveData().observeAsState()
    val availableBalance by fromCoinCardViewModel.balanceLiveData().observeAsState()
    val keyboardState by observeKeyboardState()
    val fromAmount by fromCoinCardViewModel.amountLiveData().observeAsState()
    val tradeTimeoutProgress by viewModel.tradeTimeoutProgressLiveData().observeAsState()

    ComposeAppTheme {
        val focusRequester = remember { FocusRequester() }
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

                    SwapCoinCardViewNew(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                        viewModel = fromCoinCardViewModel,
                        uuid = uuidFrom,
                        amountEnabled = true,
                        navController = navController,
                        focusRequester = focusRequester
                    ) { isFocused ->
                        showSuggestions = isFocused
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    SwitchCoinsSection(showProgressbar) { viewModel.onTapSwitch() }
                    Spacer(modifier = Modifier.height(2.dp))

                    SwapCoinCardViewNew(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        viewModel = toCoinCardViewModel,
                        uuid = uuidTo,
                        amountEnabled = true,
                        navController = navController
                    )
                }

                if (swapError != null) {
                    swapError?.let {
                        TextImportantError(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            icon = R.drawable.ic_attention_20,
                            title = stringResource(R.string.Error),
                            text = it
                        )
                    }
                } else {
                    val infoItems = mutableListOf<@Composable () -> Unit>()
                    tradeViewItem?.buyPrice?.let { buyPrice ->
                        tradeViewItem?.sellPrice?.let { sellPrice ->
                            infoItems.add { Price(buyPrice, sellPrice, tradeTimeoutProgress ?: 1f) }
                        }
                    }
                    if (allowanceViewModel.uiState.isVisible) {
                        infoItems.add { SwapAllowanceNew(allowanceViewModel, navController) }
                    }
                    tradeViewItem?.priceImpact?.let {
                        infoItems.add { PriceImpact(it, navController) }
                    }

                    if (infoItems.isEmpty()) {
                        availableBalance?.let { infoItems.add { AvailableBalance(it) } }
                    }

                    if (infoItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SingleLineGroup(infoItems)
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
                        viewModel.proceedParams?.let { sendEvmData ->
                            navController.slideFromRight(
                                R.id.uniswapConfirmationFragment,
                                UniswapConfirmationModule.prepareParams(sendEvmData)
                            )
                        }
                    }
                )

                Spacer(Modifier.height(32.dp))
            }

            if (fromAmount?.second.isNullOrEmpty() && showSuggestions && keyboardState == Keyboard.Opened) {
                SuggestionsBar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    percents = listOf(25, 50, 75, 100)
                ) {
                    fromCoinCardViewModel.onSetAmountInBalancePercent(it)
                }
            }
        }
    }
}

@Composable
fun SingleLineGroup(
    composableItems: List<@Composable () -> Unit>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        ) {
            composableItems.forEach { composable ->
                composable()
            }
        }
    }
}

@Composable
private fun AvailableBalance(value: String) {
    Row(modifier = Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        subhead2_grey(text = stringResource(id = R.string.Swap_Balance))
        Spacer(modifier = Modifier.weight(1f))
        subhead2_leah(text = value)
    }
}

@Composable
fun SwapAllowanceNew(
    viewModel: SwapAllowanceViewModel,
    navController: NavController
) {
    val uiState = viewModel.uiState
    val isError = uiState.isError
    val revokeRequired = uiState.revokeRequired
    val allowanceAmount = uiState.allowance
    val visible = uiState.isVisible

    if (visible) {
        if (revokeRequired) {
            TextImportantWarning(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.Approve_RevokeAndApproveInfo, allowanceAmount ?: "")
            )
        } else {
            Row(modifier = Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                val infoTitle = stringResource(id = R.string.SwapInfo_AllowanceTitle)
                val infoText = stringResource(id = R.string.SwapInfo_AllowanceDescription)
                Image(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable {
                            navController.slideFromBottom(
                                R.id.feeSettingsInfoDialog,
                                FeeSettingsInfoDialog.prepareParams(infoTitle, infoText)
                            )
                        },
                    painter = painterResource(id = R.drawable.ic_info_20), contentDescription = ""
                )
                subhead2_grey(text = stringResource(R.string.Swap_Allowance))
                Spacer(Modifier.weight(1f))
                allowanceAmount?.let { amount ->
                    if (isError) {
                        subhead2_lucian(text = amount)
                    } else {
                        subhead2_grey(text = amount)
                    }
                }
            }
        }
    }
}

@Composable
private fun Price(
    buyPrice: String,
    sellPrice: String,
    timeoutProgress: Float
) {
    var showBuyPrice by remember { mutableStateOf(true) }

    Row(modifier = Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically)
    {
        subhead2_grey(text = stringResource(R.string.Swap_Price))
        Spacer(Modifier.weight(1f))

        ButtonSecondary(
            onClick = { showBuyPrice = !showBuyPrice },
            buttonColors = buttonColors(
                backgroundColor = ComposeAppTheme.colors.transparent,
                contentColor = ComposeAppTheme.colors.leah,
                disabledBackgroundColor = ComposeAppTheme.colors.transparent,
                disabledContentColor = ComposeAppTheme.colors.grey50,
            ),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp),
            content = {
                subhead2_leah(
                    text = if (showBuyPrice) buyPrice else sellPrice,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )
        Box(modifier = Modifier.size(14.5.dp)) {
            CircularProgressIndicator(
                progress = 1f,
                modifier = Modifier.size(14.5.dp),
                color = ComposeAppTheme.colors.steel20,
                strokeWidth = 1.5.dp
            )
            CircularProgressIndicator(
                progress = timeoutProgress,
                modifier = Modifier
                    .size(14.5.dp)
                    .scale(scaleX = -1f, scaleY = 1f),
                color = ComposeAppTheme.colors.jacob,
                strokeWidth = 1.5.dp
            )
        }
    }
}

@Composable
private fun PriceImpact(
    priceImpact: UniswapModule.PriceImpactViewItem,
    navController: NavController
) {
    Row(modifier = Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        val infoTitle = stringResource(id = R.string.SwapInfo_PriceImpactTitle)
        val infoText = stringResource(id = R.string.SwapInfo_PriceImpactDescription)
        Image(
            modifier = Modifier
                .padding(end = 8.dp)
                .clickable {
                    navController.slideFromBottom(
                        R.id.feeSettingsInfoDialog,
                        FeeSettingsInfoDialog.prepareParams(infoTitle, infoText)
                    )
                },
            painter = painterResource(id = R.drawable.ic_info_20), contentDescription = ""
        )
        subhead2_grey(text = stringResource(R.string.Swap_PriceImpact))
        Spacer(Modifier.weight(1f))
        Text(
            text = priceImpact.value,
            style = ComposeAppTheme.typography.subhead2,
            color = getPriceImpactColor(priceImpact.level),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
