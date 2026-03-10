package io.horizontalsystems.bankwallet.modules.send.bitcoin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.address.AddressParserModule
import io.horizontalsystems.bankwallet.modules.address.AddressParserViewModel
import io.horizontalsystems.bankwallet.modules.address.HSAddressCell
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.amount.HSAmountInput
import io.horizontalsystems.bankwallet.modules.availablebalance.AvailableBalance
import io.horizontalsystems.bankwallet.modules.fee.HSFeeRaw
import io.horizontalsystems.bankwallet.modules.memo.HSMemoInput
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEffect
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.send.AddressRiskyBottomSheetScreen
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationFragment
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationScreen
import io.horizontalsystems.bankwallet.modules.send.bitcoin.advanced.FeeRateCaution
import io.horizontalsystems.bankwallet.modules.send.bitcoin.advanced.SendBtcAdvancedSettingsScreen
import io.horizontalsystems.bankwallet.modules.send.bitcoin.utxoexpert.UtxoExpertModeScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import java.math.BigDecimal
import kotlin.reflect.KClass

@Composable
fun SendBitcoinScreen(
    title: String,
    backStack: NavBackStack<HSScreen>,
    resultBus: ResultEventBus,
    viewModel: SendBitcoinViewModel,
    amountInputModeViewModel: AmountInputModeViewModel,
    sendEntryPointDestId: KClass<out HSScreen>,
    amount: BigDecimal?,
    riskyAddress: Boolean,
) {
    val wallet = viewModel.wallet
    val uiState = viewModel.uiState

    val availableBalance = uiState.availableBalance
    val amountCaution = uiState.amountCaution
    val fee = uiState.fee
    val proceedEnabled = uiState.canBeSend
    val amountInputType = amountInputModeViewModel.inputType
    val feeRateCaution = uiState.feeRateCaution
    val keyboardController = LocalSoftwareKeyboardController.current

    val rate = viewModel.coinRate

    val paymentAddressViewModel = viewModel<AddressParserViewModel>(
        factory = AddressParserModule.Factory(wallet.token, amount)
    )
    val amountUnique = paymentAddressViewModel.amountUnique

    ComposeAppTheme {
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        HSScaffold(
            title = title,
            onBack = { backStack.removeLastOrNull() },
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.SendEvmSettings_Title),
                    icon = R.drawable.manage_24,
                    onClick = { backStack.add(SendBtcAdvancedSettingsScreen) }
                ),
            ),
        ) {
            Column(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.ime)
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
            ) {
                VSpacer(16.dp)
                if (uiState.showAddressInput) {
                    HSAddressCell(
                        title = stringResource(R.string.Send_Confirmation_To),
                        value = uiState.address.hex,
                        riskyAddress = riskyAddress
                    ) {
                        backStack.removeLastOrNull()
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
                    },
                    inputType = amountInputType,
                    rate = rate,
                    amountUnique = amountUnique
                )

                VSpacer(8.dp)
                AvailableBalance(
                    coinCode = wallet.coin.code,
                    coinDecimal = viewModel.coinMaxAllowedDecimals,
                    fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                    availableBalance = availableBalance,
                    amountInputType = amountInputType,
                    rate = rate
                )

                VSpacer(16.dp)
                HSMemoInput(maxLength = 120) {
                    viewModel.onEnterMemo(it)
                }

                VSpacer(16.dp)
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ComposeAppTheme.colors.lawrence)
                        .padding(vertical = 8.dp)
                ) {
                    uiState.utxoData?.let { utxoData ->
                        UtxoCell(
                            utxoData = utxoData,
                            onClick = {
                                backStack.add(UtxoExpertModeScreen)
                            }
                        )
                    }
                    HSFeeRaw(
                        coinCode = wallet.coin.code,
                        coinDecimal = viewModel.coinMaxAllowedDecimals,
                        fee = fee,
                        amountInputType = amountInputType,
                        rate = rate,
                        backStack = backStack
                    )
                }

                feeRateCaution?.let {
                    FeeRateCaution(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                        feeRateCaution = feeRateCaution
                    )
                }

                VSpacer(16.dp)

                ResultEffect<AddressRiskyBottomSheetScreen.Result>(resultBus) {
                    if (it.canContinue) {
                        openConfirm(backStack, sendEntryPointDestId)
                    }
                }

                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    title = stringResource(R.string.Button_Next),
                    onClick = {
                        if (riskyAddress) {
                            keyboardController?.hide()
                            backStack.add(
                                AddressRiskyBottomSheetScreen(
                                    alertText = Translator.getString(R.string.Send_RiskyAddress_AlertText)
                                )
                            )
                        } else {
                            openConfirm(backStack, sendEntryPointDestId)
                        }
                    },
                    enabled = proceedEnabled
                )
                VSpacer(32.dp)
            }
        }
    }
}

private fun openConfirm(
    backStack: NavBackStack<HSScreen>,
    sendEntryPointDestId: KClass<out HSScreen>
) {
    backStack.add(
        SendConfirmationScreen(
            SendConfirmationFragment.Type.Bitcoin,
            sendEntryPointDestId
        )
    )
}

@Composable
fun UtxoCell(
    utxoData: SendBitcoinModule.UtxoData,
    onClick: () -> Unit
) {
    RowUniversal(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        onClick = onClick
    ) {
        subhead2_grey(
            text = stringResource(R.string.Send_Utxos),
            modifier = Modifier.weight(1f)
        )
        subhead2_leah(text = utxoData.value)
        HSpacer(8.dp)
        when (utxoData.type) {
            SendBitcoinModule.UtxoType.Auto -> {
                Icon(
                    painter = painterResource(R.drawable.ic_edit_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }

            SendBitcoinModule.UtxoType.Manual -> {
                Icon(
                    painter = painterResource(R.drawable.ic_edit_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.jacob
                )
            }

            null -> {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }
        }
    }
}
