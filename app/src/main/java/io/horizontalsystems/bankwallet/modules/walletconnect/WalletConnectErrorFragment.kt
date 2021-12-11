package io.horizontalsystems.bankwallet.modules.walletconnect

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import kotlinx.android.synthetic.main.fragment_wallet_connect_error.*

class WalletConnectErrorFragment : Fragment(R.layout.fragment_wallet_connect_error) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        message.text = arguments?.getString(MESSAGE_KEY)

        buttonCancelCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        buttonCancelCompose.setContent {
            ComposeAppTheme {
                ButtonPrimaryDefault(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    title = getString(R.string.Button_Cancel),
                    onClick = {
                        findNavController().popBackStack()
                    }
                )
            }
        }
    }

    companion object {
        const val MESSAGE_KEY = "MESSAGE_KEY"
    }

}
