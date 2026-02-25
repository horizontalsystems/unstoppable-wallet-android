package cash.p.terminal.modules.multiswap

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.entities.CoinValue
import cash.p.terminal.modules.multiswap.action.ActionCreate
import cash.p.terminal.modules.fee.FeeInfoSection
import cash.p.terminal.modules.fee.QuoteInfoRow
import cash.p.terminal.modules.multiswap.providers.IMultiSwapProvider
import cash.p.terminal.navigation.entity.SwapParams
import cash.p.terminal.modules.multiswap.settings.SwapTransactionSettingsScreen
import cash.p.terminal.ui.compose.Keyboard
import cash.p.terminal.ui.compose.components.CardsSwapInfo
import cash.p.terminal.ui.compose.components.CoinImage
import cash.p.terminal.ui.compose.components.HSRow
import cash.p.terminal.ui.compose.observeKeyboardState
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.ButtonSecondaryCircle
import cash.p.terminal.ui_compose.components.HFillSpacer
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.MenuItemTimeoutIndicator
import cash.p.terminal.ui_compose.components.TextImportantError
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_grey
import cash.p.terminal.ui_compose.components.headline1_grey
import cash.p.terminal.ui_compose.components.headline1_leah
import cash.p.terminal.ui_compose.components.micro_grey
import cash.p.terminal.ui_compose.components.subhead1_jacob
import cash.p.terminal.ui_compose.components.subhead1_leah
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.parcelable
import cash.p.terminal.ui_compose.theme.ColoredTextStyle
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.badge
import io.horizontalsystems.core.entities.Currency
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import cash.p.terminal.core.App
import io.horizontalsystems.core.toBigDecimalOrNullExt
import java.math.BigDecimal
import java.net.UnknownHostException
import cash.p.terminal.modules.managewallets.ManageWalletsModule
import cash.p.terminal.modules.managewallets.ManageWalletsViewModel
import cash.p.terminal.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import cash.p.terminal.modules.enablecoin.restoresettings.openRestoreSettingsDialog
import cash.p.terminal.ui_compose.components.HudHelper
import cash.p.terminal.ui_compose.components.InfoBottomSheet
import cash.p.terminal.core.composablePage
import cash.p.terminal.core.composablePopup
import kotlinx.serialization.Serializable
import cash.p.terminal.modules.multiswap.settings.SwapSettingsScreen
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SwapDeeplinkInput(val tokenOut: Token?) : Parcelable

class SwapFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val args = navController.currentBackStackEntry?.arguments
        val tokenIn: Token? = args?.parcelable(SwapParams.TOKEN_IN)
        val tokenOut: Token? = args?.parcelable(SwapParams.TOKEN_OUT)
            ?: args?.parcelable<SwapDeeplinkInput>("input")?.tokenOut
        SwapScreen(navController = navController, tokenIn = tokenIn, tokenOut = tokenOut)
    }
}

@Serializable
private object SwapMainPage

@Serializable
private data class SwapSelectCoinPage(val direction: SwapCoinDirection)

@Serializable
private object SwapSelectProviderPage

@Serializable
private object SwapConfirmPage

@Serializable
private object SwapSettingsPage

@Serializable
private object SwapTransactionSettingsPage

@Serializable
private enum class SwapCoinDirection { From, To }

@Composable
fun SwapScreen(navController: NavController, tokenIn: Token?, tokenOut: Token?) {
    val viewModel = viewModel<SwapViewModel>(
        factory = SwapViewModel.Factory(tokenIn, tokenOut)
    )
    val swapNavController = rememberNavController()

    NavHost(
        navController = swapNavController,
        startDestination = SwapMainPage
    ) {
        composable<SwapMainPage> {
            SwapMainScreen(
                fragmentNavController = navController,
                swapNavController = swapNavController,
                viewModel = viewModel
            )
        }
        composablePopup<SwapSelectCoinPage> { backStackEntry ->
            val args = backStackEntry.toRoute<SwapSelectCoinPage>()
            val direction = args.direction
            val initialToken = when (direction) {
                SwapCoinDirection.From -> viewModel.uiState.tokenIn
                SwapCoinDirection.To -> viewModel.uiState.tokenOut
            }
            val titleResId = when (direction) {
                SwapCoinDirection.From -> R.string.Swap_YouPay
                SwapCoinDirection.To -> R.string.Swap_YouGet
            }

            SwapSelectCoinScreen(
                navController = swapNavController,
                token = initialToken,
                title = stringResource(id = titleResId)
            ) { token ->
                when (direction) {
                    SwapCoinDirection.From -> viewModel.onSelectTokenIn(token)
                    SwapCoinDirection.To -> viewModel.onSelectTokenOut(token)
                }
                swapNavController.popBackStack()
            }
        }
        composablePopup<SwapSelectProviderPage> { backStackEntry ->
            val quotes = viewModel.uiState.quotes
            if (quotes.isEmpty()) {
                LaunchedEffect(Unit) {
                    swapNavController.navigateUp()
                }
                return@composablePopup
            }
            val selectProviderViewModel = viewModel<SwapSelectProviderViewModel>(
                viewModelStoreOwner = backStackEntry,
                factory = SwapSelectProviderViewModel.Factory(quotes)
            )
            SwapSelectProviderScreen(
                onClickClose = swapNavController::popBackStack,
                quotes = selectProviderViewModel.uiState.quoteViewItems,
                currentQuote = viewModel.uiState.quote,
                swapRates ={
                    HudHelper.vibrate(App.instance)
                    selectProviderViewModel.swapRates()
                },
                onSelectQuote = viewModel::onSelectQuote
            )
        }
        composablePage<SwapConfirmPage> {
            SwapConfirmScreen(
                fragmentNavController = navController,
                swapNavController = swapNavController,
                swapViewModel = viewModel,
                onOpenSettings = { swapNavController.navigate(SwapTransactionSettingsPage) }
            )
        }
        composablePage<SwapSettingsPage> {
            SwapSettingsScreen(navController = navController, swapViewModel = viewModel)
        }
        composablePage<SwapTransactionSettingsPage> {
            SwapTransactionSettingsScreen(navController = swapNavController)
        }
    }
}

@Composable
private fun SwapMainScreen(
    fragmentNavController: NavController,
    swapNavController: NavController,
    viewModel: SwapViewModel
) {
    val uiState = viewModel.uiState
    val view = LocalView.current
    val manageWalletsFactory = remember { ManageWalletsModule.Factory() }
    val restoreSettingsViewModel = viewModel<RestoreSettingsViewModel>(factory = manageWalletsFactory)
    val manageWalletsViewModel = viewModel<ManageWalletsViewModel>(factory = manageWalletsFactory)

    restoreSettingsViewModel.openTokenConfigure?.let { token ->
        fragmentNavController.openRestoreSettingsDialog(token, restoreSettingsViewModel)
    }

    LaunchedEffect(manageWalletsViewModel.errorMsg) {
        manageWalletsViewModel.errorMsg?.let {
            HudHelper.showErrorMessage(view, it)
        }
    }

    SwapScreenInner(
        uiState = uiState,
        timeRemainingProgress = { viewModel.timeRemainingProgress },
        onClickClose = fragmentNavController::navigateUp,
        onClickCoinFrom = {
            swapNavController.navigate(SwapSelectCoinPage(SwapCoinDirection.From))
        },
        onClickCoinTo = {
            swapNavController.navigate(SwapSelectCoinPage(SwapCoinDirection.To))
        },
        onSwitchPairs = viewModel::onSwitchPairs,
        onEnterAmount = viewModel::onEnterAmount,
        onEnterAmountPercentage = viewModel::onEnterAmountPercentage,
        onEnterFiatAmount = viewModel::onEnterFiatAmount,
        onClickProvider = {
            swapNavController.navigate(SwapSelectProviderPage)
        },
        onClickProviderSettings = {
            swapNavController.navigate(SwapSettingsPage)
        },
        onTimeout = viewModel::reQuote,
        onClickNext = {
            swapNavController.navigate(SwapConfirmPage)
        },
        onCreateMissingTokens = { tokens ->
            tokens.forEach { token ->
                manageWalletsViewModel.enable(token)
            }
            if (manageWalletsViewModel.showScanToAddButton) {
                manageWalletsViewModel.requestScanToAddTokens(false)
            }
            viewModel.createMissingTokens(tokens)
        },
        onActionStarted = {
            viewModel.onActionStarted()
        },
        onActionCompleted = {
            viewModel.onActionCompleted()
        },
        navController = fragmentNavController,
        onBalanceClicked = viewModel::toggleHideBalance
    )
}

@Composable
private fun SwapScreenInner(
    uiState: SwapUiState,
    timeRemainingProgress: () -> Float?,
    onClickClose: () -> Unit,
    onClickCoinFrom: () -> Unit,
    onClickCoinTo: () -> Unit,
    onSwitchPairs: () -> Unit,
    onEnterAmount: (BigDecimal?) -> Unit,
    onEnterFiatAmount: (BigDecimal?) -> Unit,
    onEnterAmountPercentage: (Int) -> Unit,
    onClickProvider: () -> Unit,
    onClickProviderSettings: () -> Unit,
    onTimeout: () -> Unit,
    onClickNext: () -> Unit,
    onCreateMissingTokens: (Set<Token>) -> Unit,
    onActionStarted: () -> Unit,
    onActionCompleted: () -> Unit,
    onBalanceClicked: () -> Unit,
    navController: NavController,
) {
    LifecycleResumeEffect(uiState.timeout) {
        if (uiState.timeout) {
            onTimeout.invoke()
        }

        onPauseOrDispose { }
    }

    val quote = uiState.quote

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.Swap),
                navigationIcon = {
                    HsBackButton(onClick = onClickClose)
                },
                menuItems = buildList {
                    timeRemainingProgress()?.let { progress ->
                        add(MenuItemTimeoutIndicator(progress))
                    }
                }
            )
        },
        containerColor = ComposeAppTheme.colors.tyler,
    ) {
        val focusManager = LocalFocusManager.current
        val keyboardState by observeKeyboardState()
        var amountInputHasFocus by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(it)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
            ) {
                VSpacer(height = 12.dp)
                SwapInput(
                    amountIn = uiState.amountIn,
                    fiatAmountIn = uiState.fiatAmountIn,
                    fiatAmountInputEnabled = uiState.fiatAmountInputEnabled,
                    onSwitchPairs = onSwitchPairs,
                    amountOut = quote?.amountOut,
                    fiatAmountOut = uiState.fiatAmountOut,
                    fiatPriceImpact = uiState.fiatPriceImpact,
                    fiatPriceImpactLevel = uiState.fiatPriceImpactLevel,
                    onValueChange = onEnterAmount,
                    onFiatValueChange = onEnterFiatAmount,
                    onClickCoinFrom = onClickCoinFrom,
                    onClickCoinTo = onClickCoinTo,
                    tokenIn = uiState.tokenIn,
                    tokenOut = uiState.tokenOut,
                    currency = uiState.currency,
                    onFocusChanged = {
                        amountInputHasFocus = it.hasFocus
                    },
                )

                VSpacer(height = 12.dp)

                when (val currentStep = uiState.currentStep) {
                    is SwapStep.InputRequired -> {
                        val title = when (currentStep.inputType) {
                            InputType.TokenIn -> stringResource(R.string.Swap_SelectTokenIn)
                            InputType.TokenOut -> stringResource(R.string.Swap_SelectTokenOut)
                            InputType.Amount -> stringResource(R.string.Swap_EnterAmount)
                        }

                        ButtonPrimaryYellow(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            title = title,
                            enabled = false,
                            onClick = {}
                        )
                    }

                    SwapStep.Quoting -> {
                        ButtonPrimaryYellow(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            title = stringResource(R.string.Swap_Quoting),
                            enabled = false,
                            loadingIndicator = true,
                            onClick = {}
                        )
                    }

                    is SwapStep.Error -> {
                        val errorText = when (val error = currentStep.error) {
                            SwapError.InsufficientBalanceFrom -> stringResource(id = R.string.Swap_ErrorInsufficientBalance)
                            is NoSupportedSwapProvider -> stringResource(id = R.string.Swap_ErrorNoProviders)
                            is SwapRouteNotFound -> stringResource(id = R.string.Swap_ErrorNoQuote)
                            is SwapDepositTooSmall -> stringResource(
                                id = R.string.swap_out_of_min_amount,
                                error.minValue.toPlainString()
                            )

                            is PriceImpactTooHigh -> stringResource(id = R.string.Swap_ErrorHighPriceImpact)
                            is UnknownHostException -> stringResource(id = R.string.Hud_Text_NoInternet)
                            is WalletSyncing -> stringResource(id = R.string.Swap_ErrorWalletSyncing)
                            is WalletNotSynced -> stringResource(id = R.string.Swap_ErrorWalletNotSynced)
                            else -> error.message ?: error.javaClass.simpleName
                        }

                        ButtonPrimaryYellow(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            title = errorText,
                            enabled = false,
                            onClick = {}
                        )
                    }

                    is SwapStep.ActionRequired -> {
                        val action = currentStep.action
                        val title = if (action.inProgress) {
                            action.getTitleInProgress()
                        } else {
                            action.getTitle()
                        }

                        ButtonPrimaryDefault(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            title = title,
                            enabled = !action.inProgress,
                            onClick = {
                                onActionStarted.invoke()
                                if (action is ActionCreate) {
                                    onCreateMissingTokens(action.tokensToAdd)
                                } else {
                                    action.execute(navController, onActionCompleted)
                                }
                            }
                        )
                    }

                    SwapStep.Proceed -> {
                        ButtonPrimaryYellow(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            title = stringResource(R.string.Swap_Proceed),
                            enabled = !uiState.insufficientFeeBalance,
                            onClick = onClickNext
                        )
                    }
                }

                VSpacer(height = 12.dp)

                val feeToken = uiState.feeToken
                val networkFee = uiState.networkFee
                FeeInfoSection(
                    tokenIn = uiState.tokenIn,
                    displayBalance = uiState.displayBalance,
                    balanceHidden = uiState.balanceHidden,
                    feeToken = feeToken,
                    feeCoinBalance = uiState.feeCoinBalance,
                    feePrimary = if (feeToken != null && networkFee != null) {
                        CoinValue(feeToken, networkFee).getFormattedFull()
                    } else {
                        "---"
                    },
                    feeSecondary = uiState.networkFeeFiatAmount?.let {
                        App.numberFormatter.formatFiatFull(it, uiState.currency.symbol)
                    } ?: "",
                    insufficientFeeBalance = uiState.insufficientFeeBalance,
                    onBalanceClicked = onBalanceClicked,
                    feeTitle = stringResource(R.string.estimated_fee),
                )

                VSpacer(height = 12.dp)
                if (quote != null) {
                    CardsSwapInfo {
                        ProviderField(quote.provider, onClickProvider, onClickProviderSettings)
                        PriceField(quote.tokenIn, quote.tokenOut, quote.amountIn, quote.amountOut)
                        PriceImpactField(
                            uiState.priceImpact,
                            uiState.priceImpactLevel,
                        )
                        quote.fields.forEach {
                            it.GetContent(navController, false)
                        }
                    }
                }

                uiState.warningMessage?.let { warning ->
                    VSpacer(height = 12.dp)
                    TextImportantWarning(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = warning.getString(),
                        icon = R.drawable.ic_attention_20
                    )
                }

                if (uiState.error is PriceImpactTooHigh) {
                    VSpacer(height = 12.dp)
                    TextImportantError(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        icon = R.drawable.ic_attention_20,
                        title = stringResource(id = R.string.Swap_PriceImpact),
                        text = stringResource(
                            id = R.string.Swap_PriceImpactTooHigh,
                            uiState.error.providerTitle ?: ""
                        )
                    )
                } else if (uiState.currentStep is SwapStep.ActionRequired) {
                    uiState.currentStep.action.getDescription()?.let { actionDescription ->
                        VSpacer(height = 12.dp)
                        TextImportantWarning(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = actionDescription
                        )
                    }
                }

                VSpacer(height = 32.dp)
            }


            if (amountInputHasFocus && keyboardState == Keyboard.Opened) {
                val hasNonZeroBalance =
                    uiState.availableBalance != null && uiState.availableBalance > BigDecimal.ZERO

                SuggestionsBar(
                    modifier = Modifier
                        .imePadding()
                        .align(Alignment.BottomCenter),
                    onDelete = {
                        onEnterAmount.invoke(null)
                    },
                    onSelect = {
                        focusManager.clearFocus()
                        onEnterAmountPercentage(it)
                    },
                    selectEnabled = hasNonZeroBalance,
                    deleteEnabled = uiState.amountIn != null,
                )
            }
        }
    }
}

@Composable
fun PriceImpactField(
    priceImpact: BigDecimal?,
    priceImpactLevel: PriceImpactLevel?,
    borderTop: Boolean = true
) {
    if (priceImpact == null || priceImpactLevel == null) return

    val infoTitle = stringResource(id = R.string.SwapInfo_PriceImpactTitle)
    val infoText = stringResource(id = R.string.SwapInfo_PriceImpactDescription)
    var showInfoDialog by remember { mutableStateOf(false) }

    QuoteInfoRow(
        borderTop = borderTop,
        title = {
            subhead2_grey(text = stringResource(R.string.Swap_PriceImpact))

            Image(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable(
                        onClick = { showInfoDialog = true },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ),
                painter = painterResource(id = R.drawable.ic_info_20),
                contentDescription = ""
            )
        },
        value = {
            Text(
                text = stringResource(
                    R.string.Swap_Percent,
                    priceImpact.toPlainString()
                ),
                style = ComposeAppTheme.typography.subhead2,
                color = getPriceImpactColor(priceImpactLevel),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )

    if (showInfoDialog) {
        InfoBottomSheet(
            title = infoTitle,
            text = infoText,
            onDismiss = { showInfoDialog = false }
        )
    }
}

@Composable
private fun ProviderField(
    swapProvider: IMultiSwapProvider,
    onClickProvider: () -> Unit,
    onClickProviderSettings: () -> Unit,
) {
    HSRow(
        modifier = Modifier
            .height(40.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        borderBottom = true,
    ) {
        Selector(
            icon = {
                Image(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(swapProvider.icon),
                    contentDescription = null
                )
            },
            text = {
                subhead1_leah(text = swapProvider.title)
            },
            onClickSelect = onClickProvider
        )
        HFillSpacer(minWidth = 16.dp)
        Icon(
            modifier = Modifier.clickable(
                onClick = onClickProviderSettings
            ),
            painter = painterResource(R.drawable.ic_manage_2),
            contentDescription = "",
            tint = ComposeAppTheme.colors.grey
        )
    }
}

@Composable
fun PriceField(tokenIn: Token, tokenOut: Token, amountIn: BigDecimal, amountOut: BigDecimal) {
    if (amountIn <= BigDecimal.ZERO || amountOut <= BigDecimal.ZERO) return

    var showRegularPrice by remember { mutableStateOf(true) }
    val swapPriceUIHelper = SwapPriceUIHelper(tokenIn, tokenOut, amountIn, amountOut)

    QuoteInfoRow(
        title = {
            subhead2_grey(text = stringResource(R.string.Swap_Price))
        },
        value = {
            Row(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            showRegularPrice = !showRegularPrice
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                subhead2_leah(
                    text = if (showRegularPrice) {
                        swapPriceUIHelper.priceStr
                    } else {
                        swapPriceUIHelper.priceInvStr
                    }
                )
                HSpacer(width = 8.dp)
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_swap3_20),
                    contentDescription = "invert price",
                    tint = ComposeAppTheme.colors.grey
                )
            }
        }
    )
}

@Composable
private fun SwapInput(
    amountIn: BigDecimal?,
    fiatAmountIn: BigDecimal?,
    fiatAmountInputEnabled: Boolean,
    onSwitchPairs: () -> Unit,
    amountOut: BigDecimal?,
    fiatAmountOut: BigDecimal?,
    fiatPriceImpact: BigDecimal?,
    fiatPriceImpactLevel: PriceImpactLevel?,
    onValueChange: (BigDecimal?) -> Unit,
    onFiatValueChange: (BigDecimal?) -> Unit,
    onClickCoinFrom: () -> Unit,
    onClickCoinTo: () -> Unit,
    tokenIn: Token?,
    tokenOut: Token?,
    currency: Currency,
    onFocusChanged: (FocusState) -> Unit,
) {
    Box(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(ComposeAppTheme.colors.lawrence)
        ) {
            SwapCoinInputIn(
                coinAmount = amountIn,
                fiatAmount = fiatAmountIn,
                currency = currency,
                onValueChange = onValueChange,
                onFiatValueChange = onFiatValueChange,
                fiatAmountInputEnabled = fiatAmountInputEnabled,
                token = tokenIn,
                onClickCoin = onClickCoinFrom,
                onFocusChanged = onFocusChanged
            )
            SwapCoinInputTo(
                coinAmount = amountOut,
                fiatAmount = fiatAmountOut,
                fiatPriceImpact = fiatPriceImpact,
                fiatPriceImpactLevel = fiatPriceImpactLevel,
                currency = currency,
                token = tokenOut,
                onClickCoin = onClickCoinTo
            )
        }
        Divider(
            modifier = Modifier.align(Alignment.Center),
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10
        )
        ButtonSecondaryCircle(
            modifier = Modifier.align(Alignment.Center),
            icon = R.drawable.ic_arrow_down_20,
            onClick = onSwitchPairs
        )
    }
}

@Composable
private fun SwapCoinInputIn(
    coinAmount: BigDecimal?,
    fiatAmount: BigDecimal?,
    currency: Currency,
    onValueChange: (BigDecimal?) -> Unit,
    onFiatValueChange: (BigDecimal?) -> Unit,
    fiatAmountInputEnabled: Boolean,
    token: Token?,
    onClickCoin: () -> Unit,
    onFocusChanged: (FocusState) -> Unit,
) {
    Row(
        modifier = Modifier
            .onFocusChanged(onFocusChanged)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AmountInput(
                value = coinAmount,
                onValueChange = onValueChange
            )
            VSpacer(height = 3.dp)
            FiatAmountInput(
                value = fiatAmount,
                currency = currency,
                onValueChange = onFiatValueChange,
                enabled = fiatAmountInputEnabled
            )
        }
        HSpacer(width = 8.dp)
        CoinSelector(token, onClickCoin)
    }
}

@Composable
private fun SwapCoinInputTo(
    coinAmount: BigDecimal?,
    fiatAmount: BigDecimal?,
    fiatPriceImpact: BigDecimal?,
    fiatPriceImpactLevel: PriceImpactLevel?,
    currency: Currency,
    token: Token?,
    onClickCoin: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (coinAmount == null) {
                headline1_grey(text = "0")
            } else {
                headline1_leah(
                    text = coinAmount.toPlainString(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            VSpacer(height = 3.dp)
            if (fiatAmount == null) {
                body_grey(text = "${currency.symbol}0")
            } else {
                Row {
                    body_grey(text = "${currency.symbol}${fiatAmount.toPlainString()}")
                    fiatPriceImpact?.let { diff ->
                        HSpacer(width = 4.dp)
                        Text(
                            text = stringResource(
                                R.string.Swap_FiatPriceImpact,
                                diff.toPlainString()
                            ),
                            style = ComposeAppTheme.typography.body,
                            color = getPriceImpactColor(fiatPriceImpactLevel),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        HSpacer(width = 8.dp)
        CoinSelector(token, onClickCoin)
    }
}

@Composable
private fun CoinSelector(
    token: Token?,
    onClickCoin: () -> Unit,
) {
    Selector(
        icon = {
            CoinImage(
                token = token,
                modifier = Modifier.size(32.dp)
            )
        },
        text = {
            if (token != null) {
                Column {
                    subhead1_leah(text = token.coin.code)
                    VSpacer(height = 1.dp)
                    micro_grey(
                        text = token.badge ?: stringResource(id = R.string.CoinPlatforms_Native)
                    )
                }
            } else {
                subhead1_jacob(text = stringResource(R.string.Swap_TokenSelectorTitle))
            }
        },
        onClickSelect = onClickCoin
    )
}

@Composable
private fun FiatAmountInput(
    value: BigDecimal?,
    currency: Currency,
    onValueChange: (BigDecimal?) -> Unit,
    enabled: Boolean,
) {
    var text by remember(value) {
        mutableStateOf(value?.toPlainString() ?: "")
    }
    Row {
        body_grey(text = currency.symbol)
        BasicTextField(
            modifier = Modifier.fillMaxWidth(),
            value = text,
            onValueChange = {
                try {
                    val amount = if (it.isBlank()) {
                        null
                    } else {
                        it.toBigDecimalOrNullExt()
                    }
                    text = it
                    onValueChange.invoke(amount)
                } catch (e: Exception) {

                }
            },
            enabled = enabled,
            textStyle = ColoredTextStyle(
                color = ComposeAppTheme.colors.grey,
                textStyle = ComposeAppTheme.typography.body
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
            decorationBox = { innerTextField ->
                if (text.isEmpty()) {
                    body_grey(text = "0")
                }
                innerTextField()
            },
        )
    }
}

@Composable
private fun Selector(
    icon: @Composable() (RowScope.() -> Unit),
    text: @Composable() (RowScope.() -> Unit),
    onClickSelect: () -> Unit,
) {
    Row(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClickSelect,
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon.invoke(this)
        HSpacer(width = 8.dp)
        text.invoke(this)
        HSpacer(width = 8.dp)
        Icon(
            painter = painterResource(R.drawable.ic_arrow_big_down_20),
            contentDescription = "",
            tint = ComposeAppTheme.colors.grey
        )
    }
}

@Composable
private fun AmountInput(
    value: BigDecimal?,
    onValueChange: (BigDecimal?) -> Unit,
) {
    var amount by rememberSaveable {
        mutableStateOf(value)
    }

    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = amount?.toPlainString() ?: ""))
    }

    LaunchedEffect(value) {
        if (value?.stripTrailingZeros() != amount?.stripTrailingZeros()) {
            amount = value

            textFieldValue = TextFieldValue(text = amount?.toPlainString() ?: "")
        }
    }

    var setCursorToEndOnFocused by remember {
        mutableStateOf(false)
    }

    BasicTextField(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged {
                setCursorToEndOnFocused = it.isFocused

                if (!it.isFocused) {
                    textFieldValue = textFieldValue.copy(selection = TextRange.Zero)
                }
            },
        value = textFieldValue,
        onValueChange = { newValue ->
            try {
                val text = newValue.text
                amount = if (text.isBlank()) {
                    null
                } else {
                    text.toBigDecimalOrNullExt()
                }

                if (!setCursorToEndOnFocused) {
                    textFieldValue = newValue
                } else {
                    textFieldValue = newValue.copy(selection = TextRange(text.length))
                    setCursorToEndOnFocused = false
                }

                onValueChange.invoke(amount)
            } catch (e: Exception) {

            }
        },
        textStyle = ColoredTextStyle(
            color = ComposeAppTheme.colors.leah,
            textStyle = ComposeAppTheme.typography.headline1
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        ),
        cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
        decorationBox = { innerTextField ->
            if (textFieldValue.text.isEmpty()) {
                headline1_grey(text = "0")
            }
            innerTextField()
        },
    )
}

@Composable
fun getPriceImpactColor(priceImpactLevel: PriceImpactLevel?): Color {
    return when (priceImpactLevel) {
        PriceImpactLevel.Warning -> ComposeAppTheme.colors.lucian
        PriceImpactLevel.Good -> ComposeAppTheme.colors.remus
        else -> ComposeAppTheme.colors.grey
    }
}

@Preview(showBackground = true)
@Composable
private fun SwapCoinInputToPreview() {
    ComposeAppTheme {
        Column {
            SwapCoinInputTo(
                coinAmount = BigDecimal("0.12345678"),
                fiatAmount = BigDecimal("1234.56"),
                fiatPriceImpact = BigDecimal("1.23"),
                fiatPriceImpactLevel = PriceImpactLevel.Normal,
                currency = Currency("usd", "$", 6, 0),
                token = null,
                onClickCoin = {}
            )
            SwapCoinInputTo(
                coinAmount = BigDecimal("0.12345678"),
                fiatAmount = BigDecimal("1234.56"),
                fiatPriceImpact = BigDecimal("1.23"),
                fiatPriceImpactLevel = PriceImpactLevel.Good,
                currency = Currency("usd", "$", 6, 0),
                token = null,
                onClickCoin = {}
            )
            SwapCoinInputTo(
                coinAmount = BigDecimal("0.12345678"),
                fiatAmount = BigDecimal("1234.56"),
                fiatPriceImpact = BigDecimal("1.23"),
                fiatPriceImpactLevel = PriceImpactLevel.Warning,
                currency = Currency("usd", "$", 6, 0),
                token = null,
                onClickCoin = {}
            )
        }
    }
}
