package io.horizontalsystems.bankwallet.modules.coinzixverify.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coinzixverify.CoinzixVerificationViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellowWithSpinner
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer

@Composable
fun CoinzixVerificationScreen(
    viewModel: CoinzixVerificationViewModel,
    onSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    onClose: () -> Unit,
    onShowError: (description: TranslatableString) -> Unit
) {
    val uiState = viewModel.uiState
    val error = uiState.error

    LaunchedEffect(error) {
        error?.let {
            onShowError.invoke(TranslatableString.PlainString(it.message ?: it.javaClass.simpleName))
        }
    }

    if (uiState.success) {
        LaunchedEffect(Unit) {
            onSuccess()
        }
    }

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.CexWithdraw_SecurityVerification),
                    navigationIcon = {
                        HsBackButton(onClick = onNavigateBack)
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = onClose
                        )
                    )
                )
            }
        ) {
            Column(modifier = Modifier.padding(it)) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .weight(1f)
                ) {
                    VSpacer(32.dp)
                    if (uiState.emailCodeEnabled) {
                        EmailVerificationCodeInput(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            hint = stringResource(R.string.CexWithdraw_EmailVerificationCode),
                            state = null,
                            actionButtonState = uiState.resendButtonState,
                            onValueChange = { v ->
                                viewModel.onEnterEmailCode(v)
                            },
                            actionButtonClick = {
                                viewModel.onResendEmailCode()
                            }
                        )
                        InfoText(
                            text = stringResource(R.string.CexWithdraw_EmailVerificationInfo)
                        )
                        VSpacer(20.dp)
                    }

                    if (uiState.googleCodeEnabled) {
                        FormsInput(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            hint = stringResource(R.string.CexWithdraw_GoogleAuthenticationCode),
                            pasteEnabled = true,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            onValueChange = { v ->
                                viewModel.onEnterTwoFactorCode(v)
                            }
                        )
                        InfoText(
                            text = stringResource(R.string.CexWithdraw_GoogleAuthenticationInfo)
                        )
                        VSpacer(20.dp)
                    }
                }

                ButtonsGroupWithShade {
                    ButtonPrimaryYellowWithSpinner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        title = stringResource(R.string.Button_Submit),
                        onClick = {
                            viewModel.submit()
                        },
                        showSpinner = uiState.loading,
                        enabled = uiState.submitEnabled
                    )
                }
            }
        }
    }
}
