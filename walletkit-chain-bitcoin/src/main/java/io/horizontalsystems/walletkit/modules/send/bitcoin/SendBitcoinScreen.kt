package io.horizontalsystems.walletkit.modules.send.bitcoin

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
import io.horizontalsystems.walletkit.R
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
import io.horizontalsystems.walletkit.modules.privatesend.PrivateSendToggleSection
import io.horizontalsystems.walletkit.modules.privatesend.PrivateSendViewModel
import io.horizontalsystems.walletkit.modules.privatesend.privateSendViewModel
import io.horizontalsystems.walletkit.modules.send.AddressRiskySheet
import io.horizontalsystems.walletkit.modules.send.SendPage
import io.horizontalsystems.walletkit.modules.send.bitcoin.advanced.BtcTransactionInputSortInfoScreen
import io.horizontalsystems.walletkit.modules.send.bitcoin.advanced.FeeRateCaution
import io.horizontalsystems.walletkit.modules.send.bitcoin.advanced.SendBtcAdvancedSettingsScreen
import io.horizontalsystems.walletkit.modules.send.bitcoin.utxoexpert.UtxoExpertModeScreen
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.walletkit.ui.compose.components.HSpacer
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.ui.compose.components.RowUniversal
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_grey
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_leah
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import kotlin.reflect.KClass

@Serializable
data object SendBtcAdvancedSettingsPage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = navigation.viewModelForScreen<SendBitcoinViewModel>(SendPage::class)
        val amountInputModeViewModel = navigation.viewModelForScreen<AmountInputModeViewModel>(SendPage::class)
        SendBtcAdvancedSettingsScreen(
            navigation = navigation,
            sendBitcoinViewModel = viewModel,
            amountInputType = amountInputModeViewModel.inputType,
        )
    }
}

data object TransactionInputsSortInfoPage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        BtcTransactionInputSortInfoScreen { navigation.removeLastOrNull() }
    }
}

data object UtxoExpertModePage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = navigation.viewModelForScreen<SendBitcoinViewModel>(SendPage::class)
        UtxoExpertModeScreen(
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
fun SendBitcoinScreen(
    title: String,
    navigation: HSNavigation,
    viewModel: SendBitcoinViewModel,
    amountInputModeViewModel: AmountInputModeViewModel,
    sendEntryPointDestId: KClass<out HSPage>,
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

    val privateSendViewModel = privateSendViewModel(wallet.token)

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    HSScaffold(
        title = title,
        onBack = { navigation.removeLastOrNull() },
        // The advanced settings (UTXO selection, sort mode, RBF, lock time) only shape the
        // regular send; the private deposit transfer is built on the confirmation screen and
        // would silently discard them, so the menu goes away rather than accept dead input.
        menuItems = if (privateSendViewModel.isEnabled) {
            emptyList()
        } else {
            listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.SendEvmSettings_Title),
                    icon = R.drawable.manage_24,
                    onClick = { navigation.add(SendBtcAdvancedSettingsPage) }
                ),
            )
        },
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

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                PrivateSendToggleSection(privateSendViewModel)
            }

            // These rows describe the REGULAR send only. The private path cannot carry a
            // user memo (the deposit's memo slot belongs to the provider's crediting
            // identifier), spends whatever UTXOs the deposit build selects, and estimates
            // its own fee on the confirmation screen — so showing them would collect input
            // the send discards.
            if (!privateSendViewModel.isEnabled) {
                VSpacer(16.dp)
                HSMemoInput(maxLength = 120, visibility = MemoVisibility.Public) {
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
                                navigation.add(UtxoExpertModePage)
                            }
                        )
                        HsDivider(modifier = Modifier.fillMaxWidth())
                    }
                    HSFeeRaw(
                        coinCode = wallet.coin.code,
                        coinDecimal = viewModel.coinMaxAllowedDecimals,
                        fee = fee,
                        amountInputType = amountInputType,
                        rate = rate,
                        navigation = navigation
                    )
                }

                feeRateCaution?.let {
                    FeeRateCaution(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                        feeRateCaution = feeRateCaution
                    )
                }
            }

            VSpacer(16.dp)

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
                    .padding(horizontal = 16.dp),
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
            VSpacer(32.dp)
        }
    }
}

private fun openConfirm(
    viewModel: SendBitcoinViewModel,
    privateSendViewModel: PrivateSendViewModel,
    navigation: HSNavigation,
    sendEntryPointDestId: KClass<out HSPage>
) {
    if (privateSendViewModel.openConfirmationIfEnabled(navigation, viewModel.wallet, viewModel.uiState.address.hex, sendEntryPointDestId)) {
        return
    }

    navigation.slideFromRight(SendBitcoinConfirmationPage(sendEntryPointDestId))
}

