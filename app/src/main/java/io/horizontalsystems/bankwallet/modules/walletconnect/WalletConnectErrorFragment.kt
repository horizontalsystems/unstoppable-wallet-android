package io.horizontalsystems.bankwallet.modules.walletconnect

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener

class WalletConnectErrorFragment : Fragment(R.layout.fragment_wallet_connect_error) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.message).text = arguments?.getString(MESSAGE_KEY)

        view.findViewById<Button>(R.id.cancelButton).setOnSingleClickListener {
            findNavController().popBackStack()
        }
    }

    companion object {
        const val MESSAGE_KEY = "MESSAGE_KEY"
    }

}
