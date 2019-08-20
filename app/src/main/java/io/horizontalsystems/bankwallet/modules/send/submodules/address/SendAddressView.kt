package io.horizontalsystems.bankwallet.modules.send.submodules.address

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_address_input.view.*

class SendAddressView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_address_input, this)

        btnBarcodeScan.visibility = View.VISIBLE
        btnPaste.visibility = View.VISIBLE
        btnDeleteAddress.visibility = View.GONE

        invalidate()
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, lifecycleOwner: LifecycleOwner, sendAddressViewModel: SendAddressViewModel) : super(context) {
        val delegate = sendAddressViewModel.delegate

        btnBarcodeScan.setOnClickListener { delegate.onAddressScanClicked() }
        btnPaste?.setOnClickListener { delegate.onAddressPasteClicked() }
        btnDeleteAddress?.setOnClickListener { delegate.onAddressDeleteClicked() }

        sendAddressViewModel.addressText.observe(lifecycleOwner, Observer { address ->
            txtAddress.text = address

            val empty = address?.isEmpty() ?: true
            btnBarcodeScan.visibility = if (empty) View.VISIBLE else View.GONE
            btnPaste.visibility = if (empty) View.VISIBLE else View.GONE
            btnDeleteAddress.visibility = if (empty) View.GONE else View.VISIBLE
        })

        sendAddressViewModel.error.observe(lifecycleOwner, Observer { error ->
            error?.let {
                val errorText = context.getString(R.string.Send_Error_IncorrectAddress)
                txtAddressError.visibility = View.VISIBLE
                txtAddressError.text = errorText
            } ?: run {
                txtAddressError.visibility = View.GONE
            }
        })

        sendAddressViewModel.pasteButtonEnabled.observe(lifecycleOwner, Observer { enabled ->
            btnPaste.isEnabled = enabled
        })

    }

}
