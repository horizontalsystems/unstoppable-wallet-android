package io.horizontalsystems.walletkit.modules.send.monero

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.modules.address.AddressParserModule
import io.horizontalsystems.walletkit.modules.address.AddressParserViewModel
import io.horizontalsystems.walletkit.modules.address.HSAddressCell
import io.horizontalsystems.walletkit.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.walletkit.modules.amount.HSAmountInput
import io.horizontalsystems.walletkit.modules.availablebalance.AvailableBalance
import io.horizontalsystems.walletkit.modules.fee.HSFeeRaw
import io.horizontalsystems.walletkit.modules.memo.HSMemoInput
import io.horizontalsystems.walletkit.modules.memo.MemoVisibility
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.AddressRiskySheet
import io.horizontalsystems.walletkit.modules.send.SendPage
import io.horizontalsystems.walletkit.modules.send.bitcoin.UtxoCell
import io.horizontalsystems.walletkit.modules.send.bitcoin.advanced.FeeRateCaution
import io.horizontalsystems.walletkit.modules.send.monero.utxoexpert.MoneroUtxoExpertModeScreen
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import java.math.BigDecimal
import kotlin.reflect.KClass

data object SendMoneroAdvancedSettingsPage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        SendMoneroAdvancedSettingsScreen(
            onBack = { navigation.removeLastOrNull() }
        )
    }
}

data object MoneroUtxoExpertModePage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = navigation.viewModelForScreen<SendMoneroViewModel>(SendPage::class)
        MoneroUtxoExpertModeScreen(
            adapter = viewModel.adapter,
            token = viewModel.wallet.token,
            customUnspentOutputs = viewModel.customUnspentOutputs,
            updateUnspentOutputs = {
                viewModel.updateCustomUnspentOutputs(it)
            },
            onBackClick = {
                navigation.removeLastOrNull()
            }
        )
    }
}

@Composable
fun SendMoneroScreen(
    title: String,
    navigation: HSNavigation,
    viewModel: SendMoneroViewModel,
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


    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    HSScaffold(
        title = title,
        onBack = { navigation.removeLastOrNull() },
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.SendEvmSettings_Title),
                icon = R.drawable.manage_24,
                onClick = { navigation.add(SendMoneroAdvancedSettingsPage) }
            ),
        ),
    ) {
        Column(
            modifier = Modifier
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
        HSMemoInput(maxLength = 120, memo = memo, visibility = MemoVisibility.Offchain) {
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
                        navigation.add(MoneroUtxoExpertModePage)
                    }
                )
                HsDivider(modifier = Modifier.fillMaxWidth())
            }
            HSFeeRaw(
                coinCode = viewModel.feeToken.coin.code,
                coinDecimal = viewModel.feeTokenMaxAllowedDecimals,
                fee = fee,
                amountInputType = amountInputType,
                rate = viewModel.feeCoinRate,
                navigation = navigation,
            )
        }

        uiState.feeCaution?.let { caution ->
            FeeRateCaution(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                feeRateCaution = caution
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
            openConfirm(navigation, sendEntryPointDestId)
        }

        ButtonPrimaryYellow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            title = when (uiState.syncState) {
                is AdapterState.Synced -> stringResource(R.string.Button_Next)
                is AdapterState.NotSynced -> stringResource(R.string.Swap_ErrorWalletNotSynced)
                else -> stringResource(R.string.Swap_ErrorWalletSyncing)
            },
            onClick = {
                if (riskyAddress) {
                    keyboardController?.hide()
                    forResult()
                } else {
                    openConfirm(navigation, sendEntryPointDestId)
                }
            },
            enabled = proceedEnabled
        )
        }
    }
}

private fun openConfirm(
    navigation: HSNavigation,
    sendEntryPointDestId: KClass<out HSPage>
) {
    navigation.slideFromRight(SendMoneroConfirmationPage(sendEntryPointDestId))
}
