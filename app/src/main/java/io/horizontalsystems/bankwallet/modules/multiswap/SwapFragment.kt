package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromBottomForResult
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.slideFromRightForResult
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.evmfee.FeeSettingsInfoDialog
import io.horizontalsystems.bankwallet.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Keyboard
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.CardsSwapInfo
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.HFillSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HSRow
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItemTimeoutIndicator
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantError
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.micro_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.ui.compose.observeKeyboardState
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal
import java.net.UnknownHostException

class SwapFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SwapScreen(navController, navController.getInput())
    }
}

@Composable
fun SwapScreen(navController: NavController, tokenIn: Token?) {
    val currentBackStackEntry = remember { navController.currentBackStackEntry }
    val viewModel = viewModel<SwapViewModel>(
        viewModelStoreOwner = currentBackStackEntry!!,
        factory = SwapViewModel.Factory(tokenIn)
    )
    val uiState = viewModel.uiState
    val context = LocalContext.current

    SwapScreenInner(
        uiState = uiState,
        onClickClose = navController::popBackStack,
        onClickCoinFrom = {
            navController.slideFromBottomForResult<Token>(
                R.id.swapSelectCoinFragment,
                SwapSelectCoinFragment.Input(uiState.tokenOut, context.getString(R.string.Swap_YouPay))
            ) {
                viewModel.onSelectTokenIn(it)
            }
        },
        onClickCoinTo = {
            navController.slideFromBottomForResult<Token>(
                R.id.swapSelectCoinFragment,
                SwapSelectCoinFragment.Input(uiState.tokenIn, context.getString(R.string.Swap_YouGet))
            ) {
                viewModel.onSelectTokenOut(it)
            }
        },
        onSwitchPairs = viewModel::onSwitchPairs,
        onEnterAmount = viewModel::onEnterAmount,
        onEnterAmountPercentage = viewModel::onEnterAmountPercentage,
        onEnterFiatAmount = viewModel::onEnterFiatAmount,
        onClickProvider = {
            navController.slideFromBottom(R.id.swapSelectProvider)

            stat(page = StatPage.Swap, event = StatEvent.Open(StatPage.SwapProvider))
        },
        onClickProviderSettings = {
            navController.slideFromRight(R.id.swapSettings)

            stat(page = StatPage.Swap, event = StatEvent.Open(StatPage.SwapSettings))
        },
        onTimeout = viewModel::reQuote,
        onClickNext = {
            navController.slideFromRightForResult<SwapConfirmFragment.Result>(R.id.swapConfirm) {
                if (it.success) {
                    navController.popBackStack()
                }
            }

            stat(page = StatPage.Swap, event = StatEvent.Open(StatPage.SwapConfirmation))
        },
        onActionStarted = {
            viewModel.onActionStarted()
        },
        onActionCompleted = {
            viewModel.onActionCompleted()
        },
        navController = navController
    )
}

@Composable
private fun SwapScreenInner(
    uiState: SwapUiState,
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
    onActionStarted: () -> Unit,
    onActionCompleted: () -> Unit,
    navController: NavController,
) {
    LifecycleResumeEffect(uiState.timeout) {
        if (uiState.timeout) {
            onTimeout.invoke()
        }

        onPauseOrDispose { }
    }

    val quote = uiState.quote

    HSScaffold(
        title = stringResource(R.string.Swap),
        onBack = onClickClose,
        menuItems = buildList {
            uiState.timeRemainingProgress?.let { timeRemainingProgress ->
                add(
                    MenuItemTimeoutIndicator(timeRemainingProgress)
                )
            }
        }
    ) {
        val focusManager = LocalFocusManager.current
        val keyboardState by observeKeyboardState()
        var amountInputHasFocus by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
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
                VSpacer(height = 8.dp)
                AvailableBalanceField(uiState.tokenIn, uiState.availableBalance)

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

                    SwapStep.Initializing -> {
                        ButtonPrimaryYellow(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            title = stringResource(R.string.Swap_Initializing),
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
                            is PriceImpactTooHigh -> stringResource(id = R.string.Swap_ErrorHighPriceImpact)
                            is UnknownHostException -> stringResource(id = R.string.Hud_Text_NoInternet)
                            is TokenNotEnabled -> stringResource(id = R.string.Swap_ErrorTokenNotEnabled)
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
                                action.execute(navController, onActionCompleted)
                            }
                        )
                    }

                    SwapStep.Proceed -> {
                        ButtonPrimaryYellow(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            title = stringResource(R.string.Swap_Proceed),
                            onClick = onClickNext
                        )
                    }
                }

                VSpacer(height = 12.dp)
                if (quote != null) {
                    CardsSwapInfo {
                        ProviderField(quote.provider, onClickProvider, onClickProviderSettings)
                        PriceField(quote.tokenIn, quote.tokenOut, quote.amountIn, quote.amountOut, StatPage.Swap)
                        PriceImpactField(uiState.priceImpact, uiState.priceImpactLevel, navController)
                        quote.fields.forEach {
                            it.GetContent(navController, false)
                        }
                    }
                }

                if (uiState.error is PriceImpactTooHigh) {
                    VSpacer(height = 12.dp)
                    TextImportantError(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        icon = R.drawable.ic_attention_20,
                        title = stringResource(id = R.string.Swap_PriceImpact),
                        text = stringResource(id = R.string.Swap_PriceImpactTooHigh, uiState.error.providerTitle ?: "")
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

                if (uiState.cautions.isNotEmpty()) {
                    Cautions(cautions = uiState.cautions)
                }

                VSpacer(height = 32.dp)
            }


            if (amountInputHasFocus && keyboardState == Keyboard.Opened) {
                val hasNonZeroBalance =
                    uiState.availableBalance != null && uiState.availableBalance > BigDecimal.ZERO
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        // Add IME (keyboard) padding to push content above keyboard
                        .windowInsetsPadding(
                            WindowInsets.ime
                        )
                        .systemBarsPadding()
                ) {
                    SuggestionsBar(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        onDelete = {
                            onEnterAmount.invoke(null)
                        },
                        onSelect = {
                            focusManager.clearFocus()
                            onEnterAmountPercentage.invoke(it)
                        },
                        selectEnabled = hasNonZeroBalance,
                        deleteEnabled = uiState.amountIn != null,
                    )
                }
            }
        }
    }
}

@Composable
private fun AvailableBalanceField(tokenIn: Token?, availableBalance: BigDecimal?) {
    Row(modifier = Modifier.padding(horizontal = 32.dp)) {
        caption_grey(text = stringResource(R.string.Swap_AvailableBalance))
        val text = if (tokenIn != null && availableBalance != null) {
            CoinValue(tokenIn, availableBalance).getFormattedFull()
        } else {
            "---"
        }
        Spacer(modifier = Modifier.weight(1f))
        caption_grey(text = text)
    }
}

@Composable
fun PriceImpactField(
    priceImpact: BigDecimal?,
    priceImpactLevel: PriceImpactLevel?,
    navController: NavController
) {
    if (priceImpact == null || priceImpactLevel == null) return

    val infoTitle = stringResource(id = R.string.SwapInfo_PriceImpactTitle)
    val infoText = stringResource(id = R.string.SwapInfo_PriceImpactDescription)

    QuoteInfoRow(
        title = {
            subhead2_grey(text = stringResource(R.string.Swap_PriceImpact))

            Image(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable(
                        onClick = {
                            navController.slideFromBottom(
                                R.id.feeSettingsInfoDialog,
                                FeeSettingsInfoDialog.Input(infoTitle, infoText)
                            )
                        },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ),
                painter = painterResource(id = R.drawable.ic_info_20),
                contentDescription = ""
            )
        },
        value = {
            Text(
                text = stringResource(R.string.Swap_Percent, (priceImpact * BigDecimal.valueOf(-1)).toPlainString()),
                style = ComposeAppTheme.typography.subheadR,
                color = getPriceImpactColor(priceImpactLevel),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
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
fun PriceField(tokenIn: Token, tokenOut: Token, amountIn: BigDecimal, amountOut: BigDecimal, statPage: StatPage) {
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

                            stat(page = statPage, event = StatEvent.TogglePrice)
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
        HsDivider(modifier = Modifier.align(Alignment.Center))
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
                            text = stringResource(R.string.Swap_FiatPriceImpact, diff.toPlainString()),
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
                    micro_grey(text = token.badge ?: stringResource(id = R.string.CoinPlatforms_Native))
                }
            } else {
                subhead1_jacob(text = stringResource(R.string.Swap_TokenSelectorTitle))
            }
        },
        onClickSelect = onClickCoin
    )
}

@Composable
fun FiatAmountInput(
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
                        it.toBigDecimal()
                    }
                    text = it
                    onValueChange.invoke(amount)
                } catch (e: Exception) {

                }
            },
            enabled = enabled,
            textStyle = ColoredTextStyle(
                color = ComposeAppTheme.colors.grey, textStyle = ComposeAppTheme.typography.body
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            cursorBrush = SolidColor(ComposeAppTheme.colors.leah),
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
fun AmountInput(
    value: BigDecimal?,
    onValueChange: (BigDecimal?) -> Unit,
    focusRequester: FocusRequester = FocusRequester()
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
            .focusRequester(focusRequester)
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
                    text.toBigDecimal()
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
            color = ComposeAppTheme.colors.leah, textStyle = ComposeAppTheme.typography.headline1
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        ),
        cursorBrush = SolidColor(ComposeAppTheme.colors.leah),
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
        PriceImpactLevel.Normal -> ComposeAppTheme.colors.grey
        PriceImpactLevel.Warning -> ComposeAppTheme.colors.jacob
        PriceImpactLevel.Forbidden -> ComposeAppTheme.colors.lucian

        else -> ComposeAppTheme.colors.grey
    }
}
