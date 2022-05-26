package io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.SignMessage
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.WCSignMessageRequestModule.TYPED_MESSAGE
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.WCSignMessageRequestViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.ui.TitleTypedValueCell
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*

@Composable
fun SignMessageRequestScreen(
    navController: NavController,
    viewModel: WCSignMessageRequestViewModel,
) {

    ComposeAppTheme {
        Column(
            modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
        ) {
            AppBar(
                TranslatableString.PlainString(stringResource(R.string.WalletConnect_SignMessageRequest_Title)),

                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = { navController.popBackStack() }
                    )
                )
            )
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Spacer(Modifier.height(12.dp))

                when (val message = viewModel.message) {
                    is SignMessage.PersonalMessage -> {
                        Text(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            text = message.data,
                            color = ComposeAppTheme.colors.grey,
                            style = ComposeAppTheme.typography.subhead2
                        )
                    }
                    is SignMessage.Message -> {
                        MessageContent(message, viewModel.dAppName, navController, viewModel)
                    }
                    is SignMessage.TypedMessage -> {
                        TypedMessageContent(message, viewModel.dAppName, navController)
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
            Column(Modifier.padding(horizontal = 24.dp)) {
                ButtonPrimaryYellow(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.WalletConnect_SignMessageRequest_ButtonSign),
                    enabled = viewModel.signEnabled,
                    onClick = { viewModel.sign() },
                )
                Spacer(Modifier.height(16.dp))
                ButtonPrimaryDefault(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Button_Reject),
                    onClick = { viewModel.reject() }
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun TypedMessageContent(
    message: SignMessage.TypedMessage,
    dAppName: String?,
    navController: NavController
) {
    val composableItems: MutableList<@Composable () -> Unit> = mutableListOf()
    message.domain?.let { domain ->
        composableItems.add {
            TitleTypedValueCell(
                stringResource(R.string.WalletConnect_SignMessageRequest_Domain),
                domain
            )
        }
    }
    composableItems.add {
        SignMessageButton(
            stringResource(R.string.WalletConnect_SignMessageRequest_ShowMessageTitle),
            message.data,
            navController
        )
    }
    dAppName?.let { dAppName ->
        composableItems.add {
            TitleTypedValueCell(
                stringResource(R.string.WalletConnect_SignMessageRequest_dApp),
                dAppName
            )
        }
    }

    CellSingleLineLawrenceSection(
        composableItems
    )
}

@Composable
private fun MessageContent(
    message: SignMessage.Message,
    dAppName: String?,
    navController: NavController,
    viewModel: WCSignMessageRequestViewModel
) {
    val composableItems: MutableList<@Composable () -> Unit> = mutableListOf({
        SignMessageButton(
            stringResource(R.string.WalletConnect_SignMessageRequest_ShowMessageTitle),
            message.data,
            navController
        )
    })
    dAppName?.let { dApp ->
        composableItems.add { TitleTypedValueCell(stringResource(R.string.WalletConnect_SignMessageRequest_dApp), dApp) }
    }

    CellSingleLineLawrenceSection(
        composableItems
    )

    if (message.showLegacySignWarning) {
        Spacer(Modifier.height(12.dp))
        TextImportantWarning(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.WalletConnect_LegacySignWarning),
            icon = R.drawable.ic_attention_20,
            title = stringResource(R.string.WalletConnect_Note),
        )
        Spacer(Modifier.height(12.dp))
        CellSingleLineLawrenceSection {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        viewModel.onTrustChecked(!viewModel.trustCheckmarkChecked)
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HsCheckbox(
                    checked = viewModel.trustCheckmarkChecked,
                    onCheckedChange = { checked ->
                        viewModel.onTrustChecked(checked)
                    }
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.WalletConnect_I_Trust),
                    color = ComposeAppTheme.colors.leah,
                    style = ComposeAppTheme.typography.subhead2
                )
            }
        }
    }
}

@Composable
private fun SignMessageButton(title: String, data: String, navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable {
                navController.slideFromBottom(
                    R.id.wcDisplayTypedMessageFragment,
                    bundleOf(TYPED_MESSAGE to formatJson(data))
                )
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead2
        )
        Spacer(Modifier.weight(1f))
        Image(
            modifier = Modifier.padding(start = 8.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null
        )
    }
}

private fun formatJson(text: String): String {
    val json = StringBuilder()
    var indentString = ""
    for (element in text) {
        when (element) {
            '{', '[' -> {
                json.append("\n$indentString$element\n")
                indentString += "\t"
                json.append(indentString)
            }
            '}', ']' -> {
                indentString = indentString.replaceFirst("\t".toRegex(), "")
                json.append("\n$indentString$element")
            }
            ',' -> json.append("$element\n$indentString")
            else -> json.append(element)
        }
    }
    return json.toString()
}
