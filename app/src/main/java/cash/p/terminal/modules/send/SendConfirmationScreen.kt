package cash.p.terminal.modules.send

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.entities.Address
import io.horizontalsystems.core.entities.CurrencyValue
import cash.p.terminal.modules.amount.AmountInputType
import cash.p.terminal.modules.contacts.model.Contact
import cash.p.terminal.modules.fee.FeeInfoSection
import cash.p.terminal.modules.hodler.HSHodler
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.CoinImage
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.SectionTitleCell
import cash.p.terminal.ui.compose.components.TransactionInfoAddressCell
import cash.p.terminal.ui.compose.components.TransactionInfoContactCell
import cash.p.terminal.ui.compose.components.TransactionInfoRbfCell
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.components.subhead1Italic_leah
import cash.p.terminal.ui_compose.components.subhead1_grey
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.ui_compose.components.SnackbarDuration
import cash.p.terminal.ui_compose.components.HudHelper
import io.horizontalsystems.hodler.LockTimeInterval
import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.ui_compose.components.CustomSnackbar
import cash.p.terminal.ui_compose.components.VSpacer
import kotlinx.coroutines.delay
import java.math.BigDecimal

@Composable
fun SendConfirmationScreen(
    navController: NavController,
    coinMaxAllowedDecimals: Int,
    feeCoinMaxAllowedDecimals: Int,
    amountInputType: AmountInputType,
    rate: CurrencyValue?,
    feeCoinRate: CurrencyValue?,
    sendResult: SendResult?,
    blockchainType: BlockchainType,
    coin: Coin,
    feeCoin: Coin,
    amount: BigDecimal,
    address: Address,
    contact: Contact?,
    fee: BigDecimal?,
    lockTimeInterval: LockTimeInterval?,
    memo: String?,
    rbfEnabled: Boolean?,
    onClickSend: () -> Unit,
    sendEntryPointDestId: Int,
    isSynced: Boolean,
    sendToken: Token? = null,
    feeToken: Token? = null,
    feeCoinBalance: BigDecimal? = null,
    displayBalance: BigDecimal? = null,
    insufficientFeeBalance: Boolean = false,
    balanceHidden: Boolean = false,
    onBalanceClicked: () -> Unit = {},
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
) {
    val closeUntilDestId = if (sendEntryPointDestId == 0) {
        R.id.sendXFragment
    } else {
        sendEntryPointDestId
    }
    val view = LocalView.current
    var currentSnackbar by remember { mutableStateOf<CustomSnackbar?>(null) }

    when (sendResult) {
        SendResult.Sending -> {
            currentSnackbar = HudHelper.showInProcessMessage(
                view,
                R.string.Send_Sending,
                SnackbarDuration.INDEFINITE
            )
        }

        is SendResult.Sent -> {
            currentSnackbar = HudHelper.showSuccessMessage(
                view,
                R.string.Send_Success,
                SnackbarDuration.MEDIUM
            )
        }

        is SendResult.SentButQueued -> {
            currentSnackbar = HudHelper.showWarningMessage(
                view,
                R.string.send_success_queued,
                SnackbarDuration.LONG
            )
        }

        is SendResult.Failed -> {
            currentSnackbar = HudHelper.showErrorMessage(view, sendResult.caution.getDescription() ?: sendResult.caution.getString())
        }

        null -> {
            currentSnackbar?.dismiss()
        }
    }

    LaunchedEffect(sendResult) {
        if (sendResult is SendResult.Sent || sendResult is SendResult.SentButQueued) {
            delay(1200)
            navController.popBackStack(closeUntilDestId, true)
        }
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        if (sendResult is SendResult.Sent || sendResult is SendResult.SentButQueued) {
            navController.popBackStack(closeUntilDestId, true)
        }
    }

    Column(Modifier.windowInsetsPadding(windowInsets).background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = stringResource(R.string.Send_Confirmation_Title),
            navigationIcon = {
                HsBackButton(onClick = { navController.popBackStack() })
            },
            menuItems = listOf()
        )
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 106.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                val topSectionItems = buildList<@Composable () -> Unit> {
                    add {
                        SectionTitleCell(
                            stringResource(R.string.Send_Confirmation_YouSend),
                            coin.name,
                            R.drawable.ic_arrow_up_right_12
                        )
                    }
                    add {
                        val coinAmount = App.numberFormatter.formatCoinNotRounded(
                            amount,
                            coin.code,
                            coinMaxAllowedDecimals
                        )

                        val currencyAmount = rate?.let { rate ->
                            rate.copy(value = amount.times(rate.value))
                                .getFormattedFull()
                        }

                        ConfirmAmountCell(currencyAmount, coinAmount, coin)
                    }
                    add {
                        TransactionInfoAddressCell(
                            title = stringResource(R.string.Send_Confirmation_To),
                            value = address.hex,
                            showAdd = contact == null,
                            blockchainType = blockchainType,
                            navController = navController,
                            onCopy = {
                            },
                            onAddToExisting = {
                            },
                            onAddToNew = {
                            }
                        )
                    }
                    contact?.let {
                        add {
                            TransactionInfoContactCell(name = contact.name)
                        }
                    }
                    if (lockTimeInterval != null) {
                        add {
                            HSHodler(lockTimeInterval = lockTimeInterval)
                        }
                    }

                    if (rbfEnabled == false) {
                        add {
                            TransactionInfoRbfCell(rbfEnabled)
                        }
                    }
                }

                CellUniversalLawrenceSection(topSectionItems)

                VSpacer(height = 16.dp)

                val feePrimary = if (fee != null) {
                    App.numberFormatter.formatCoinFull(fee, feeCoin.code, feeCoinMaxAllowedDecimals)
                } else {
                    "---"
                }
                val feeSecondary = if (fee != null && feeCoinRate != null) {
                    feeCoinRate.copy(value = fee.times(feeCoinRate.value)).getFormattedFull()
                } else {
                    ""
                }

                FeeInfoSection(
                    tokenIn = sendToken,
                    displayBalance = displayBalance,
                    balanceHidden = balanceHidden,
                    feeToken = feeToken,
                    feeCoinBalance = feeCoinBalance,
                    feePrimary = feePrimary,
                    feeSecondary = feeSecondary,
                    insufficientFeeBalance = insufficientFeeBalance,
                    onBalanceClicked = onBalanceClicked,
                )

                if (!memo.isNullOrBlank()) {
                    VSpacer(height = 12.dp)
                    CellUniversalLawrenceSection(listOf { MemoCell(memo) })
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                if (!isSynced) {
                    TextImportantWarning(
                        modifier = Modifier.padding(bottom = 12.dp),
                        text = stringResource(R.string.send_confirmation_syncing_warning)
                    )
                }
                SendButton(
                    modifier = Modifier.fillMaxWidth(),
                    sendResult = sendResult,
                    onClickSend = onClickSend,
                    enabled = isSynced
                )
            }
        }
    }
}

@Composable
fun SendButton(modifier: Modifier, sendResult: SendResult?, onClickSend: () -> Unit, enabled: Boolean = true) {
    when (sendResult) {
        SendResult.Sending -> {
            ButtonPrimaryYellow(
                modifier = modifier,
                title = stringResource(R.string.Send_Sending),
                onClick = { },
                enabled = false
            )
        }

        is SendResult.Sent,
        is SendResult.SentButQueued -> {
            ButtonPrimaryYellow(
                modifier = modifier,
                title = stringResource(R.string.Send_Success),
                onClick = { },
                enabled = false
            )
        }

        else -> {
            ButtonPrimaryYellow(
                modifier = modifier,
                title = stringResource(R.string.Send_Confirmation_Send_Button),
                onClick = onClickSend,
                enabled = enabled
            )
        }
    }
}

@Composable
fun ConfirmAmountCell(fiatAmount: String?, coinAmount: String, coin: Coin) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        CoinImage(
            coin = coin,
            modifier = Modifier.size(32.dp)
        )
        subhead2_leah(
            modifier = Modifier.padding(start = 16.dp),
            text = coinAmount,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.weight(1f))
        subhead1_grey(text = fiatAmount ?: "")
    }
}

@Composable
fun MemoCell(value: String) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        subhead2_grey(
            modifier = Modifier.padding(end = 16.dp),
            text = stringResource(R.string.Send_Confirmation_HintMemo),
        )
        Spacer(Modifier.weight(1f))
        subhead1Italic_leah(
            text = value,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
