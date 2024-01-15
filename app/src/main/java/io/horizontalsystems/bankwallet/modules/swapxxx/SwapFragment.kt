package io.horizontalsystems.bankwallet.modules.swapxxx

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.ui.draw.scale
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
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal
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

    SwapScreenInner(
        uiState = uiState,
        onClickClose = navController::popBackStack,
        onClickCoinFrom = {
            val dex = SwapMainModule.Dex(
                blockchain = Blockchain(BlockchainType.Ethereum, "Ethereum", null),
                provider = SwapMainModule.OneInchProvider
            )
            navController.slideFromBottomForResult<SwapMainModule.CoinBalanceItem>(
                R.id.selectSwapCoinDialog,
                dex
            ) {
                viewModel.onSelectTokenIn(it.token)
            }
        },
        onClickCoinTo = {
            val dex = SwapMainModule.Dex(
                blockchain = Blockchain(BlockchainType.Ethereum, "Ethereum", null),
                provider = SwapMainModule.OneInchProvider
            )
            navController.slideFromBottomForResult<SwapMainModule.CoinBalanceItem>(
                R.id.selectSwapCoinDialog,
                dex
            ) {
                viewModel.onSelectTokenOut(it.token)
            }
        },
        onSwitchPairs = viewModel::onSwitchPairs,
        onEnterAmount = viewModel::onEnterAmount,
        onClickProvider = {
            navController.slideFromBottom(R.id.swapSelectProvider)
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

            var showRegularPrice by remember { mutableStateOf(true) }
            uiState.quote?.provider?.let { swapProvider ->
                VSpacer(height = 12.dp)
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(12.dp)),
                ) {
                    QuoteInfoRow(
                        title = {
                            subhead2_grey(text = stringResource(R.string.Swap_Provider))
                        },
                        value = {
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
                        }
                    )

                    uiState.prices?.let { (price, priceInverted) ->
                        QuoteInfoRow(
                            title = {
                                subhead2_grey(text = stringResource(R.string.Swap_Price))
                            },
                            value = {
                                subhead2_leah(
                                    modifier = Modifier
                                        .clickable {
                                            showRegularPrice = !showRegularPrice
                                        },
                                    text = if (showRegularPrice) price else priceInverted
                                )
                                HSpacer(width = 8.dp)
                                Box(modifier = Modifier.size(14.5.dp)) {
                                    val progress = remember { Animatable(1f) }
                                    LaunchedEffect(uiState) {
                                        progress.animateTo(
                                            targetValue = 0f,
                                            animationSpec = tween(uiState.quoteLifetime.toInt(), easing = LinearEasing),
                                        )
                                    }

                                    CircularProgressIndicator(
                                        progress = 1f,
                                        modifier = Modifier.size(14.5.dp),
                                        color = ComposeAppTheme.colors.steel20,
                                        strokeWidth = 1.5.dp
                                    )
                                    CircularProgressIndicator(
                                        progress = progress.value,
                                        modifier = Modifier
                                            .size(14.5.dp)
                                            .scale(scaleX = -1f, scaleY = 1f),
                                        color = ComposeAppTheme.colors.jacob,
                                        strokeWidth = 1.5.dp
                                    )
                                }

                            }
                        )
                    }
                }
            }

            VSpacer(height = 24.dp)
            if (uiState.calculating) {
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
                    onClick = { /*TODO*/ }
                )
            }
        }
    }
}

@Composable
private fun QuoteInfoRow(
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
