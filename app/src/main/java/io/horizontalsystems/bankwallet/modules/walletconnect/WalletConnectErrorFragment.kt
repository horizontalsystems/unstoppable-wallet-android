package io.horizontalsystems.bankwallet.modules.walletconnect

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import kotlinx.android.synthetic.main.fragment_wallet_connect_error.*

class WalletConnectErrorFragment : Fragment(R.layout.fragment_wallet_connect_error) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        message.text = arguments?.getString(MESSAGE_KEY)

        cancelButton.setOnSingleClickListener {
            findNavController().popBackStack()
        }
    }

    companion object {
        const val MESSAGE_KEY = "MESSAGE_KEY"
    }

}
