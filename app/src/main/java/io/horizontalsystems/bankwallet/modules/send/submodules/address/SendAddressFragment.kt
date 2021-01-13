package io.horizontalsystems.bankwallet.modules.send.submodules.address

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.zxing.integration.android.IntentIntegrator
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.SendSubmoduleFragment
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.RecipientAddressViewModel
import io.horizontalsystems.bankwallet.ui.extensions.AddressInputView

class SendAddressFragment(
        private val coin: Coin,
        private val addressModuleDelegate: SendAddressModule.IAddressModuleDelegate,
        private val sendHandler: SendModule.ISendHandler)
    : SendSubmoduleFragment() {

    private val addressInputView by lazy {
        AddressInputView(requireContext())
    }

    private val presenter by activityViewModels<RecipientAddressViewModel> {
        SendAddressModule.Factory(coin, sendHandler, addressModuleDelegate, placeholder = getString(R.string.Send_Hint_Address))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return addressInputView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        addressInputView.setViewModel(presenter, viewLifecycleOwner, onClickQrScan = {
            QRScannerActivity.start(this)
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IntentIntegrator.REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.getStringExtra(ModuleField.SCAN_ADDRESS)?.let {
                presenter.onFetch(it)
            }
        }
    }

    override fun init() {
    }
}
