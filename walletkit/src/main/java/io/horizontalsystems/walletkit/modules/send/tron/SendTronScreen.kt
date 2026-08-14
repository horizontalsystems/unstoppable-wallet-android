package io.horizontalsystems.walletkit.modules.send.tron

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.address.AddressParserModule
import io.horizontalsystems.walletkit.modules.address.AddressParserViewModel
import io.horizontalsystems.walletkit.modules.address.HSAddressCell
import io.horizontalsystems.walletkit.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.walletkit.modules.amount.HSAmountInput
import io.horizontalsystems.walletkit.modules.availablebalance.AvailableBalance
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.privatesend.PrivateSendConfirmationPage
import io.horizontalsystems.walletkit.modules.privatesend.PrivateSendToggleSection
import io.horizontalsystems.walletkit.modules.privatesend.PrivateSendViewModel
import io.horizontalsystems.walletkit.modules.privatesend.privateSendViewModel
import io.horizontalsystems.walletkit.modules.send.AddressRiskySheet
import io.horizontalsystems.walletkit.modules.send.SendScreen
import io.horizontalsystems.walletkit.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import java.math.BigDecimal
import kotlin.reflect.KClass

@Composable
fun SendTronScreen(
    title: String,
    navigation: HSNavigation,
    viewModel: SendTronViewModel,
    amountInputModeViewModel: AmountInputModeViewModel,
    sendEntryPointDestId: KClass<out HSPage>,
    amount: BigDecimal?,
    riskyAddress: Boolean
) {
    val view = LocalView.current
    val wallet = viewModel.wallet
    val uiState = viewModel.uiState

    val availableBalance = uiState.availableBalance
    val amountCaution = uiState.amountCaution
    val proceedEnabled = uiState.proceedEnabled
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
            availableBalance = availableBalance,
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

        val forResult = navigation.slideFromBottomForResult<AddressRiskySheet.Result>(
            {
                AddressRiskySheet(
                    AddressRiskySheet.Input(
                        alertText = Translator.getString(R.string.Send_RiskyAddress_AlertText)
                    )
                )
            }
        ) {
            openConfirm(viewModel, privateSendViewModel, wallet, navigation, sendEntryPointDestId)
        }

        ButtonPrimaryYellow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            title = stringResource(R.string.Button_Next),
            onClick = {
                if (!viewModel.hasConnection()) {
                    HudHelper.showErrorMessage(view, R.string.Hud_Text_NoInternet)
                } else if (riskyAddress) {
                    keyboardController?.hide()
                    forResult()
                } else {
                    openConfirm(viewModel, privateSendViewModel, wallet, navigation, sendEntryPointDestId)
                }
            },
            enabled = proceedEnabled
        )
    }

}

private fun openConfirm(
    viewModel: SendTronViewModel,
    privateSendViewModel: PrivateSendViewModel,
    wallet: Wallet,
    navigation: HSNavigation,
    sendEntryPointDestId: KClass<out HSPage>
) {
    if (privateSendViewModel.isEnabled) {
        val amount = privateSendViewModel.amount ?: return

        navigation.slideFromRight(
            PrivateSendConfirmationPage(
                PrivateSendConfirmationPage.Input(
                    wallet = wallet,
                    recipient = viewModel.uiState.address.hex,
                    amount = amount,
                    sendEntryPointDestId = sendEntryPointDestId,
                )
            )
        )
        return
    }

    viewModel.onNavigateToConfirmation()

    navigation.slideFromRight(SendTronConfirmationPage(sendEntryPointDestId))
}
