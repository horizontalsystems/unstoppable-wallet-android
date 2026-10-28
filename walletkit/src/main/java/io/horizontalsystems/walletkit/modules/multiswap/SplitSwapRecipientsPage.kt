package io.horizontalsystems.walletkit.modules.multiswap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.CoinValue
import io.horizontalsystems.walletkit.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.walletkit.modules.multiswap.history.shortenAddress
import io.horizontalsystems.walletkit.modules.multiswap.settings.SwapSettingsRecipientPage
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.uiv3.components.AlertCard
import io.horizontalsystems.walletkit.uiv3.components.AlertFormat
import io.horizontalsystems.walletkit.uiv3.components.AlertType
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import io.horizontalsystems.walletkit.uiv3.components.controls.HSButton
import io.horizontalsystems.walletkit.uiv3.components.controls.HSStepSlider
import io.horizontalsystems.walletkit.uiv3.components.info.TextBlock
import java.math.BigDecimal
import kotlinx.serialization.Serializable

/**
 * The Private tab's step between the swap screen and the confirmation: where each output goes
 * and how the deposit is divided. The shares are requoted as the slider moves; the amounts on the
 * cards are what the provider expects to pay each address. Opens the confirmation itself and
 * stays in the back stack, so going back from it returns here with everything intact.
 */
@Serializable
data class SplitSwapRecipientsPage(val input: Input) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        SplitSwapRecipientsScreen(navigation, input)
    }

    @Serializable
    data class Input(
        val parentScreenContentKey: String,
        val closeAfterSwap: Boolean,
    )
}

@Composable
private fun SplitSwapRecipientsScreen(navigation: HSNavigation, input: SplitSwapRecipientsPage.Input) {
    val swapViewModel = navigation.viewModelForScreen<SwapViewModel>(
        input.parentScreenContentKey,
        key = SwapViewModel.PRIVATE_VIEW_MODEL_KEY,
    )
    val uiState = swapViewModel.uiState
    val tokenOut = uiState.tokenOut ?: return

    val openConfirmation = navigation.slideFromRightForResult<SwapConfirmPage.Result>(
        {
            SwapConfirmPage(
                parentScreenContentKey = input.parentScreenContentKey,
                splitRecipients = swapViewModel.splitRecipientsForConfirm(),
                swapViewModelKey = SwapViewModel.PRIVATE_VIEW_MODEL_KEY,
            )
        }
    ) { result ->
        if (result.success) {
            // The confirmation popped itself; this page goes too, so the user lands on the
            // swap screen, which starts the next split from scratch.
            swapViewModel.resetSplit()
            navigation.removeLastOrNull()
            if (input.closeAfterSwap) {
                navigation.removeLastOrNull()
            } else {
                swapViewModel.onEnterAmount(null)
            }
        }
    }

    // One address page serves every card; the card tapped last is the one its result fills.
    var editingIndex by rememberSaveable { mutableIntStateOf(0) }
    val openAddress = navigation.slideFromRightForResult<SwapSettingsRecipientPage.Result>(
        {
            SwapSettingsRecipientPage(
                SwapSettingsRecipientPage.Input(
                    token = tokenOut,
                    recipient = uiState.splitRecipients.getOrNull(editingIndex),
                    allowRemoval = false,
                    splitIndex = editingIndex + 1,
                    zcashTransparentOnly = uiState.quote
                        ?.let { App.swapProviderInfoManager.zcashTransparentRecipientOnly(it.provider.id, it.swapQuote) }
                        ?: (tokenOut.blockchainType == BlockchainType.Zcash),
                )
            )
        }
    ) { result ->
        result.address?.let { swapViewModel.setSplitRecipient(editingIndex, it) }
    }

    HSScaffold(
        title = stringResource(R.string.SplitSwap_Recipients_Title),
        onBack = navigation::removeLastOrNull,
        bottomBar = {
            ButtonsGroupWithShade {
                val modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                when (uiState.splitRecipientsStep) {
                    SplitRecipientsStep.Quoting -> HSButton(
                        modifier = modifier,
                        title = stringResource(R.string.Swap_Quoting),
                        loadingIndicator = true,
                        enabled = false,
                    )

                    SplitRecipientsStep.InvalidSplit -> HSButton(
                        modifier = modifier,
                        title = stringResource(R.string.Button_Next),
                        enabled = false,
                    )

                    SplitRecipientsStep.EnterAddresses -> HSButton(
                        modifier = modifier,
                        title = stringResource(R.string.SplitSwap_AddAddress, uiState.splitRecipients.indexOfFirst { it == null } + 1),
                        enabled = false,
                    )

                    SplitRecipientsStep.NoQuotes -> HSButton(
                        modifier = modifier,
                        title = stringResource(R.string.Swap_ErrorNoQuote),
                        enabled = false,
                    )

                    SplitRecipientsStep.Next -> HSButton(
                        modifier = modifier,
                        title = stringResource(R.string.Button_Next),
                        onClick = openConfirmation,
                    )
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            VSpacer(12.dp)
            TextBlock(text = stringResource(R.string.SplitSwap_Recipients_Description))
            VSpacer(16.dp)

            uiState.splitRecipients.forEachIndexed { index, address ->
                // The quote on screen prices the current shares; while new shares are being
                // quoted the previous amounts stay, dimmed, so the cards never go blank.
                val currentAmount = uiState.splitOutputAmounts?.getOrNull(index)
                val amount = currentAmount ?: uiState.splitLastOutputAmounts?.takeIf { uiState.splitRequoting }?.getOrNull(index)
                RecipientCard(
                    index = index,
                    address = address,
                    tokenOut = tokenOut,
                    amount = amount,
                    amountDimmed = currentAmount == null,
                    percent = uiState.splitPercents.getOrElse(index) { 0 },
                    duplicate = index in uiState.splitDuplicateIndexes,
                    onClick = {
                        editingIndex = index
                        openAddress()
                    },
                )
                VSpacer(16.dp)
            }

            val firstPercent = uiState.splitPercents.firstOrNull() ?: SplitSwapRules.PERCENT_DEFAULT
            HSStepSlider(
                modifier = Modifier.padding(horizontal = 16.dp),
                value = firstPercent,
                range = SplitSwapRules.PERCENT_MIN..SplitSwapRules.PERCENT_MAX,
                step = SplitSwapRules.PERCENT_STEP,
                onValueChange = swapViewModel::onSplitPercentChanged,
                leftLabel = "$firstPercent%",
                rightLabel = "${100 - firstPercent}%",
            )

            if (uiState.splitInvalid) {
                VSpacer(16.dp)
                AlertCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    format = AlertFormat.Structured,
                    type = AlertType.Critical,
                    titleCustom = stringResource(R.string.SplitSwap_InvalidSplit_Title),
                    text = stringResource(
                        if (uiState.splitBelowMinimum) R.string.SplitSwap_InvalidSplit_BelowMinimum else R.string.SplitSwap_InvalidSplit_Text
                    ),
                )
            }

            if (uiState.splitDuplicateIndexes.isNotEmpty()) {
                VSpacer(16.dp)
                AlertCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    format = AlertFormat.Structured,
                    type = AlertType.Caution,
                    titleCustom = stringResource(R.string.SplitSwap_DuplicateAddresses_Title),
                    text = stringResource(R.string.SplitSwap_DuplicateAddresses_Text),
                )
            }

            VSpacer(32.dp)
        }
    }
}

@Composable
private fun RecipientCard(
    index: Int,
    address: Address?,
    tokenOut: Token,
    amount: BigDecimal?,
    amountDimmed: Boolean,
    percent: Int,
    duplicate: Boolean,
    onClick: () -> Unit,
) {
    val leah = ComposeAppTheme.colors.leah
    val contactName = remember(address?.hex, tokenOut.blockchainType) {
        address?.let {
            App.contactsRepository
                .getContactsFiltered(tokenOut.blockchainType, addressQuery = it.hex)
                .firstOrNull()
                ?.name
        }
    }
    // A repeated address is pointed out but allowed: it only weakens the split's purpose.
    val addressColor = if (duplicate) ComposeAppTheme.colors.jacob else leah

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence),
    ) {
        CellPrimary(
            left = {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(if (contactName != null) R.drawable.user_filled_24 else R.drawable.wallet_filled_24),
                    tint = ComposeAppTheme.colors.grey,
                    contentDescription = null,
                )
            },
            middle = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when {
                        address == null -> CellMiddleInfo(
                            title = stringResource(R.string.SplitSwap_Address, index + 1).hs(leah),
                        )

                        contactName != null -> CellMiddleInfo(
                            title = contactName.hs(leah),
                            subtitle = address.hex.shortenAddress().hs(addressColor),
                        )

                        else -> CellMiddleInfo(
                            title = address.hex.shortenAddress().hs(addressColor),
                        )
                    }
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.arrow_s_down_20),
                        tint = ComposeAppTheme.colors.grey,
                        contentDescription = null,
                    )
                }
            },
            right = {
                CellRightInfo(
                    eyebrow = (amount?.let { CoinValue(tokenOut, it).getFormattedFull() } ?: "---").hs(leah, dimmed = amountDimmed),
                    subtitle = "$percent%".hs(ComposeAppTheme.colors.jacob),
                )
            },
            onClick = onClick,
        )
    }
}
