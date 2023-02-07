package cash.p.terminal.modules.walletconnect.request.signmessage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.modules.walletconnect.request.signmessage.WCSignMessageRequestModule.TYPED_MESSAGE
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.subhead2_grey
import io.horizontalsystems.core.findNavController

class WCDisplayTypedMessageFragment : BaseFragment() {

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
                SignMessageTextScreen(
                    findNavController(),
                    arguments?.getString(TYPED_MESSAGE)
                )
            }
        }
    }
}

@Composable
private fun SignMessageTextScreen(navController: NavController, messageText: String?) {
    ComposeAppTheme {
        Column(
            modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
        ) {
            AppBar(
                TranslatableString.PlainString(stringResource(R.string.WalletConnect_SignMessageRequest_ShowMessageTitle)),

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
                    .fillMaxWidth()
            ) {
                Spacer(Modifier.height(12.dp))
                messageText?.let {
                    subhead2_grey(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = it,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
