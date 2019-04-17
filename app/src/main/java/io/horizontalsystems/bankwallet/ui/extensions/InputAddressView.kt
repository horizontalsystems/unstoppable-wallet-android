package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_address_input.view.*

class InputAddressView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_address_input, this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


    fun bindAddressInputInitial(onAmpersandClick: (() -> (Unit))? = null,
                         onBarcodeClick: (() -> (Unit))? = null,
                         onPasteClick: (() -> (Unit))? = null,
                         onDeleteClick: (() -> (Unit))? = null
    ) {
        btnBarcodeScan.visibility =  View.VISIBLE
        btnPaste.visibility =  View.VISIBLE
        btnDeleteAddress.visibility =  View.GONE

        btnAmpersand?.setOnClickListener { onAmpersandClick?.invoke() }
        btnBarcodeScan?.setOnClickListener { onBarcodeClick?.invoke() }
        btnPaste?.setOnClickListener { onPasteClick?.invoke() }
        btnDeleteAddress?.setOnClickListener { onDeleteClick?.invoke() }

        invalidate()
    }

    fun updateInput(address: String = "", errorText: String? = null) {
        val empty = address.isEmpty()
        btnBarcodeScan.visibility =  if (empty) View.VISIBLE else View.GONE
        btnPaste.visibility =  if (empty) View.VISIBLE else View.GONE

        btnDeleteAddress.visibility =  if (empty) View.GONE else View.VISIBLE

        txtAddress.text = address

        errorText?.let {
            txtAddressError.visibility = View.VISIBLE
            txtAddressError.text = it
        } ?: run {
            txtAddressError.visibility = View.GONE
        }
    }

    fun enablePasteButton(enabled: Boolean) {
        btnPaste.isEnabled = enabled
    }

}
