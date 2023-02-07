package cash.p.terminal.modules.walletconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.TextImportantWarning
import cash.p.terminal.ui.extensions.BaseComposableBottomSheetFragment
import cash.p.terminal.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController

class WCErrorNoAccountFragment : BaseComposableBottomSheetFragment() {
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
                ComposeAppTheme {
                    WalletConnectErrorNoAccount() {
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }
}

@Composable
fun WalletConnectErrorNoAccount(onCloseClick: () -> Unit) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_wallet_connect_24),
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
        title = stringResource(R.string.WalletConnect_Title),
        onCloseClick = onCloseClick
    ) {
        TextImportantWarning(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            text = stringResource(id = R.string.WalletConnect_Error_NoWallet)
        )
        ButtonPrimaryYellow(
            modifier = Modifier
                .padding(vertical = 20.dp, horizontal = 24.dp)
                .fillMaxWidth(),
            title = stringResource(R.string.Button_Close),
            onClick = onCloseClick
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Preview
@Composable
private fun WalletConnectErrorNoAccountPreview() {
    ComposeAppTheme {
        WalletConnectErrorNoAccount({})
    }
}
