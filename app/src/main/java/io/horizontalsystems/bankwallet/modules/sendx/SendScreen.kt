package io.horizontalsystems.bankwallet.modules.sendx

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.fee.FeeRateCaution
import io.horizontalsystems.bankwallet.modules.fee.HSFeeInputRaw
import io.horizontalsystems.bankwallet.modules.sendevm.AvailableBalance
import io.horizontalsystems.bankwallet.modules.sendevm.HSAmountInput
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.hodler.LockTimeInterval

@Composable
fun SendScreen(
    navController: NavController,
    viewModel: SendBitcoinViewModel,
    xRateViewModel: XRateViewModel,
    amountInputModeViewModel: AmountInputModeViewModel
) {
    val wallet = viewModel.wallet
    val uiState = viewModel.uiState
    val isLockTimeEnabled = viewModel.isLockTimeEnabled
    val lockTimeIntervals = viewModel.lockTimeIntervals

    val availableBalance = uiState.availableBalance
    val addressError = uiState.addressError
    val amountCaution = uiState.amountCaution
    val fee = uiState.fee
    val proceedEnabled = uiState.canBeSend
    val amountInputType = amountInputModeViewModel.inputType
    val feeRateCaution = uiState.feeRateCaution
    val lockTimeInterval = uiState.lockTimeInterval

    val rate = xRateViewModel.rate

    ComposeAppTheme {
        val fullCoin = wallet.platformCoin.fullCoin
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.Send_Title, fullCoin.coin.code),
                navigationIcon = {
                    CoinImage(
                        iconUrl = fullCoin.coin.iconUrl,
                        placeholder = fullCoin.iconPlaceholder,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .size(24.dp)
                    )
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = {
                            navController.popBackStack()
                        }
                    )
                )
            )

            AvailableBalance(
                coinCode = wallet.coin.code,
                coinDecimal = viewModel.coinMaxAllowedDecimals,
                fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                availableBalance = availableBalance,
                amountInputType = amountInputType,
                rate = rate
            )

            Spacer(modifier = Modifier.height(12.dp))
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
                },
                inputType = amountInputType,
                rate = rate
            )

            Spacer(modifier = Modifier.height(12.dp))
            HSAddressInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                coinType = wallet.coinType,
                coinCode = wallet.coin.code,
                error = addressError
            ) {
                viewModel.onEnterAddress(it)
            }

            Spacer(modifier = Modifier.height(12.dp))

            val composableItems = buildList<@Composable () -> Unit> {
                add {
                    HSFeeInputRaw(
                        coinCode = wallet.coin.code,
                        coinDecimal = viewModel.coinMaxAllowedDecimals,
                        fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                        fee = fee,
                        amountInputType = amountInputType,
                        rate = rate,
                        enabled = viewModel.feeRateChangeable,
                        onClick = {
                            navController.slideFromBottom(R.id.feeSettings)
                        }
                    )
                }
                if (isLockTimeEnabled) {
                    add {
                        HSHodlerInput(
                            lockTimeIntervals = lockTimeIntervals,
                            lockTimeInterval = lockTimeInterval,
                            onSelect = {
                                viewModel.onEnterLockTimeInterval(it)
                            }
                        )
                    }
                }
            }

            CellSingleLineLawrenceSection(composableItems = composableItems)

            feeRateCaution?.let {
                FeeRateCaution(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    feeRateCaution = feeRateCaution
                )
            }

            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                title = stringResource(R.string.Send_DialogProceed),
                onClick = {
                    navController.slideFromRight(R.id.sendConfirmation)
                },
                enabled = proceedEnabled
            )
        }
    }

}

@Composable
fun HSHodlerInput(
    lockTimeIntervals: List<LockTimeInterval?> = listOf(),
    lockTimeInterval: LockTimeInterval?,
    onSelect: ((LockTimeInterval?) -> Unit)? = null
) {
    var showSelectorDialog by remember { mutableStateOf(false) }
    if (showSelectorDialog) {
        SelectorDialogCompose(
            title = stringResource(R.string.Send_DialogSpeed),
            items = lockTimeIntervals.map {
                TabItem(stringResource(it.stringResId()), it == lockTimeInterval, it)
            },
            onDismissRequest = {
                showSelectorDialog = false
            },
            onSelectItem = {
                onSelect?.invoke(it)
            }
        )
    }

    val selectable = lockTimeIntervals.isNotEmpty()
    val modifierClickable = if (selectable) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {
                showSelectorDialog = true
            }
        )
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .then(modifierClickable),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.Send_DialogLockTime),
            style = ComposeAppTheme.typography.subhead2,
            color = ComposeAppTheme.colors.grey,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(lockTimeInterval.stringResId()),
            style = ComposeAppTheme.typography.subhead1,
            color = ComposeAppTheme.colors.leah,
        )
        if (selectable) {
            Icon(
                modifier = Modifier.padding(start = 8.dp),
                painter = painterResource(R.drawable.ic_down_arrow_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
    }
}

