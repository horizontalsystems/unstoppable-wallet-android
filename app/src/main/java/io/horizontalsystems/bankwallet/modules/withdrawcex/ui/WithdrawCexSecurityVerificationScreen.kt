package io.horizontalsystems.bankwallet.modules.withdrawcex.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.withdrawcex.WithdrawCexModule.CodeGetButtonState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.core.helpers.HudHelper

@Composable
fun WithdrawCexSecurityVerificationScreen(
    withdrawId: String,
    onNavigateBack: () -> Unit,
    onClose: () -> Unit
) {
    val viewModel = viewModel<CexWithdrawVerificationViewModel>(factory = CexWithdrawVerificationViewModel.Factory(withdrawId))
    val view = LocalView.current
    val uiState = viewModel.uiState
    val error = uiState.error

    LaunchedEffect(error) {
        error?.let {
            HudHelper.showErrorMessage(view, it.message ?: it.javaClass.simpleName)
        }
    }

    if (uiState.success) {
        LaunchedEffect(uiState.success) {
            HudHelper.showSuccessMessage(view, R.string.CexWithdraw_WithdrawSuccess)
            onClose.invoke()
        }
    }

    var actionButtonState by remember { mutableStateOf<CodeGetButtonState>(CodeGetButtonState.Active) }
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
                    EmailVerificationCodeInput(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        hint = stringResource(R.string.CexWithdraw_EmailVerificationCode),
                        state = null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        actionButtonState = actionButtonState,
                        onValueChange = {
                            viewModel.onEnterEmailCode(it)
                        },
                        actionButtonClick = {
                            actionButtonState = CodeGetButtonState.Pending(30)
                            viewModel.onResendEmailVerificationCode()
                        }
                    )
                    InfoText(
                        text = stringResource(R.string.CexWithdraw_EmailVerificationInfo)
                    )
                    VSpacer(20.dp)
                    AuthenticationCodeInput(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        hint = stringResource(R.string.CexWithdraw_GoogleAuthenticationCode),
                        state = null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        onValueChange = {
                            viewModel.onEnterTwoFactorCode(it)
                        },
                    )
                    InfoText(
                        text = stringResource(R.string.CexWithdraw_GoogleAuthenticationInfo)
                    )
                    VSpacer(20.dp)
                }

                ButtonsGroupWithShade {
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        title = stringResource(R.string.Button_Submit),
                        onClick = {
                            viewModel.submit()
                        },
                        enabled = uiState.submitEnabled
                    )
                }
            }
        }
    }
}
