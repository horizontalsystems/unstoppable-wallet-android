package io.horizontalsystems.walletkit.modules.send.zano

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.providers.Translator
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
import io.horizontalsystems.walletkit.modules.privatesend.PrivateSendToggleSection
import io.horizontalsystems.walletkit.modules.privatesend.PrivateSendViewModel
import io.horizontalsystems.walletkit.modules.privatesend.privateSendViewModel
import io.horizontalsystems.walletkit.modules.send.AddressRiskySheet
import io.horizontalsystems.walletkit.modules.send.SendScreen
import io.horizontalsystems.walletkit.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.walletkit.ui.compose.components.TextImportantError
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import java.math.BigDecimal
import kotlin.reflect.KClass

@Composable
fun SendZanoScreen(
    title: String,
    navigation: HSNavigation,
    viewModel: SendZanoViewModel,
    amountInputModeViewModel: AmountInputModeViewModel,
    sendEntryPointDestId: KClass<out HSPage>,
    amount: BigDecimal?,
    memo: String?,
    riskyAddress: Boolean
) {
    val wallet = viewModel.wallet
    val uiState = viewModel.uiState

    val availableBalance = uiState.availableBalance
    val amountCaution = uiState.amountCaution
    val proceedEnabled = uiState.canBeSend
    val fee = uiState.fee
    val feeInProgress = uiState.feeInProgress
    val amountInputType = amountInputModeViewModel.inputType
    val keyboardController = LocalSoftwareKeyboardController.current

    val paymentAddressViewModel = viewModel<AddressParserViewModel>(
        factory = AddressParserModule.Factory(wallet.token, amount)
    )
    val amountUnique = paymentAddressViewModel.amountUnique

    val privateSendViewModel = privateSendViewModel(wallet.token)

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
            caution = amountCaution,
            coinCode = wallet.coin.code,
            coinDecimal = viewModel.coinMaxAllowedDecimals,
            fiatDecimal = viewModel.fiatMaxAllowedDecimals,
            onClickHint = {
                amountInputModeViewModel.onToggleInputType()
            },
            onValueChange = {
                viewModel.onEnterAmount(it)
                privateSendViewModel.onEnterAmount(it)
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

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            PrivateSendToggleSection(privateSendViewModel)
        }

        // A user memo cannot be delivered on a private send — the deposit's memo slot
        // belongs to the provider's crediting identifier — so don't collect one the
        // send would discard.
        if (!privateSendViewModel.isEnabled) {
            VSpacer(16.dp)
            HSMemoInput(maxLength = 120, memo = memo, visibility = MemoVisibility.Offchain) {
                viewModel.onEnterMemo(it)
            }
        }

        VSpacer(16.dp)
        HSFee(
            coinCode = viewModel.feeToken.coin.code,
            coinDecimal = viewModel.feeTokenMaxAllowedDecimals,
            fee = fee,
            amountInputType = amountInputType,
            rate = viewModel.feeCoinRate,
            navigation = navigation,
        )

        uiState.feeCaution?.let { caution ->
            VSpacer(12.dp)
            TextImportantError(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = caution.getString(),
                text = caution.getDescription() ?: ""
            )
        }

        val forResult = navigation.slideFromBottomForResult<AddressRiskySheet.Result>(
            {
                AddressRiskySheet(
                    AddressRiskySheet.Input(
                        alertText = Translator.getString(R.string.Send_RiskyAddress_AlertText)
                    )
                )
            }
        ) {
            openConfirm(viewModel, privateSendViewModel, navigation, sendEntryPointDestId)
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
                    openConfirm(viewModel, privateSendViewModel, navigation, sendEntryPointDestId)
                }
            },
            enabled = proceedEnabled
        )
    }
}

private fun openConfirm(
    viewModel: SendZanoViewModel,
    privateSendViewModel: PrivateSendViewModel,
    navigation: HSNavigation,
    sendEntryPointDestId: KClass<out HSPage>
) {
    if (privateSendViewModel.openConfirmationIfEnabled(navigation, viewModel.wallet, viewModel.uiState.address.hex, sendEntryPointDestId)) {
        return
    }

    navigation.slideFromRight(SendZanoConfirmationPage(sendEntryPointDestId))
}
