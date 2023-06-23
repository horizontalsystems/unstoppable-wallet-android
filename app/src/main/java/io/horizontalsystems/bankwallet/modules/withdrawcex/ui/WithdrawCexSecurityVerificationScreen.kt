package cash.p.terminal.modules.withdrawcex.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.withdrawcex.WithdrawCexModule.CodeGetButtonState
import cash.p.terminal.modules.withdrawcex.WithdrawCexViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.VSpacer

@Composable
fun WithdrawCexSecurityVerificationScreen(
    mainViewModel: WithdrawCexViewModel,
    onNavigateBack: () -> Unit,
    onClose: () -> Unit
) {
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
                            /*TODO*/
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
                            /*TODO*/
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
                            //openConfirm.invoke()
                        },
                        enabled = true
                    )
                }
            }
        }
    }
}
