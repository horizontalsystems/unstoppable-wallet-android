package io.horizontalsystems.walletkit.modules.send.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.badge
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.modules.memo.HSMemoInput
import io.horizontalsystems.walletkit.modules.multiswap.AmountInput
import io.horizontalsystems.walletkit.modules.multiswap.FiatAmountInput
import io.horizontalsystems.walletkit.modules.multiswap.SuggestionsBar
import io.horizontalsystems.walletkit.modules.multiswap.SwapError
import io.horizontalsystems.walletkit.modules.multiswap.TokenNotEnabled
import io.horizontalsystems.walletkit.modules.multiswap.WalletNotSynced
import io.horizontalsystems.walletkit.modules.multiswap.WalletSyncing
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.send.AddressRiskySheet
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.Keyboard
import io.horizontalsystems.walletkit.ui.compose.observeKeyboardState
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.BadgeText
import io.horizontalsystems.walletkit.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.walletkit.ui.compose.components.CoinImage
import io.horizontalsystems.walletkit.ui.compose.components.HSpacer
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.headline2_leah
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.tabs.TabFolderItem
import io.horizontalsystems.walletkit.uiv3.components.tabs.TabsFolder
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

@Composable
fun SendV2Screen(
    navigation: HSNavigation,
    viewModel: SendViewModel,
) {
    val uiState = viewModel.uiState
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val keyboardState by observeKeyboardState()
    var amountInputHasFocus by remember { mutableStateOf(false) }

    // Opens over the risky-address sheet when that was shown; both are popped once the
    // transaction is sent.
    val proceed = {
        val address = uiState.address
        val amount = uiState.amount
        if (address != null && amount != null) {
            navigation.slideFromRight(
                SendV2ConfirmPage(
                    SendV2ConfirmPage.Input(
                        wallet = uiState.wallet,
                        amount = amount,
                        address = address,
                        memo = uiState.memo,
                    )
                )
            )
        }
    }
    val confirmRiskyAddress = navigation.slideFromBottomForResult<AddressRiskySheet.Result>(
        {
            AddressRiskySheet(
                AddressRiskySheet.Input(
                    alertText = Translator.getString(R.string.Send_RiskyAddress_AlertText)
                )
            )
        }
    ) {
        proceed()
    }

    val openAddress = navigation.slideFromRightForResult<SendAddressPage.Result>(
        { SendAddressPage(uiState.wallet, uiState.address?.hex) }
    ) {
        viewModel.onSelectAddress(it.address, it.risky)
    }

    HSScaffold(
        title = stringResource(R.string.Balance_Send),
        onBack = { navigation.removeLastOrNull() },
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.SendEvmSettings_Title),
                icon = R.drawable.manage_24,
                onClick = { },
            ),
        ),
    ) {
        val tabs = SendTab.entries
        val focusRequester = remember { FocusRequester() }

        Column(modifier = Modifier.fillMaxSize()) {
            TabsFolder(
                tabs = tabs.map { it.tabItem() },
                selectedIndex = tabs.indexOf(uiState.tab),
                onSelect = { viewModel.onSelectTab(tabs[it]) },
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(ComposeAppTheme.colors.lawrence)
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    AmountSection(
                        token = uiState.wallet.token,
                        amount = uiState.amount,
                        fiatAmount = uiState.fiatAmount,
                        fiatAmountInputEnabled = uiState.fiatAmountInputEnabled,
                        currency = uiState.currency,
                        availableBalance = uiState.availableBalance,
                        focusRequester = focusRequester,
                        onValueChange = viewModel::onEnterAmount,
                        onFiatValueChange = viewModel::onEnterFiatAmount,
                        onFocusChanged = { amountInputHasFocus = it },
                    )
                    SectionArrow()
                    AddressRow(
                        address = uiState.address,
                        onClick = openAddress,
                    )
                    HsDivider(modifier = Modifier.fillMaxWidth())
                    uiState.memoSupport?.let { memoSupport ->
                        VSpacer(16.dp)
                        HSMemoInput(
                            maxLength = memoSupport.maxLength,
                            memo = uiState.memo,
                            visibility = memoSupport.visibility,
                            onValueChange = viewModel::onEnterMemo,
                        )
                    }
                    VSpacer(32.dp)
                }

                val buttonTitle = when (val step = uiState.step) {
                    is SendStep.InputRequired -> when (step.inputType) {
                        SendInputType.Amount -> stringResource(R.string.Send_EnterAmount)
                        SendInputType.Address -> stringResource(R.string.Send_EnterAddress)
                    }

                    is SendStep.Error -> when (step.error) {
                        SwapError.InsufficientBalanceFrom -> stringResource(R.string.Swap_ErrorInsufficientBalance)
                        is TokenNotEnabled -> stringResource(R.string.Swap_ErrorTokenNotEnabled)
                        is WalletSyncing -> stringResource(R.string.Swap_ErrorWalletSyncing)
                        is WalletNotSynced -> stringResource(R.string.Swap_ErrorWalletNotSynced)
                        else -> step.error.message ?: step.error.javaClass.simpleName
                    }

                    SendStep.Proceed -> stringResource(R.string.Button_Next)
                }
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    title = buttonTitle,
                    enabled = uiState.step is SendStep.Proceed,
                    onClick = {
                        if (uiState.riskyAddress) {
                            keyboardController?.hide()
                            confirmRiskyAddress()
                        } else {
                            proceed()
                        }
                    },
                )
                if (amountInputHasFocus && keyboardState == Keyboard.Opened) {
                    val hasNonZeroBalance =
                        uiState.availableBalance != null && uiState.availableBalance > BigDecimal.ZERO
                    VSpacer(16.dp)
                    SuggestionsBar(
                        onDelete = { viewModel.onEnterAmount(null) },
                        onSelect = {
                            focusManager.clearFocus()
                            viewModel.onEnterAmountPercentage(it)
                        },
                        selectEnabled = hasNonZeroBalance,
                        deleteEnabled = uiState.amount != null,
                    )
                } else {
                    VSpacer(16.dp)
                }
            }
        }
    }
}

@Composable
private fun SendTab.tabItem() = when (this) {
    SendTab.Standard -> TabFolderItem(stringResource(R.string.Send_Tab_Standard))
    SendTab.Private -> TabFolderItem(
        title = stringResource(R.string.Send_Tab_Private),
        icon = R.drawable.ic_incognito_24,
        badge = true,
    )

    SendTab.CrossPay -> TabFolderItem(stringResource(R.string.Send_Tab_CrossPay))
}

@Composable
private fun AmountSection(
    token: Token,
    amount: BigDecimal?,
    fiatAmount: BigDecimal?,
    fiatAmountInputEnabled: Boolean,
    currency: Currency,
    availableBalance: BigDecimal?,
    focusRequester: FocusRequester,
    onValueChange: (BigDecimal?) -> Unit,
    onFiatValueChange: (BigDecimal?) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .onFocusChanged { onFocusChanged(it.hasFocus) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            availableBalance?.let {
                Text(
                    text = stringResource(
                        R.string.Send_Available,
                        App.numberFormatter.formatCoinFull(it, token.coin.code, token.decimals)
                    ),
                    style = ComposeAppTheme.typography.caption,
                    color = ComposeAppTheme.colors.ocean,
                )
            }
        }
        VSpacer(8.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoinImage(
                token = token,
                modifier = Modifier.size(40.dp)
            )
            HSpacer(16.dp)
            Column {
                headline2_leah(text = token.coin.code)
                VSpacer(5.dp)
                BadgeText(
                    text = token.badge ?: stringResource(R.string.CoinPlatforms_Native),
                    background = ComposeAppTheme.colors.blade,
                    textColor = ComposeAppTheme.colors.leah,
                )
            }
            HSpacer(8.dp)
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                AmountInput(
                    value = amount,
                    onValueChange = onValueChange,
                    focusRequester = focusRequester,
                )
                if (fiatAmountInputEnabled || fiatAmount != null) {
                    VSpacer(3.dp)
                    FiatAmountInput(
                        value = fiatAmount,
                        currency = currency,
                        onValueChange = onFiatValueChange,
                        enabled = fiatAmountInputEnabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionArrow() {
    Box(modifier = Modifier.fillMaxWidth()) {
        HsDivider(modifier = Modifier.align(Alignment.Center))
        Icon(
            modifier = Modifier
                .align(Alignment.Center)
                .background(ComposeAppTheme.colors.lawrence)
                .padding(horizontal = 8.dp)
                .size(20.dp),
            painter = painterResource(R.drawable.ic_arrow_down_20),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey,
        )
    }
}

@Composable
private fun AddressRow(
    address: Address?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(R.drawable.wallet_24),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey,
            )
        }
        HSpacer(16.dp)
        headline2_leah(
            modifier = Modifier.weight(1f, fill = false),
            text = address?.title ?: stringResource(R.string.Send_ToAddress),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        HSpacer(8.dp)
        Icon(
            painter = painterResource(R.drawable.arrow_s_down_20),
            contentDescription = null,
            tint = ComposeAppTheme.colors.leah,
        )
    }
}
