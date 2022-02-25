package io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WalletConnectRequestModule.TYPED_MESSAGE
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.WCSignMessageRequestModule.SignMessage
import io.horizontalsystems.bankwallet.modules.walletconnect.request.ui.TitleTypedValueCell
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController

class WCSignMessageRequestFragment : BaseFragment() {

    private val baseViewModel by navGraphViewModels<WalletConnectViewModel>(R.id.wcSessionFragment)
    val vmFactory by lazy {
        WCSignMessageRequestModule.Factory(
            baseViewModel.sharedSignMessageRequest!!,
            baseViewModel.service
        )
    }
    private val viewModel by viewModels<WCSignMessageRequestViewModel> { vmFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                SignMessageRequestScreen(
                    findNavController(),
                    viewModel
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            viewModel.reject()
        }

        viewModel.closeLiveEvent.observe(viewLifecycleOwner) {
            baseViewModel.sharedSignMessageRequest = null
            findNavController().popBackStack()
        }
    }

}

@Composable
private fun SignMessageRequestScreen(
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
                    is SignMessage.Message,
                    is SignMessage.PersonalMessage -> {
                        Text(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            text = message.data,
                            color = ComposeAppTheme.colors.grey,
                            style = ComposeAppTheme.typography.subhead2
                        )
                    }
                    is SignMessage.TypedMessage -> {
                        CellSingleLineLawrenceSection(
                            listOf({
                                TitleTypedValueCell(
                                    stringResource(R.string.WalletConnect_SignMessageRequest_Domain),
                                    message.domain
                                )
                            }, {
                                SignMessageButton(
                                    stringResource(R.string.WalletConnect_SignMessageRequest_ShowMessageTitle),
                                    formatJson(message.data),
                                    navController
                                )
                            })
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
            Column(Modifier.padding(horizontal = 24.dp)) {
                ButtonPrimaryYellow(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.WalletConnect_SignMessageRequest_ButtonSign),
                    onClick = { viewModel.sign() }
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
fun SignMessageButton(title: String, data: String, navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable {
                navController.slideFromBottom(
                    R.id.wcSignMessageRequestFragment_to_wcDisplayTypedMessageFragment,
                    bundleOf(TYPED_MESSAGE to data)
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
