package io.horizontalsystems.walletkit.modules.send.xrp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.DataState
import io.horizontalsystems.walletkit.modules.address.AddressParserModule
import io.horizontalsystems.walletkit.modules.address.AddressParserViewModel
import io.horizontalsystems.walletkit.modules.address.HSAddressCell
import io.horizontalsystems.walletkit.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.walletkit.modules.amount.HSAmountInput
import io.horizontalsystems.walletkit.modules.availablebalance.AvailableBalance
import io.horizontalsystems.walletkit.modules.fee.HSFee
import io.horizontalsystems.walletkit.modules.memo.HSMemoInput
import io.horizontalsystems.walletkit.modules.memo.MemoVisibility
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.AddressRiskySheet
import io.horizontalsystems.walletkit.modules.send.SendScreen
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.walletkit.ui.compose.components.FormsInput
import io.horizontalsystems.walletkit.ui.compose.components.FormsInputStateWarning
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.caption_grey
import java.math.BigDecimal
import kotlin.reflect.KClass

@Composable
fun SendXrpScreen(
    title: String,
    navigation: HSNavigation,
    viewModel: SendXrpViewModel,
    amountInputModeViewModel: AmountInputModeViewModel,
    sendEntryPointDestId: KClass<out HSPage>,
    amount: BigDecimal?,
    riskyAddress: Boolean
) {
    val wallet = viewModel.wallet
    val uiState = viewModel.uiState

    val availableBalance = uiState.availableBalance
    val amountInputType = amountInputModeViewModel.inputType
    val keyboardController = LocalSoftwareKeyboardController.current

    val paymentAddressViewModel = viewModel<AddressParserViewModel>(
        factory = AddressParserModule.Factory(wallet.token, amount)
    )
    val amountUnique = paymentAddressViewModel.amountUnique

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    SendScreen(
        title = title,
        onBack = { navigation.removeLastOrNull() }
    ) {
        VSpacer(16.dp)
        if (uiState.showAddressInput) {
            HSAddressCell(
                title = stringResource(R.string.Send_Confirmation_To),
                value = uiState.address.hex,
                riskyAddress = riskyAddress
            ) {
                navigation.removeLastOrNull()
            }
            VSpacer(16.dp)
        }

        HSAmountInput(
            modifier = Modifier.padding(horizontal = 16.dp),
            focusRequester = focusRequester,
            availableBalance = availableBalance ?: BigDecimal.ZERO,
            caution = uiState.amountCaution,
            coinCode = wallet.coin.code,
            coinDecimal = viewModel.coinMaxAllowedDecimals,
            fiatDecimal = viewModel.fiatMaxAllowedDecimals,
            onClickHint = {
                amountInputModeViewModel.onToggleInputType()
            },
            onValueChange = {
                viewModel.onEnterAmount(it)
            },
            inputType = amountInputType,
            rate = viewModel.coinRate,
            amountUnique = amountUnique
        )

        VSpacer(8.dp)
        AvailableBalance(
            coinCode = wallet.coin.code,
            coinDecimal = viewModel.coinMaxAllowedDecimals,
            fiatDecimal = viewModel.fiatMaxAllowedDecimals,
            availableBalance = availableBalance,
            amountInputType = amountInputType,
            rate = viewModel.coinRate
        )

        VSpacer(16.dp)
        DestinationTagInput(
            fixedTag = uiState.fixedDestinationTag,
            required = uiState.destinationTagRequired,
            error = uiState.destinationError,
        ) {
            viewModel.onEnterDestinationTag(it)
        }

        VSpacer(16.dp)
        HSMemoInput(
            maxLength = 120,
            visibility = MemoVisibility.Public,
        ) {
            viewModel.onEnterMemo(it)
        }

        VSpacer(16.dp)
        HSFee(
            coinCode = viewModel.feeToken.coin.code,
            coinDecimal = viewModel.feeTokenMaxAllowedDecimals,
            fee = uiState.fee,
            amountInputType = amountInputType,
            rate = viewModel.feeCoinRate,
            navigation = navigation,
        )

        val forResult = navigation.slideFromBottomForResult<AddressRiskySheet.Result>(
            {
                AddressRiskySheet(
                    AddressRiskySheet.Input(
                        alertText = Translator.getString(R.string.Send_RiskyAddress_AlertText)
                    )
                )
            }
        ) {
            openConfirm(navigation, sendEntryPointDestId)
        }

        ButtonPrimaryYellow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            title = stringResource(R.string.Button_Next),
            onClick = {
                if (riskyAddress) {
                    keyboardController?.hide()
                    forResult()
                } else {
                    openConfirm(navigation, sendEntryPointDestId)
                }
            },
            enabled = uiState.canBeSend
        )
    }
}

/**
 * Numeric destination-tag field. Exchanges and custodial services use the tag to credit the
 * right customer; it is public on the ledger like a memo. An X-address fixes the tag.
 */
@Composable
private fun DestinationTagInput(
    fixedTag: Long?,
    required: Boolean,
    error: Throwable?,
    onValueChange: (String) -> Unit,
) {
    val state = error?.let { DataState.Error(FormsInputStateWarning(it.message ?: "")) }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        FormsInput(
            hint = stringResource(R.string.Send_DestinationTag_Hint),
            initial = fixedTag?.toString(),
            enabled = fixedTag == null,
            hintColor = ComposeAppTheme.colors.andy,
            hintStyle = ComposeAppTheme.typography.bodyItalic,
            textColor = ComposeAppTheme.colors.leah,
            textStyle = ComposeAppTheme.typography.bodyItalic,
            pasteEnabled = false,
            singleLine = true,
            maxLength = 10,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            state = state,
            onValueChange = onValueChange,
        )

        if (state == null) {
            caption_grey(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                text = stringResource(
                    if (required) R.string.Send_DestinationTag_Required else R.string.Send_DestinationTag_Info
                )
            )
        }
    }
}

private fun openConfirm(navigation: HSNavigation, sendEntryPointDestId: KClass<out HSPage>) {
    navigation.slideFromRight(SendXrpConfirmationPage(sendEntryPointDestId))
}
