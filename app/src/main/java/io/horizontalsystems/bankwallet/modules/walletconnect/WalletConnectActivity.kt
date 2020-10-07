package io.horizontalsystems.bankwallet.modules.walletconnect

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.modules.walletconnect.main.WalletConnectMainFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.scanqr.WalletConnectScanQrFragment

class WalletConnectActivity : BaseActivity() {

    private val viewModel by viewModels<WalletConnectViewModel> { WalletConnectModule.Factory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_connect)

        openScreen(viewModel.initialScreen)
    }

    fun showFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            replace(R.id.fragmentContainerViewXxx, fragment)
        }
    }

    private fun openScreen(screen: WalletConnectViewModel.InitialScreen) {
        when (screen) {
            WalletConnectViewModel.InitialScreen.NoEthereumKit -> {
                WalletConnectNoEthereumKitFragment().show(supportFragmentManager, "NoEthereumKitFragment")
            }
            WalletConnectViewModel.InitialScreen.ScanQrCode -> {
                showFragment(WalletConnectScanQrFragment())
            }
            WalletConnectViewModel.InitialScreen.Main -> {
                showFragment(WalletConnectMainFragment())
            }
        }
    }

}
