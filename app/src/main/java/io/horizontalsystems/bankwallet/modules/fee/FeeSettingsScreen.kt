package io.horizontalsystems.bankwallet.modules.fee

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.send.SendErrorInsufficientBalance
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*

@Composable
fun FeeSettingsScreen(
    navController: NavController,
    sendViewModel: SendBitcoinViewModel,
    amountInputModeViewModel: AmountInputModeViewModel
) {
    val uiState = sendViewModel.uiState
    val fee = uiState.fee
    val feeRatePriority = uiState.feeRatePriority
    val feeRate = uiState.feeRate
    val feeRateCaution = uiState.feeRateCaution
    val amountCaution = uiState.amountCaution

    ComposeAppTheme {
        Column(modifier = Modifier
            .background(color = ComposeAppTheme.colors.tyler)
            .fillMaxSize()) {
            AppBar(
                title = TranslatableString.ResString(R.string.FeeSettings_Title),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.FeeSettings_Reset),
                        enabled = feeRatePriority != FeeRatePriority.RECOMMENDED,
                        onClick = {
                            sendViewModel.onEnterFeeRatePriority(FeeRatePriority.RECOMMENDED)
                        }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
            HSFeeInput(
                coinCode = sendViewModel.wallet.coin.code,
                coinDecimal = sendViewModel.coinMaxAllowedDecimals,
                fiatDecimal = sendViewModel.fiatMaxAllowedDecimals,
                fee = fee,
                amountInputType = amountInputModeViewModel.inputType,
                rate = sendViewModel.coinRate
            )

            Spacer(modifier = Modifier.height(8.dp))

            CellSingleLineLawrenceSection {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .clickable {
                                navController.slideFromBottom(R.id.feePriorityInfo)
                            },
                        painter = painterResource(R.drawable.ic_info_20),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.grey
                    )
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = stringResource(R.string.Send_DialogSpeed),
                        color = ComposeAppTheme.colors.grey,
                        style = ComposeAppTheme.typography.subhead2
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    var showSpeedSelectorDialog by remember { mutableStateOf(false) }
                    if (showSpeedSelectorDialog) {
                        SelectorDialogCompose(
                            title = stringResource(R.string.Send_DialogSpeed),
                            items = sendViewModel.feeRatePriorities.map {
                                TabItem(stringResource(it.titleRes), it::class == feeRatePriority::class, it)
                            },
                            onDismissRequest = {
                                showSpeedSelectorDialog = false
                            },
                            onSelectItem = {
                                if (it is FeeRatePriority.Custom) {
                                    feeRate?.let {
                                        sendViewModel.onEnterFeeRatePriority(FeeRatePriority.Custom(feeRate))
                                    }
                                } else {
                                    sendViewModel.onEnterFeeRatePriority(it)
                                }
                            }
                        )
                    }

                    ButtonSecondaryWithIcon(
                        modifier = Modifier.padding(end = 16.dp),
                        title = stringResource(feeRatePriority.titleRes),
                        iconRight = painterResource(R.drawable.ic_down_arrow_20),
                        onClick = {
                            showSpeedSelectorDialog = true
                        }
                    )
                }
            }

            val valueRange = sendViewModel.feeRateRange
            if (feeRate != null && valueRange != null) {
                Spacer(modifier = Modifier.height(8.dp))

                var sliderValue by remember(feeRate) { mutableStateOf(feeRate) }

                HSSectionRounded {
                    CellSingleLineLawrence {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.Send_FeeRate),
                                style = ComposeAppTheme.typography.subhead2,
                                color = ComposeAppTheme.colors.grey
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = "$sliderValue " + stringResource(R.string.Send_TxSpeed_CustomFeeHint),
                                style = ComposeAppTheme.typography.subhead1,
                                color = ComposeAppTheme.colors.leah,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    CellSingleLineLawrence(borderTop = true) {
                        HsSlider(
                            value = feeRate,
                            onValueChange = {
                                sliderValue = it
                            },
                            valueRange = valueRange,
                            onValueChangeFinished = {
                                sendViewModel.onEnterFeeRatePriority(FeeRatePriority.Custom(sliderValue))
                            }
                        )
                    }
                }
            }

            if (amountCaution is SendErrorInsufficientBalance) {
                FeeRateCaution(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    feeRateCaution = amountCaution
                )
            }

            feeRateCaution?.let {
                FeeRateCaution(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    feeRateCaution = feeRateCaution
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                title = stringResource(R.string.Button_Done),
                onClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun FeeRateCaution(modifier: Modifier, feeRateCaution: HSCaution) {
    when (feeRateCaution.type) {
        HSCaution.Type.Error -> {
            TextImportantError(
                modifier = modifier,
                icon = R.drawable.ic_attention_20,
                title = feeRateCaution.getString(),
                text = feeRateCaution.getDescription() ?: ""
            )
        }
        HSCaution.Type.Warning -> {
            TextImportantWarning(
                modifier = modifier,
                icon = R.drawable.ic_attention_20,
                title = feeRateCaution.getString(),
                text = feeRateCaution.getDescription() ?: ""
            )
        }
    }
}