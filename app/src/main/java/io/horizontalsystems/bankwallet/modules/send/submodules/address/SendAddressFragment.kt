package io.horizontalsystems.bankwallet.modules.send.submodules.address

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_address_input.*

class SendAddressFragment( private val addressViewModel: SendAddressViewModel )
    : Fragment() {

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.view_address_input, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {

        super.onActivityCreated(savedInstanceState)

        addressViewModel.let {

            val delegate  = it.delegate

            btnBarcodeScan.setOnClickListener {  delegate.onAddressScanClicked() }
            btnPaste?.setOnClickListener { delegate.onAddressPasteClicked() }
            btnDeleteAddress?.setOnClickListener { delegate.onAddressDeleteClicked() }

            it.addressText.observe( viewLifecycleOwner, Observer { address ->
                txtAddress.text = address

                val empty = address?.isEmpty() ?: true
                btnBarcodeScan.visibility = if (empty) View.VISIBLE else View.GONE
                btnPaste.visibility = if (empty) View.VISIBLE else View.GONE
                btnDeleteAddress.visibility = if (empty) View.GONE else View.VISIBLE
            })

            it.error.observe(viewLifecycleOwner, Observer { error ->
                error?.let {
                    val errorText = context?.getString(R.string.Send_Error_IncorrectAddress)
                    txtAddressError.visibility = View.VISIBLE
                    txtAddressError.text = errorText
                } ?: run {
                    txtAddressError.visibility = View.GONE
                }
            })

            it.pasteButtonEnabled.observe(viewLifecycleOwner, Observer { enabled ->
                btnPaste.isEnabled = enabled
            })
        }
    }

}
