package cash.p.terminal.modules.withdrawcex.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.R
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.withdrawcex.WithdrawCexModule.CodeGetButtonState
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.*

@Composable
fun WithdrawCexSecurityVerificationScreen(
    withdrawId: String,
    onNavigateBack: () -> Unit,
    onClose: () -> Unit
) {
    val viewModel = viewModel {
        CexWithdrawVerificationViewModel(withdrawId)
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
                            viewModel.onEnter2FaCode(it)
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
                        enabled = viewModel.submitEnabled
                    )
                }
            }
        }
    }
}
