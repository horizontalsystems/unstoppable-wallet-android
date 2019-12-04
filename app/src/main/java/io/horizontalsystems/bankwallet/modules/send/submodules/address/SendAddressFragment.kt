package io.horizontalsystems.bankwallet.modules.send.submodules.address

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.SendSubmoduleFragment
import io.horizontalsystems.hodler.HodlerPlugin
import kotlinx.android.synthetic.main.view_address_input.*

class SendAddressFragment(
        private val coin: Coin,
        private val addressModuleDelegate: SendAddressModule.IAddressModuleDelegate,
        private val sendHandler: SendModule.ISendHandler
) : SendSubmoduleFragment() {

    private lateinit var presenter: SendAddressPresenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.view_address_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        btnBarcodeScan.visibility = View.VISIBLE
        btnPaste.visibility = View.VISIBLE
        btnDeleteAddress.visibility = View.GONE

        presenter = ViewModelProvider(this, SendAddressModule.Factory(coin, sendHandler))
                .get(SendAddressPresenter::class.java)
        val presenterView = presenter.view as SendAddressView
        presenter.moduleDelegate = addressModuleDelegate

        btnBarcodeScan.setOnClickListener { presenter.onAddressScanClicked() }
        btnPaste?.setOnClickListener { presenter.onAddressPasteClicked() }
        btnDeleteAddress?.setOnClickListener { presenter.onAddressDeleteClicked() }

        presenterView.addressText.observe(viewLifecycleOwner, Observer { address ->
            txtAddress.text = address

            val empty = address?.isEmpty() ?: true
            btnBarcodeScan.visibility = if (empty) View.VISIBLE else View.GONE
            btnPaste.visibility = if (empty) View.VISIBLE else View.GONE
            btnDeleteAddress.visibility = if (empty) View.GONE else View.VISIBLE
        })

        presenterView.error.observe(viewLifecycleOwner, Observer { error ->
            when (error) {
                null -> txtAddressError.visibility = View.GONE
                else -> {
                    txtAddressError.text = when (error) {
                        is HodlerPlugin.UnsupportedAddressType -> getString(R.string.Send_Error_UnsupportedAddress)
                        else -> getString(R.string.Send_Error_IncorrectAddress)
                    }
                    txtAddressError.visibility = View.VISIBLE
                }
            }
        })

        presenterView.pasteButtonEnabled.observe(viewLifecycleOwner, Observer { enabled ->
            btnPaste.isEnabled = enabled
        })
    }

    override fun init() {
        presenter.onViewDidLoad()
    }
}
