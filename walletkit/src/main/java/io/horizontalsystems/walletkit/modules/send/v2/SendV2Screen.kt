package io.horizontalsystems.walletkit.modules.send.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.core.shorten
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.modules.crosspay.CrossPayTabBody
import io.horizontalsystems.walletkit.modules.crosspay.CrossPayTabViewModel
import io.horizontalsystems.walletkit.modules.memo.HSMemoInput
import io.horizontalsystems.walletkit.modules.multiswap.SwapError
import io.horizontalsystems.walletkit.modules.multiswap.TokenNotEnabled
import io.horizontalsystems.walletkit.modules.multiswap.WalletNotSynced
import io.horizontalsystems.walletkit.modules.multiswap.WalletSyncing
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.privatesend.PrivateSendConfirmationPage
import io.horizontalsystems.walletkit.modules.send.AddressRiskySheet
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.HSpacer
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.headline1_leah
import io.horizontalsystems.walletkit.uiv3.components.BoxBordered
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.bottombars.ButtonsGroupVertical
import io.horizontalsystems.walletkit.uiv3.components.controls.AvailableBalanceRow
import io.horizontalsystems.walletkit.uiv3.components.controls.HSButton
import io.horizontalsystems.walletkit.uiv3.components.controls.TokenAmountInput
import io.horizontalsystems.walletkit.uiv3.components.tabs.TabFolderItem
import io.horizontalsystems.walletkit.uiv3.components.tabs.TabsFolder
import java.net.UnknownHostException
import kotlin.reflect.KClass

@Composable
fun SendV2Screen(
    navigation: HSNavigation,
    viewModel: SendViewModel,
    sendEntryPointDestId: KClass<out HSPage>,
    purpose: SendV2Page.Purpose = SendV2Page.Purpose.Transfer(),
) {
    val uiState = viewModel.uiState
    val title = (purpose as? SendV2Page.Purpose.Donation)?.title
    val chainPlugin = remember { ChainRegistry[uiState.wallet.token.blockchainType] }
    val hasSettings = remember { chainPlugin?.sendSettingsPage(uiState.wallet, null) != null }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Opens over the risky-address sheet when that was shown; both are popped once the
    // transaction is sent. The Private tab confirms through the provider order flow, which
    // has its own page; the memo cannot travel with a deposit and is not passed.
    val proceed = {
        val address = uiState.address
        val amount = uiState.amount
        if (address != null && amount != null) {
            val page = if (uiState.isPrivateSend) {
                PrivateSendConfirmationPage(
                    PrivateSendConfirmationPage.Input(
                        wallet = uiState.wallet,
                        recipient = address.hex,
                        amount = amount,
                        sendEntryPointDestId = sendEntryPointDestId,
                    )
                )
            } else {
                SendV2ConfirmPage(
                    SendV2ConfirmPage.Input(
                        wallet = uiState.wallet,
                        amount = amount,
                        address = address,
                        memo = uiState.memo,
                        extraInput = uiState.extraInputValue,
                        sendEntryPointDestId = sendEntryPointDestId,
                    )
                )
            }
            navigation.slideFromRight(page)
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

    val openAddress = navigation.slideFromBottomForResult<SendAddressPage.Result>(
        { SendAddressPage(uiState.wallet.token, uiState.address?.hex) }
    ) {
        viewModel.onSelectAddress(it.address, it.risky)
    }

    HSScaffold(
        title = title ?: stringResource(R.string.Send_Title, uiState.wallet.coin.code),
        onBack = { navigation.removeLastOrNull() },
        menuItems = if (hasSettings) {
            listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.SendEvmSettings_Title),
                    icon = R.drawable.manage_24,
                    onClick = {
                        chainPlugin?.sendSettingsPage(uiState.wallet, uiState.address?.hex)
                            ?.let { navigation.slideFromRight(it) }
                    },
                )
            )
        } else {
            listOf()
        },
    ) {
        val tabs = SendTab.entries.filter {
            when (it) {
                SendTab.Standard -> true
                SendTab.Private -> uiState.privateSendSupported
                SendTab.CrossPay -> purpose is SendV2Page.Purpose.Transfer
            }
        }
        val focusRequester = remember { FocusRequester() }

        Column(modifier = Modifier.fillMaxSize()) {
            TabsFolder(
                tabs = tabs.map { it.tabItem() },
                selectedIndex = tabs.indexOf(uiState.tab).coerceAtLeast(0),
                onSelect = {
                    // The amount field keeps its focus across Standard and Private, so the
                    // keyboard would otherwise stay up; a tab switch always closes it.
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    viewModel.onSelectTab(tabs[it])
                },
            )
            if (uiState.tab == SendTab.CrossPay) {
                val crossPayViewModel = viewModel<CrossPayTabViewModel>(
                    factory = CrossPayTabViewModel.Factory(uiState.wallet)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(ComposeAppTheme.colors.lawrence)
                        .imePadding()
                ) {
                    CrossPayTabBody(
                        navigation = navigation,
                        viewModel = crossPayViewModel,
                    )
                }
                return@Column
            }
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
                    val step = uiState.step
                    AvailableBalanceRow(
                        balanceToken = uiState.wallet.token,
                        availableBalance = uiState.availableBalance,
                        // The line doubles as the 100% shortcut whenever that percent is offered.
                        onAvailableBalanceClick = if (100 in uiState.disabledPercents) {
                            null
                        } else {
                            {
                                focusManager.clearFocus()
                                viewModel.onEnterAmountPercentage(100)
                            }
                        },
                        onPercentClick = {
                            focusManager.clearFocus()
                            viewModel.onEnterAmountPercentage(it)
                        },
                        onClearClick = {
                            viewModel.onEnterAmount(null)
                        },
                        showClear = uiState.amount != null
                    )
                    Box {
                        Column {
                            TokenAmountInput(
                                token = uiState.wallet.token,
                                amount = uiState.amount,
                                fiatAmount = uiState.fiatAmount,
                                fiatAmountInputEnabled = uiState.fiatAmountInputEnabled,
                                currency = uiState.currency,
                                focusRequester = focusRequester,
                                onValueChange = viewModel::onEnterAmount,
                                onFiatValueChange = viewModel::onEnterFiatAmount,
                                amountExceedsBalance = step is SendStep.Error && step.error == SwapError.InsufficientBalanceFrom,
                                onTokenClick = null
                            )
                            if (!uiState.hideAddress) {
                                AddressRow(
                                    address = uiState.address,
                                    contactName = uiState.contactName,
                                    onClick = openAddress,
                                    risky = uiState.riskyAddress
                                )
                            }
                        }
                        if (!uiState.hideAddress) {
                            SectionArrow()
                        }
                    }
                    // Neither the chain's own field nor a memo can travel with a private send
                    // deposit (its memo slot belongs to the provider's identifier), so they
                    // are not offered on that tab.
                    val extraInput = uiState.extraInput?.takeIf { !uiState.isPrivateSend }
                    val memoSupport = uiState.memoSupport?.takeIf { !uiState.isPrivateSend }
                    if (extraInput != null) {
                        val error = uiState.extraInputError
                        SendInputCell(
                            value = uiState.extraInputValue,
                            hint = extraInput.title,
                            enabled = extraInput.fixedValue == null,
                            keyboardType = extraInput.keyboardType,
                            maxLength = extraInput.maxLength,
                            caption = error ?: extraInput.info,
                            captionColor = when {
                                error != null -> ComposeAppTheme.colors.lucian
                                extraInput.required -> ComposeAppTheme.colors.jacob
                                else -> ComposeAppTheme.colors.grey
                            },
                            onValueChange = viewModel::onEnterExtraInput,
                        )
                        if (memoSupport != null) {
                            VSpacer(8.dp)
                            HsDivider(modifier = Modifier.fillMaxWidth())
                        }
                    }
                    if (memoSupport != null) {
                        HSMemoInput(
                            maxBytes = memoSupport.maxBytes,
                            memo = uiState.memo,
                            visibility = memoSupport.visibility,
                            onValueChange = viewModel::onEnterMemo,
                        )
                    }
                    if (uiState.isPrivateSend) {
                        VSpacer(64.dp)
                        InfoCard(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            title = stringResource(R.string.PrivateSend_Toggle_Title),
                            text = stringResource(R.string.PrivateSend_Tab_Description)
                        )
                        VSpacer(32.dp)
                    }
                }

                val buttonTitle = when (val step = uiState.step) {
                    is SendStep.InputRequired -> when (step.inputType) {
                        SendInputType.Amount -> stringResource(R.string.Send_EnterAmount)
                        SendInputType.Address -> stringResource(R.string.Send_EnterAddress)
                        SendInputType.Extra -> stringResource(R.string.Send_EnterField, uiState.extraInput?.title.orEmpty())
                    }

                    is SendStep.Error -> when (step.error) {
                        is UnknownHostException -> stringResource(R.string.Hud_Text_NoInternet)
                        SwapError.InsufficientBalanceFrom -> stringResource(R.string.Swap_ErrorInsufficientBalance)
                        is TokenNotEnabled -> stringResource(R.string.Swap_ErrorTokenNotEnabled)
                        is WalletSyncing -> stringResource(R.string.Swap_ErrorWalletSyncing)
                        is WalletNotSynced -> stringResource(R.string.Swap_ErrorWalletNotSynced)
                        else -> step.error.message ?: step.error.javaClass.simpleName
                    }

                    SendStep.Proceed -> stringResource(R.string.Button_Next)
                }
                ButtonsGroupVertical {
                    HSButton(
                        modifier = Modifier.fillMaxWidth(),
                        title = buttonTitle,
                        enabled = uiState.step is SendStep.Proceed,
                    ) {
                        if (uiState.riskyAddress) {
                            keyboardController?.hide()
                            confirmRiskyAddress()
                        } else {
                            proceed()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BoxScope.SectionArrow() {
    Box(
        Modifier
            .align(Alignment.Center)
            .size(32.dp)
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        Icon(
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.Center),
            painter = painterResource(R.drawable.ic_arrow_down_24),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey,
        )
    }
}

@Composable
private fun SendTab.tabItem() = when (this) {
    SendTab.Standard -> TabFolderItem(stringResource(R.string.Send_Tab_Standard))
    SendTab.Private -> TabFolderItem(
        title = stringResource(R.string.Send_Tab_Private),
        icon = R.drawable.ic_incognito_24,
    )

    SendTab.CrossPay -> TabFolderItem(stringResource(R.string.Send_Tab_CrossPay))
}

@Composable
internal fun InfoCard(
    modifier: Modifier,
    title: String,
    text: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(16.dp))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.info_filled_24),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
            HSpacer(8.dp)
            Text(
                text = title,
                style = ComposeAppTheme.typography.headline2,
                color = ComposeAppTheme.colors.grey,
            )
        }
        VSpacer(8.dp)
        Text(
            text = text,
            style = ComposeAppTheme.typography.subheadR,
            color = ComposeAppTheme.colors.andy,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * The recipient row: a placeholder until an address is chosen, then the address in full, or
 * the contact or domain name over the shortened address when the recipient has one.
 */
@Composable
internal fun AddressRow(
    address: Address?,
    onClick: () -> Unit,
    contactName: String?,
    risky: Boolean
) {
    val name = contactName ?: address?.domain
    BoxBordered(bottom = true) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(89.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(
                    modifier = Modifier.size(32.dp),
                    painter = painterResource(if (contactName != null) R.drawable.user_filled_24 else R.drawable.wallet_filled_24),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey,
                )
            }
            HSpacer(16.dp)
            Column(modifier = Modifier.weight(1f, fill = false)) {
                when {
                    address == null -> headline1_leah(text = stringResource(R.string.Send_ToAddress))
                    name != null -> {
                        headline1_leah(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = address.hex.shorten(),
                            maxLines = ADDRESS_MAX_LINES,
                            style = ComposeAppTheme.typography.body,
                            color = if (risky) ComposeAppTheme.colors.lucian else ComposeAppTheme.colors.grey
                        )

                    }

                    else -> AddressText(address.hex, risky)
                }
            }
            HSpacer(8.dp)
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.arrow_s_down_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.leah,
            )
        }
    }
}

private const val ADDRESS_MAX_LINES = 2

/**
 * A full address on up to two lines. One that needs more is shortened instead: cut in the
 * middle to fit a single line.
 */
@Composable
private fun AddressText(address: String, risky: Boolean) {
    BoxWithConstraints {
        val textMeasurer = rememberTextMeasurer()
        val style = ComposeAppTheme.typography.headline1
        val text = remember(address, constraints.maxWidth, style) {
            if (fits(address, textMeasurer, style, constraints.maxWidth, ADDRESS_MAX_LINES)) {
                address
            } else {
                middleEllipsized(address, textMeasurer, style, constraints.maxWidth)
            }
        }
        Text(
            text = text,
            maxLines = ADDRESS_MAX_LINES,
            style = ComposeAppTheme.typography.headline1,
            color = if (risky) ComposeAppTheme.colors.lucian else ComposeAppTheme.colors.leah
        )
    }
}

private fun fits(text: String, textMeasurer: TextMeasurer, style: TextStyle, maxWidth: Int, maxLines: Int) =
    !textMeasurer.measure(
        text = text,
        style = style,
        maxLines = maxLines,
        constraints = Constraints(maxWidth = maxWidth),
    ).hasVisualOverflow

/**
 * The longest head + "..." + tail of [text] that fits [maxWidth] on one line, found by
 * bisecting the number of kept characters.
 */
private fun middleEllipsized(
    text: String,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    maxWidth: Int,
): String {
    fun cut(kept: Int): String {
        val head = (kept + 1) / 2
        return text.take(head) + "..." + text.takeLast(kept - head)
    }

    var low = 0
    var high = text.length - 1
    while (low < high) {
        val mid = (low + high + 1) / 2
        if (fits(cut(mid), textMeasurer, style, maxWidth, maxLines = 1)) low = mid else high = mid - 1
    }
    return cut(low)
}
