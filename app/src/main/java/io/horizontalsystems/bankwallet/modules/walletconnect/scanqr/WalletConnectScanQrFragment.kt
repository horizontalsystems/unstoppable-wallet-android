package io.horizontalsystems.bankwallet.modules.walletconnect.scanqr

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.zxing.integration.android.IntentIntegrator
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.managers.WalletConnectInteractor
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectActivity
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectErrorFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectModule
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.main.WalletConnectMainFragment

class WalletConnectScanQrFragment : BaseFragment() {
    private val baseViewModel by activityViewModels<WalletConnectViewModel> { WalletConnectModule.Factory() }
    private val viewModel by viewModels<WalletConnectScanQrViewModel> { WalletConnectScanQrModule.Factory(baseViewModel.service) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        QRScannerActivity.start(this)

        viewModel.openMainLiveEvent.observe(this, Observer {
            (requireActivity() as WalletConnectActivity).showFragment(WalletConnectMainFragment())
        })

        viewModel.openErrorLiveEvent.observe(this, Observer {
            val message = when (it) {
                is WalletConnectInteractor.SessionError.InvalidUri -> getString(R.string.WalletConnect_Error_InvalidUrl)
                else -> it.message ?: getString(R.string.default_error_msg)
            }

            (requireActivity() as WalletConnectActivity).showFragment(WalletConnectErrorFragment.newInstance(message))
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    data?.getStringExtra(ModuleField.SCAN_ADDRESS)?.let {
                        Log.e("AAA", "Scanned string: $it")
                        viewModel.handleScanned(it)
                    }
                }
                Activity.RESULT_CANCELED -> {
                    requireActivity().finish()
                }
            }
        }
    }
}