package cash.p.terminal.modules.swapxxx

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.badge
import cash.p.terminal.core.iconPlaceholder
import cash.p.terminal.core.imageUrl
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.core.slideFromBottomForResult
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.entities.CoinValue
import cash.p.terminal.entities.Currency
import cash.p.terminal.modules.swap.SwapMainModule
import cash.p.terminal.modules.swap.getPriceImpactColor
import cash.p.terminal.modules.swap.ui.SuggestionsBar
import cash.p.terminal.modules.swapxxx.providers.ISwapXxxProvider
import cash.p.terminal.ui.compose.ColoredTextStyle
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.Keyboard
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellowWithSpinner
import cash.p.terminal.ui.compose.components.ButtonSecondaryCircle
import cash.p.terminal.ui.compose.components.CardsSwapInfo
import cash.p.terminal.ui.compose.components.CoinImage
import cash.p.terminal.ui.compose.components.HFillSpacer
import cash.p.terminal.ui.compose.components.HSRow
import cash.p.terminal.ui.compose.components.HSpacer
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.body_grey
import cash.p.terminal.ui.compose.components.cell.CellUniversal
import cash.p.terminal.ui.compose.components.headline1_grey
import cash.p.terminal.ui.compose.components.headline1_leah
import cash.p.terminal.ui.compose.components.micro_grey
import cash.p.terminal.ui.compose.components.subhead1_jacob
import cash.p.terminal.ui.compose.components.subhead1_leah
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.compose.components.subhead2_leah
import cash.p.terminal.ui.compose.components.subhead2_lucian
import cash.p.terminal.ui.compose.observeKeyboardState
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

class SwapFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SwapScreen(navController)
    }
}

@Composable
fun SwapScreen(navController: NavController) {
    val currentBackStackEntry = remember { navController.currentBackStackEntry }
    val viewModel = viewModel<SwapViewModel>(
        viewModelStoreOwner = currentBackStackEntry!!,
        factory = SwapViewModel.Factory()
    )
    val uiState = viewModel.uiState

    val selectToken = { onResult: (Token) -> Unit ->
        navController.slideFromBottomForResult(R.id.swapSelectCoinFragment, onResult = onResult)
    }

    SwapScreenInner(
        uiState = uiState,
        onClickClose = navController::popBackStack,
        onClickCoinFrom = {
            selectToken {
                viewModel.onSelectTokenIn(it)
            }
        },
        onClickCoinTo = {
            selectToken {
                viewModel.onSelectTokenOut(it)
            }
        },
        onSwitchPairs = viewModel::onSwitchPairs,
        onEnterAmount = viewModel::onEnterAmount,
        onEnterAmountPercentage = viewModel::onEnterAmountPercentage,
        onEnterFiatAmount = viewModel::onEnterFiatAmount,
        onClickProvider = {
            navController.slideFromBottom(R.id.swapSelectProvider)
        },
        onClickProviderSettings = {
            navController.slideFromRight(R.id.swapSettings)
        },
        onClickNext = {
            navController.slideFromRight(R.id.swapConfirm)
        }
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
    onClickNext: () -> Unit,
) {
    val quote = uiState.quote

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.Swap),
                navigationIcon = {
                    HsBackButton(onClick = onClickClose)
                }
            )
        },
        backgroundColor = ComposeAppTheme.colors.tyler,
    ) {
        val focusManager = LocalFocusManager.current
        val keyboardState by observeKeyboardState()
        var showSuggestions by remember { mutableStateOf(false) }

        Box(modifier = Modifier
            .padding(it)
            .fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(it)
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
                        showSuggestions = it.hasFocus
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
                            enabled = uiState.swapEnabled,
                            onClick = onClickNext
                        )
                    }

                    SwapStep.Quoting -> {
                        ButtonPrimaryYellowWithSpinner(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            title = stringResource(R.string.Alert_Loading),
                            enabled = false,
                            onClick = { /*TODO*/ }
                        )
                    }

                    SwapStep.Error -> TODO()
                    SwapStep.Proceed -> {
                        ButtonPrimaryYellow(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            title = stringResource(R.string.Swap_Proceed),
                            enabled = uiState.swapEnabled,
                            onClick = onClickNext
                        )
                    }
                }

                VSpacer(height = 12.dp)

                uiState.error?.let { error ->
                    VSpacer(height = 12.dp)
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(12.dp)),
                    ) {
                        QuoteInfoRow(
                            title = {
                                val errorText = if (error is SwapRouteNotFound) {
                                    stringResource(id = R.string.Swap_SwapRouteNotFound)
                                } else {
                                    error.javaClass.simpleName
                                }
                                subhead2_lucian(text = errorText)
                            },
                            value = {
                            }
                        )
                    }
                }

                VSpacer(height = 12.dp)
                if (quote != null) {
                    CardsSwapInfo {
                        ProviderField(quote.provider, onClickProvider, onClickProviderSettings)
                        PriceField(quote.tokenIn, quote.tokenOut, quote.amountIn, quote.amountOut)
                        quote.fields.forEach {
                            it.GetContent()
                        }
                        PriceImpactField(uiState.priceImpact, uiState.priceImpactLevel)
                    }
                } else {
                    CardsSwapInfo {
                        AvailableBalanceField(uiState.tokenIn, uiState.availableBalance)
                    }
                }

                VSpacer(height = 32.dp)
            }


            if (
                uiState.availableBalance != null &&
                uiState.availableBalance > BigDecimal.ZERO &&
                showSuggestions &&
                keyboardState == Keyboard.Opened
            ) {
                SuggestionsBar(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    focusManager.clearFocus()
                    onEnterAmountPercentage.invoke(it)
                }
            }
        }
    }
}

@Composable
private fun AvailableBalanceField(tokenIn: Token?, availableBalance: BigDecimal?) {
    QuoteInfoRow(
        title = {
            subhead2_grey(text = stringResource(R.string.Swap_AvailableBalance))
        },
        value = {
            val text = if (tokenIn != null && availableBalance != null) {
                CoinValue(tokenIn, availableBalance).getFormattedFull()
            } else {
                "-"
            }

            subhead2_leah(text = text)
        }
    )
}

@Composable
private fun PriceImpactField(
    priceImpact: BigDecimal?,
    priceImpactLevel: SwapMainModule.PriceImpactLevel?
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
//                            navController.slideFromBottom(
//                                R.id.feeSettingsInfoDialog,
//                                FeeSettingsInfoDialog.Input(infoTitle, infoText)
//                            )
                        },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    )
                ,
                painter = painterResource(id = R.drawable.ic_info_20),
                contentDescription = ""
            )
        },
        value = {
            Text(
                text = stringResource(R.string.Swap_Percent, (priceImpact * BigDecimal.valueOf(-1)).toPlainString()),
                style = ComposeAppTheme.typography.subhead2,
                color = getPriceImpactColor(priceImpactLevel),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@Composable
private fun ProviderField(
    swapProvider: ISwapXxxProvider,
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
        HFillSpacer(minWidth = 8.dp)
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
private fun PriceField(tokenIn: Token, tokenOut: Token, amountIn: BigDecimal, amountOut: BigDecimal) {
    var showRegularPrice by remember { mutableStateOf(true) }
    val swapPriceUIHelper = SwapPriceUIHelper(tokenIn, tokenOut, amountIn, amountOut)

    QuoteInfoRow(
        title = {
            subhead2_grey(text = stringResource(R.string.Swap_Price))
        },
        value = {
            subhead2_leah(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            showRegularPrice = !showRegularPrice
                        }
                    ),
                text = if (showRegularPrice) swapPriceUIHelper.priceStr else swapPriceUIHelper.priceInvStr
            )
            HSpacer(width = 8.dp)
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_swap3_20),
                contentDescription = "invert price",
                tint = ComposeAppTheme.colors.grey
            )
        }
    )
}

@Composable
fun QuoteInfoRow(
    title: @Composable() (RowScope.() -> Unit),
    value: @Composable() (RowScope.() -> Unit),
) {
    CellUniversal(borderTop = false) {
        title.invoke(this)
        HFillSpacer(minWidth = 8.dp)
        value.invoke(this)
    }
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
    fiatPriceImpactLevel: SwapMainModule.PriceImpactLevel?,
    onValueChange: (BigDecimal?) -> Unit,
    onFiatValueChange: (BigDecimal?) -> Unit,
    onClickCoinFrom: () -> Unit,
    onClickCoinTo: () -> Unit,
    tokenIn: Token?,
    tokenOut: Token?,
    currency: Currency,
    onFocusChanged: (FocusState) -> Unit,
) {
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(ComposeAppTheme.colors.lawrence)
                .padding()
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
    fiatPriceImpactLevel: SwapMainModule.PriceImpactLevel?,
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
                iconUrl = token?.coin?.imageUrl,
                placeholder = token?.iconPlaceholder,
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
    var textFieldValue by rememberSaveable(value, stateSaver = TextFieldValue.Saver) {
        val valueStr = value?.toPlainString()
        val cursorPosition = valueStr?.length ?: 0

        mutableStateOf(
            TextFieldValue(
                text = valueStr ?: "",
                selection = TextRange(cursorPosition)
            )
        )
    }

    var setCursorToEnd by remember {
        mutableStateOf(false)
    }

    BasicTextField(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged {
                setCursorToEnd = it.isFocused
            },
        value = textFieldValue,
        onValueChange = { newValue ->
            try {
                val text = newValue.text
                val amount = if (text.isBlank()) {
                    null
                } else {
                    text.toBigDecimal()
                }

                textFieldValue = if (setCursorToEnd) {
                    setCursorToEnd = false
                    newValue.copy(selection = TextRange(text.length))
                } else {
                    newValue
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
        cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
        decorationBox = { innerTextField ->
            if (textFieldValue.text.isEmpty()) {
                headline1_grey(text = "0")
            }
            innerTextField()
        },
    )
}
