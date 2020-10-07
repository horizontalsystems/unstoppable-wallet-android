package io.horizontalsystems.bankwallet.modules.walletconnect

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment

class WalletConnectNoEthereumKitFragment : BaseBottomSheetDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setContentView(R.layout.fragment_bottom_sheet_eth_kit_required)

        setTitle(getString(R.string.WalletConnect_Title))
        setSubtitle(getString(R.string.WalletConnect_Requirement))
        setHeaderIconDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_wallet_connect))
    }

}
