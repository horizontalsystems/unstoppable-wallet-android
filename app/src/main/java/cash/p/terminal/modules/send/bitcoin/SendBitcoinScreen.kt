package cash.p.terminal.modules.send.bitcoin

import androidx.annotation.StringRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import cash.p.terminal.R
import cash.p.terminal.core.composablePage
import cash.p.terminal.core.composablePopup
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.modules.address.AddressParserModule
import cash.p.terminal.modules.address.AddressParserViewModel
import cash.p.terminal.modules.address.HSAddressInput
import cash.p.terminal.modules.amount.AmountInputModeViewModel
import cash.p.terminal.modules.amount.HSAmountInput
import cash.p.terminal.modules.availablebalance.AvailableBalance
import cash.p.terminal.modules.fee.FeeRateCaution
import cash.p.terminal.modules.fee.HSFeeInputRaw
import cash.p.terminal.modules.hodler.HSHodlerInput
import cash.p.terminal.modules.send.SendConfirmationFragment
import cash.p.terminal.modules.send.SendScreen
import io.horizontalsystems.bankwallet.modules.send.bitcoin.advanced.SendBtcAdvancedSettingsScreen
import io.horizontalsystems.bankwallet.modules.send.bitcoin.advanced.BtcTransactionInputSortInfoScreen
import cash.p.terminal.modules.settings.about.*
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.*
import java.math.BigDecimal


const val SendBtcPage = "send_btc"
const val SendBtcAdvancedSettingsPage = "send_btc_advanced_settings"
const val TransactionInputsSortInfoPage = "transaction_input_sort_info_settings"

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SendBitcoinNavHost(
    fragmentNavController: NavController,
    viewModel: SendBitcoinViewModel,
    amountInputModeViewModel: AmountInputModeViewModel
) {
    val navController = rememberAnimatedNavController()
    AnimatedNavHost(
        navController = navController,
        startDestination = SendBtcPage,
    ) {
        composable(SendBtcPage) {
            SendBitcoinScreen(
                fragmentNavController,
                navController,
                viewModel,
                amountInputModeViewModel
            )
        }
        composablePage(SendBtcAdvancedSettingsPage) {
            SendBtcAdvancedSettingsScreen(navController, viewModel.blockchainType)
        }
        composablePopup(TransactionInputsSortInfoPage) { BtcTransactionInputSortInfoScreen { navController.popBackStack() } }
    }
}

@Composable
fun SendBitcoinScreen(
    fragmentNavController: NavController,
    composeNavController: NavController,
    viewModel: SendBitcoinViewModel,
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

    val rate = viewModel.coinRate

    val paymentAddressViewModel = viewModel<AddressParserViewModel>(factory = AddressParserModule.Factory(wallet.token.blockchainType))
    val amountUnique = paymentAddressViewModel.amountUnique

    ComposeAppTheme {
        val fullCoin = wallet.token.fullCoin
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        SendScreen(
            fullCoin = fullCoin,
            onCloseClick = { fragmentNavController.popBackStack() }
        ) {
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

            Spacer(modifier = Modifier.height(12.dp))
            HSAddressInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                tokenQuery = wallet.token.tokenQuery,
                coinCode = wallet.coin.code,
                error = addressError,
                textPreprocessor = paymentAddressViewModel
            ) {
                viewModel.onEnterAddress(it)
            }

            Spacer(modifier = Modifier.height(12.dp))

            val additionalItems = buildList<@Composable () -> Unit> {
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
                            fragmentNavController.slideFromBottom(R.id.feeSettings)
                        }
                    )
                }
                add {
                    AdvancedSettingCell(
                        title = R.string.Send_Advanced,
                        onClick = { composeNavController.navigate(SendBtcAdvancedSettingsPage) }
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

            CellUniversalLawrenceSection(additionalItems)

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
                    fragmentNavController.slideFromRight(
                        R.id.sendConfirmation,
                        SendConfirmationFragment.prepareParams(SendConfirmationFragment.Type.Bitcoin)
                    )
                },
                enabled = proceedEnabled
            )
        }
    }
}

@Composable
private fun AdvancedSettingCell(
    @StringRes title: Int,
    onClick: () -> Unit
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        subhead2_grey(
            text = stringResource(title),
            maxLines = 1,
            modifier = Modifier.padding(end = 16.dp)
        )
        Spacer(Modifier.weight(1f))
        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
        )
    }
}