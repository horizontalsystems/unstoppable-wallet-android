package io.horizontalsystems.bankwallet.modules.swapxxx

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromBottomForResult
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellowWithSpinner
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.HFillSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HSRow
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey50
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_lucian
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

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

    val selectToken = { onResult: (SwapMainModule.CoinBalanceItem) -> Unit ->
        navController.slideFromBottomForResult(R.id.selectSwapCoinDialog, onResult = onResult)
    }

    SwapScreenInner(
        uiState = uiState,
        onClickClose = navController::popBackStack,
        onClickCoinFrom = {
            selectToken {
                viewModel.onSelectTokenIn(it.token)
            }
        },
        onClickCoinTo = {
            selectToken {
                viewModel.onSelectTokenOut(it.token)
            }
        },
        onSwitchPairs = viewModel::onSwitchPairs,
        onEnterAmount = viewModel::onEnterAmount,
        onClickProvider = {
            navController.slideFromBottom(R.id.swapSelectProvider)
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
    onClickProvider: () -> Unit,
    onClickNext: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.Swap),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = onClickClose
                    )
                ),
            )
        },
        backgroundColor = ComposeAppTheme.colors.tyler,
    ) {
        Column(modifier = Modifier.padding(it)) {
            VSpacer(height = 12.dp)
            SwapInput(
                amountIn = uiState.amountIn,
                onSwitchPairs = onSwitchPairs,
                amountOut = uiState.quote?.quote?.amountOut,
                onValueChange = onEnterAmount,
                onClickCoinFrom = onClickCoinFrom,
                onClickCoinTo = onClickCoinTo,
                tokenIn = uiState.tokenIn,
                tokenOut = uiState.tokenOut
            )

            VSpacer(height = 12.dp)

            if (uiState.quoting) {
                ButtonPrimaryYellowWithSpinner(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    title = stringResource(R.string.Alert_Loading),
                    enabled = false,
                    onClick = { /*TODO*/ }
                )
            } else {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    title = stringResource(R.string.Swap_Proceed),
                    enabled = uiState.swapEnabled,
                    onClick = onClickNext
                )
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

            uiState.quote?.let { swapProviderQuote ->
                val swapProvider = swapProviderQuote.provider
                val quote = swapProviderQuote.quote

                VSpacer(height = 12.dp)
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(12.dp))
                        .padding(vertical = 2.dp),
                ) {
                    ProviderField(swapProvider, onClickProvider)
                    AvailableBalanceField(uiState.tokenIn, uiState.availableBalance)
                    PriceField(uiState.tokenIn, uiState.tokenOut, uiState.amountIn, quote.amountOut)
                    quote.fields.forEach {
                        it.GetContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun AvailableBalanceField(tokenIn: Token?, availableBalance: BigDecimal?) {
    if (tokenIn != null && availableBalance != null) {
        QuoteInfoRow(
            title = {
                subhead2_grey(text = stringResource(R.string.Swap_AvailableBalance))
            },
            value = {
                subhead2_leah(text = CoinValue(tokenIn, availableBalance).getFormattedFull())
            }
        )
    }
}

@Composable
private fun ProviderField(
    swapProvider: SwapMainModule.ISwapProvider,
    onClickProvider: () -> Unit,
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
            painter = painterResource(R.drawable.ic_manage_2),
            contentDescription = "",
            tint = ComposeAppTheme.colors.grey
        )
    }
}

@Composable
private fun PriceField(tokenIn: Token?, tokenOut: Token?, amountIn: BigDecimal?, amountOut: BigDecimal) {
    var showRegularPrice by remember { mutableStateOf(true) }
    if (tokenIn != null && tokenOut != null && amountIn != null) {
        val price = amountOut.divide(amountIn, tokenOut.decimals, RoundingMode.HALF_EVEN).stripTrailingZeros()
        val priceInv = BigDecimal.ONE.divide(price, tokenIn.decimals, RoundingMode.HALF_EVEN).stripTrailingZeros()

        val priceStr = "${CoinValue(tokenIn, BigDecimal.ONE).getFormattedFull()} = ${CoinValue(tokenOut, price).getFormattedFull()}"
        val priceInvStr = "${CoinValue(tokenOut, BigDecimal.ONE).getFormattedFull()} = ${CoinValue(tokenIn, priceInv).getFormattedFull()}"
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
                    text = if (showRegularPrice) priceStr else priceInvStr
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
}

@Composable
fun QuoteInfoRow(
    title: @Composable() (RowScope.() -> Unit),
    value: @Composable() (RowScope.() -> Unit),
) {
    Row(
        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        title.invoke(this)
        HFillSpacer(minWidth = 8.dp)
        value.invoke(this)
    }
}

@Composable
private fun SwapInput(
    amountIn: BigDecimal?,
    onSwitchPairs: () -> Unit,
    amountOut: BigDecimal?,
    onValueChange: (BigDecimal?) -> Unit,
    onClickCoinFrom: () -> Unit,
    onClickCoinTo: () -> Unit,
    tokenIn: Token?,
    tokenOut: Token?,
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
            SwapCoinInput(
                coinAmount = amountIn,
                onValueChange = onValueChange,
                token = tokenIn,
                onClickCoin = onClickCoinFrom
            )
            SwapCoinInput(
                coinAmount = amountOut,
                onValueChange = { },
                enabled = false,
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
private fun SwapCoinInput(
    coinAmount: BigDecimal?,
    onValueChange: (BigDecimal?) -> Unit,
    enabled: Boolean = true,
    token: Token?,
    onClickCoin: () -> Unit,
) {
    val uuid = remember { UUID.randomUUID().toString() }
    val fiatViewModel = viewModel<FiatViewModel>(key = uuid, factory = FiatViewModel.Factory())
    val currencyAmount = fiatViewModel.fiatAmountString

    LaunchedEffect(token) {
        fiatViewModel.setCoin(token?.coin)
    }
    LaunchedEffect(coinAmount) {
        fiatViewModel.setAmount(coinAmount)
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AmountInput(coinAmount, onValueChange, enabled)
            VSpacer(height = 3.dp)
            if (currencyAmount != null) {
                body_grey(text = currencyAmount)
            } else {
                body_grey50(text = fiatViewModel.currencyAmountHint)
            }
        }
        HSpacer(width = 8.dp)
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
                    subhead1_leah(text = token.coin.code)
                } else {
                    subhead1_jacob(text = stringResource(R.string.Swap_TokenSelectorTitle))
                }
            },
            onClickSelect = onClickCoin
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
    enabled: Boolean,
) {
    var text by remember(value) {
        mutableStateOf(value?.toPlainString() ?: "")
    }
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
            color = ComposeAppTheme.colors.leah, textStyle = ComposeAppTheme.typography.headline1
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        ),
        cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
        decorationBox = { innerTextField ->
            if (text.isEmpty()) {
                headline1_grey(text = "0")
            }
            innerTextField()
        },
    )
}
