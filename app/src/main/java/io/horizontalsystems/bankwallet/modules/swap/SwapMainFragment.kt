package io.horizontalsystems.bankwallet.modules.swap

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromBottomForResult
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.evmfee.FeeSettingsInfoDialog
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.PriceImpactLevel
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ProviderTradeData
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapActionState
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceViewModel
import io.horizontalsystems.bankwallet.modules.swap.approve.confirmation.SwapApproveConfirmationFragment
import io.horizontalsystems.bankwallet.modules.swap.approve.confirmation.SwapApproveConfirmationModule
import io.horizontalsystems.bankwallet.modules.swap.confirmation.oneinch.OneInchSwapConfirmationFragment
import io.horizontalsystems.bankwallet.modules.swap.confirmation.uniswap.UniswapConfirmationFragment
import io.horizontalsystems.bankwallet.modules.swap.settings.oneinch.OneInchSettingsFragment
import io.horizontalsystems.bankwallet.modules.swap.settings.uniswap.UniswapSettingsFragment
import io.horizontalsystems.bankwallet.modules.swap.ui.ActionButtons
import io.horizontalsystems.bankwallet.modules.swap.ui.AvailableBalance
import io.horizontalsystems.bankwallet.modules.swap.ui.Price
import io.horizontalsystems.bankwallet.modules.swap.ui.SingleLineGroup
import io.horizontalsystems.bankwallet.modules.swap.ui.SuggestionsBar
import io.horizontalsystems.bankwallet.modules.swap.ui.SwapAllowance
import io.horizontalsystems.bankwallet.modules.swap.ui.SwapError
import io.horizontalsystems.bankwallet.modules.swap.ui.SwitchCoinsSection
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Keyboard.Opened
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryToggle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.observeKeyboardState
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class SwapMainFragment : BaseFragment() {

    @Parcelize
    data class Input(
        val tokenFrom: Token,
        val swapEntryPointDestId: Int = 0
    ) : Parcelable

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            val navController = findNavController()
            try {
                val input = navController.requireInput<Input>()
                val swapEntryPointDestId = input.swapEntryPointDestId
                val factory = SwapMainModule.Factory(input.tokenFrom)
                val mainViewModel: SwapMainViewModel by viewModels { factory }
                val allowanceViewModel: SwapAllowanceViewModel by viewModels { factory }
                setContent {
                    ComposeAppTheme {
                        SwapNavHost(
                            navController,
                            mainViewModel,
                            allowanceViewModel,
                            swapEntryPointDestId
                        )
                    }
                }
            } catch (t: Throwable) {
                navController.popBackStack()
            }
        }
    }
}

@Composable
private fun SwapNavHost(
    fragmentNavController: NavController,
    mainViewModel: SwapMainViewModel,
    allowanceViewModel: SwapAllowanceViewModel,
    swapEntryPointDestId: Int,
) {
    SwapMainScreen(
        navController = fragmentNavController,
        viewModel = mainViewModel,
        allowanceViewModel = allowanceViewModel,
        onCloseClick = { fragmentNavController.popBackStack() },
        swapEntryPointDestId = swapEntryPointDestId
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SwapMainScreen(
    navController: NavController,
    viewModel: SwapMainViewModel,
    allowanceViewModel: SwapAllowanceViewModel,
    onCloseClick: () -> Unit,
    swapEntryPointDestId: Int,
) {
    val coroutineScope = rememberCoroutineScope()
    val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val providerViewItems = viewModel.swapState.providerViewItems
    val focusManager = LocalFocusManager.current

    ModalBottomSheetLayout(
        sheetState = modalBottomSheetState,
        sheetBackgroundColor = ComposeAppTheme.colors.transparent,
        sheetContent = {
            BottomSheetProviderSelector(
                items = providerViewItems,
                onSelect = { viewModel.setProvider(it) }
            ) {
                coroutineScope.launch {
                    modalBottomSheetState.hide()
                }
            }
        },
    ) {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = stringResource(R.string.Swap),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
                menuItems = listOf()
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                TopMenu(
                    viewModel = viewModel,
                    navController = navController,
                    showProviderSelector = {
                        focusManager.clearFocus(true)
                        coroutineScope.launch {
                            modalBottomSheetState.show()
                        }
                    }
                )
                SwapCards(
                    navController = navController,
                    viewModel = viewModel,
                    allowanceViewModel = allowanceViewModel,
                    focusManager = focusManager,
                    swapEntryPointDestId = swapEntryPointDestId
                )
            }
        }
    }
}

@Composable
fun SwapCards(
    navController: NavController,
    viewModel: SwapMainViewModel,
    allowanceViewModel: SwapAllowanceViewModel,
    focusManager: FocusManager,
    swapEntryPointDestId: Int
) {

    val focusRequester = remember { FocusRequester() }
    val keyboardState by observeKeyboardState()
    var showSuggestions by remember { mutableStateOf(false) }

    val swapState = viewModel.swapState
    val fromState = viewModel.swapState.fromState
    val toState = viewModel.swapState.toState
    val availableBalance = viewModel.swapState.availableBalance
    val swapError = viewModel.swapState.error
    val tradeView = viewModel.swapState.tradeView
    val tradePriceExpiration = viewModel.swapState.tradePriceExpiration
    val buttons = viewModel.swapState.buttons
    val hasNonZeroBalance = viewModel.swapState.hasNonZeroBalance

    LaunchedEffect(swapState.refocusKey) {
        focusRequester.requestFocus()
    }

    Box {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            VSpacer(12.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ComposeAppTheme.colors.lawrence)
            ) {

                SwapCoinCardView(
                    dex = viewModel.swapState.dex,
                    cardState = fromState,
                    navController = navController,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp),
                    focusRequester = focusRequester,
                    onCoinSelect = { viewModel.onSelectFromCoin(it) },
                    onAmountChange = { viewModel.onFromAmountChange(it) },
                ) { isFocused ->
                    showSuggestions = isFocused
                }

                VSpacer(8.dp)
                SwitchCoinsSection { viewModel.onTapSwitch() }
                VSpacer(8.dp)

                SwapCoinCardView(
                    dex = viewModel.swapState.dex,
                    cardState = toState,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 22.dp),
                    navController = navController,
                    onCoinSelect = { viewModel.onSelectToCoin(it) },
                    onAmountChange = { viewModel.onToAmountChange(it) },
                )
            }

            if (swapError != null) {
                SwapError(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), text = swapError)
            } else {
                val infoItems = mutableListOf<@Composable () -> Unit>()

                when (val data = tradeView?.providerTradeData) {
                    is ProviderTradeData.OneInchTradeViewItem -> {
                        data.primaryPrice?.let { primaryPrice ->
                            data.secondaryPrice?.let { secondaryPrice ->
                                infoItems.add { Price(primaryPrice, secondaryPrice, tradePriceExpiration ?: 1f, tradeView.expired) }
                            }
                        }
                    }

                    is ProviderTradeData.UniswapTradeViewItem -> {
                        data.primaryPrice?.let { primaryPrice ->
                            data.secondaryPrice?.let { secondaryPrice ->
                                infoItems.add { Price(primaryPrice, secondaryPrice, tradePriceExpiration ?: 1f, tradeView.expired) }
                            }
                        }
                        data.priceImpact?.let {
                            infoItems.add { PriceImpact(it, navController) }
                        }
                    }

                    else -> {}
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
            }

            if (buttons.revoke is SwapActionState.Enabled && allowanceViewModel.uiState.revokeRequired) {
                Spacer(modifier = Modifier.height(12.dp))
                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.Approve_RevokeAndApproveInfo, allowanceViewModel.uiState.allowance ?: "")
                )
            }

            VSpacer(32.dp)

            ActionButtons(
                buttons = buttons,
                onTapRevoke = {
                    viewModel.revokeEvmData?.let { revokeEvmData ->
                        navController.slideFromBottomForResult<SwapApproveConfirmationFragment.Result>(
                            R.id.swapApproveConfirmationFragment,
                            SwapApproveConfirmationModule.Input(revokeEvmData, swapState.dex.blockchainType, false)
                        ) {
                            if (it.approved) {
                                viewModel.didApprove()
                            }
                        }
                    }
                },
                onTapApprove = {
                    viewModel.approveData?.let { data ->
                        navController.slideFromBottomForResult<SwapApproveConfirmationFragment.Result>(
                            R.id.swapApproveFragment,
                            data
                        ) {
                            if (it.approved) {
                                viewModel.didApprove()
                            }
                        }
                    }
                },
                onTapProceed = {
                    when (val swapData = viewModel.proceedParams) {
                        is SwapMainModule.SwapData.OneInchData -> {
                            navController.slideFromRight(
                                R.id.oneInchConfirmationFragment,
                                OneInchSwapConfirmationFragment.Input(
                                    swapState.dex.blockchainType,
                                    swapData.data,
                                    swapEntryPointDestId
                                )
                            )
                        }

                        is SwapMainModule.SwapData.UniswapData -> {
                            viewModel.getSendEvmData(swapData)?.let { sendEvmData ->
                                navController.slideFromRight(
                                    R.id.uniswapConfirmationFragment,
                                    UniswapConfirmationFragment.Input(
                                        swapState.dex,
                                        SendEvmModule.TransactionDataParcelable(sendEvmData.transactionData),
                                        sendEvmData.additionalInfo,
                                        swapEntryPointDestId
                                    )
                                )
                            }
                        }

                        null -> {}
                    }
                }
            )
        }

        VSpacer(32.dp)
        if (showSuggestions && keyboardState == Opened) {
            SuggestionsBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                onDelete = {
                    viewModel.onFromAmountChange(null)
                },
                onSelect = {
                    focusManager.clearFocus()
                    viewModel.onSetAmountInBalancePercent(it)
                },
                selectEnabled = hasNonZeroBalance ?: false,
                deleteEnabled = fromState.inputState.amount.isNotBlank()
            )
        }
    }
}


@Composable
private fun TopMenu(
    viewModel: SwapMainViewModel,
    navController: NavController,
    showProviderSelector: () -> Unit,
) {
    val state = viewModel.swapState
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f)) {
            ButtonSecondaryTransparent(
                title = state.dex.provider.title,
                iconRight = R.drawable.ic_down_arrow_20,
                onClick = showProviderSelector
            )
        }
        ButtonSecondaryToggle(
            modifier = Modifier.padding(end = 16.dp),
            select = state.amountTypeSelect,
            onSelect = {
                viewModel.onToggleAmountType()
            },
            enabled = state.amountTypeSelectEnabled
        )
        ButtonSecondaryCircle(
            icon = R.drawable.ic_manage_2,
            onClick = {
                val onResult: (SwapMainModule.Result) -> Unit = {
                    viewModel.onUpdateSwapSettings(it.recipient, it.slippageStr.toBigDecimal(), it.ttl)
                }
                when (state.dex.provider) {
                    SwapMainModule.OneInchProvider -> {
                        navController.slideFromBottomForResult(
                            R.id.oneinchSettingsFragment,
                            OneInchSettingsFragment.Input(
                                state.dex,
                                state.recipient,
                                state.slippage,
                            ),
                            onResult
                        )
                    }

                    SwapMainModule.UniswapV3Provider -> {
                        navController.slideFromBottomForResult(
                            R.id.uniswapSettingsFragment,
                            UniswapSettingsFragment.Input(
                                dex = state.dex,
                                address = state.recipient,
                                slippage = state.slippage,
                                ttlEnabled = false,
                            ),
                            onResult
                        )
                    }

                    SwapMainModule.PancakeSwapV3Provider -> {
                        navController.slideFromBottomForResult(
                            R.id.uniswapSettingsFragment,
                            UniswapSettingsFragment.Input(
                                dex = state.dex,
                                address = state.recipient,
                                slippage = state.slippage,
                                ttlEnabled = false,
                            ),
                            onResult
                        )
                    }

                    else -> {
                        navController.slideFromBottomForResult(
                            R.id.uniswapSettingsFragment,
                            UniswapSettingsFragment.Input(
                                dex = state.dex,
                                address = state.recipient,
                                slippage = state.slippage,
                                ttlEnabled = true,
                                ttl = state.ttl,
                            ),
                            onResult
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun BottomSheetProviderSelector(
    items: List<SwapMainModule.ProviderViewItem>,
    onSelect: (SwapMainModule.ISwapProvider) -> Unit,
    onCloseClick: () -> Unit
) {
    val context = LocalContext.current
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_swap_24),
        title = stringResource(R.string.Swap_SelectSwapProvider_Title),
        onCloseClick = onCloseClick,
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob)
    ) {
        Spacer(Modifier.height(12.dp))
        CellUniversalLawrenceSection(items, showFrame = true) { item ->
            RowUniversal(
                onClick = {
                    onSelect.invoke(item.provider)
                    onCloseClick.invoke()
                },
            ) {
                Image(
                    modifier = Modifier.padding(horizontal = 16.dp).size(32.dp),
                    painter = painterResource(
                        id = getDrawableResource(item.provider.id, context)
                            ?: R.drawable.coin_placeholder
                    ),
                    contentDescription = null
                )
                body_leah(
                    modifier = Modifier.weight(1f),
                    text = item.provider.title
                )
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.selected) {
                        Icon(
                            painter = painterResource(R.drawable.ic_checkmark_20),
                            tint = ComposeAppTheme.colors.jacob,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(44.dp))
    }
}

@Composable
fun PriceImpact(
    priceImpact: SwapMainModule.PriceImpactViewItem,
    navController: NavController
) {
    Row(modifier = Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        val infoTitle = stringResource(id = R.string.SwapInfo_PriceImpactTitle)
        val infoText = stringResource(id = R.string.SwapInfo_PriceImpactDescription)
        Row(
            modifier = Modifier.clickable(
                onClick = {
                    navController.slideFromBottom(
                        R.id.feeSettingsInfoDialog,
                        FeeSettingsInfoDialog.Input(infoTitle, infoText)
                    )
                },
                interactionSource = MutableInteractionSource(),
                indication = null
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            subhead2_grey(text = stringResource(R.string.Swap_PriceImpact))

            Image(
                modifier = Modifier.padding(horizontal = 8.dp),
                painter = painterResource(id = R.drawable.ic_info_20),
                contentDescription = ""
            )
        }
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
fun getPriceImpactColor(
    priceImpactLevel: PriceImpactLevel?
): Color {
    return when (priceImpactLevel) {
        PriceImpactLevel.Normal -> ComposeAppTheme.colors.jacob
        PriceImpactLevel.Warning,
        PriceImpactLevel.Forbidden -> ComposeAppTheme.colors.lucian

        else -> ComposeAppTheme.colors.grey
    }
}

private fun getDrawableResource(name: String, context: Context): Int? {
    val resourceId = context.resources.getIdentifier(name, "drawable", context.packageName)
    return if (resourceId == 0) null else resourceId
}
