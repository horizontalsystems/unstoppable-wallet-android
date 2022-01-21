package io.horizontalsystems.bankwallet.modules.walletconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportant
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader

class WalletConnectErrorNoAccountFragment : BaseComposableBottomSheetFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): ComposeView {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    WalletConnectErrorNoAccount(
                        onClickClose = {
                            close()
                        },
                        onClickUnderstand = {
                            close()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WalletConnectErrorNoAccount(
    onClickClose: () -> Unit,
    onClickUnderstand: () -> Unit
) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_wallet_connect_24),
        title = stringResource(R.string.WalletConnect_Title),
        subtitle = stringResource(R.string.WalletConnect_Requirement),
        onCloseClick = onClickClose
    ) {
        TextImportant(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 22.dp),
            text = stringResource(id = R.string.WalletConnect_Error_NoWallet)
        )
        ButtonPrimaryYellow(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 22.dp)
                .fillMaxWidth(),
            title = stringResource(R.string.Button_Understand),
            onClick = onClickUnderstand
        )
    }
}
